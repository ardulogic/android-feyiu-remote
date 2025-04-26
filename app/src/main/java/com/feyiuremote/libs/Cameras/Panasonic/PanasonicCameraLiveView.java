package com.feyiuremote.libs.Cameras.Panasonic;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.feyiuremote.libs.Cameras.abstracts.Connection.ICameraControlListener;
import com.feyiuremote.libs.LiveStream.image.JpegPacket;
import com.feyiuremote.libs.LiveStream.image.LiveFeedReceiver;
import com.feyiuremote.libs.Utils.NamedThreadFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PanasonicCameraLiveView {
    private static final String TAG = "PanasonicCameraLiveView";
    public static final int PORT = 49152;
    private static final int RECEIVE_BUFFER_SZ = 2_000_000;
    private static final int TIMEOUT_MS = 500;
    private static final int PACKET_QUEUE_CAP = 128;
    private static final int FRAME_QUEUE_CAP = 4;
    private static final int MAX_EXCEPTIONS_RESTART = 15;
    private static final int MAX_EXCEPTIONS_HALT = 30;

    // Executors for 4-stage pipeline
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor(
            new NamedThreadFactory("Net-I/O"));
    private final ExecutorService decodeExecutor = Executors.newSingleThreadExecutor(
            new NamedThreadFactory("Decoder"));
    private final ExecutorService trackExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            new NamedThreadFactory("Tracker"));
    private final ExecutorService displayExecutor = Executors.newSingleThreadExecutor(
            new NamedThreadFactory("Display-Loop"));

    // Bounded queues for back-pressure
    private final BlockingQueue<DatagramPacket> packetQueue =
            new ArrayBlockingQueue<>(PACKET_QUEUE_CAP);
    private final BlockingQueue<PanasonicCameraFrame> frameForDisplayQueue =
            new ArrayBlockingQueue<>(FRAME_QUEUE_CAP);
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final JpegPacket jpegPacket = new JpegPacket();

    private final PanasonicCamera mCamera;
    private final LiveFeedReceiver mLiveViewReceiver;

    private volatile boolean streamActive = false;

    // Stream-restart & error-counting fields
    private int exceptionsRestart = 0;
    private int exceptionsHalt = 0;
    private int frames = 0;
    private int frameSinceStreamRequest = 0;
    private boolean requestingStream = false;
    private int requestRetries = 0;

    public PanasonicCameraLiveView(PanasonicCamera camera,
                                   LiveFeedReceiver receiver) {
        this.mCamera = camera;
        this.mLiveViewReceiver = receiver;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            stop();
        } finally {
            super.finalize();
        }
    }

    public void start() {
        streamActive = true;
        startNetworkLoop();
        startDecoderLoop();
        startDisplayLoop();
    }

    public void stop() {
        streamActive = false;
        networkExecutor.shutdownNow();
        decodeExecutor.shutdownNow();
        displayExecutor.shutdownNow();
    }

    public boolean isActive() {
        return streamActive;
    }

    //-------------------------------------------------------------------------
    // 1) Network I/O: read UDP packets, restart stream if needed
    //-------------------------------------------------------------------------
    private void startNetworkLoop() {
        networkExecutor.execute(() -> {
            Log.d(TAG, "Network loop starting");
            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket(PORT);
                socket.setReuseAddress(true);
                socket.setSoTimeout(TIMEOUT_MS);
                byte[] buf = new byte[RECEIVE_BUFFER_SZ];
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);

                while (streamActive && exceptionsHalt < MAX_EXCEPTIONS_HALT) {
                    try {
                        // Prolong the camera stream periodically
                        if (frameSinceStreamRequest > 200 || exceptionsRestart > MAX_EXCEPTIONS_RESTART) {
                            Log.d(TAG, "frame:" + frameSinceStreamRequest + " exceptions restart:" + exceptionsRestart);
                            frameSinceStreamRequest = 0;
                            exceptionsRestart = 0;
                            requestStream();
                        }

                        long refTime = System.currentTimeMillis();
                        socket.receive(pkt);
                        long refEndTime = System.currentTimeMillis() - refTime;
                        if (refEndTime > 20) {
                            Log.w(TAG, "Long frame packet time:" + refEndTime);
                        }

                        byte[] data = Arrays.copyOf(pkt.getData(), pkt.getLength());
                        DatagramPacket copy = new DatagramPacket(data, data.length);
                        if (!packetQueue.offer(copy)) {
                            packetQueue.poll(); // drop oldest
                            packetQueue.offer(copy);
                        }

                        frames++;
                        frameSinceStreamRequest++;
                        exceptionsHalt = 0;
                        exceptionsRestart = 0;

                    } catch (SocketTimeoutException e) {
                        exceptionsHalt++;
                        mLiveViewReceiver.onWarning("Stream Socket Timed Out:" + exceptionsHalt);
                    } catch (IOException e) {
                        exceptionsHalt++;
                        mLiveViewReceiver.onError("Network I/O error: " + e.getMessage());
                        streamActive = false;
                    } catch (Exception e) {
                        exceptionsHalt++;
                        mLiveViewReceiver.onError("Unknown stream error: " + e.getMessage());
                        streamActive = false;
                    }
                }
            } catch (SocketException e) {
                mLiveViewReceiver.onError("Could not open socket: " + e.getMessage());
            } finally {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                    mLiveViewReceiver.onInfo("Socket closed due to endless timeouts!");
                }
                mLiveViewReceiver.onError("Stream has finished.");
            }
            Log.d(TAG, "Network loop exiting");
        });
    }

    //-------------------------------------------------------------------------
    // 2) Decoder: turn raw JPEG packets into Frame objects
    //-------------------------------------------------------------------------
    private void startDecoderLoop() {
        decodeExecutor.execute(() -> {
            Log.d(TAG, "Decoder loop starting");
            while (streamActive) {
                try {
                    DatagramPacket pkt = packetQueue.take();
                    Bitmap bmp = jpegPacket.getBitmapIfAvailable(pkt.getData(), pkt.getLength());
                    if (bmp == null) continue;

                    PanasonicCameraFrame f = new PanasonicCameraFrame(bmp);
                    if (!frameForDisplayQueue.offer(f)) {
                        frameForDisplayQueue.poll();
                        frameForDisplayQueue.offer(f);
                    }
                } catch (InterruptedException ignored) {
                    break;
                }
            }
            Log.d(TAG, "Decoder loop exiting");
        });
    }

    //-------------------------------------------------------------------------
    // 3) Display: consume decoded frames and post to the UI / Other processors
    //-------------------------------------------------------------------------
    private void startDisplayLoop() {
        displayExecutor.execute(() -> {
            Log.d(TAG, "Display loop starting");
            int displayFrames = 0;
            while (streamActive) {
                try {
                    PanasonicCameraFrame f = frameForDisplayQueue.take();
                    uiHandler.post(() -> mLiveViewReceiver.onNewFrame(f));
                    Log.d(TAG, "New frame received");
                    // Throttle display to balance latency vs. frame-rate
                    displayFrames++;
                    Thread.sleep(displayFrames % 4 == 0 ? 25L : 33L);
                } catch (InterruptedException e) {
                    break;
                }
            }
            Log.d(TAG, "Display loop exiting");
        });
    }

    /**
     * After ~200 frames or repeated errors, re-request the live stream.
     */
    private void requestStream() {
        if (!requestingStream) {
            requestingStream = true;
            mCamera.controls.updateModeState(new ICameraControlListener() {
                @Override
                public void onSuccess() {
                    requestingStream = false;
                    frameSinceStreamRequest = 0;
                    Log.d(TAG, "Stream request prolonged");
                    mLiveViewReceiver.onInfo("Stream has been prolonged.");
                }

                @Override
                public void onFailure() {
                    requestingStream = false;
                    requestRetries++;
                    Log.e(TAG, "Stream request failed, retry " + requestRetries);
                    if (requestRetries < 3) {
                        mLiveViewReceiver.onInfo("Stream request failed, retrying: " + requestRetries);
                        requestStream();
                    } else {
                        mLiveViewReceiver.onError("Stream request failed!");
                        stop();
                    }
                }
            });
        }
    }

}
