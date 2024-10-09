package com.feyiuremote.libs.Cameras.Panasonic;

import android.graphics.Bitmap;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;

public class PanasonicCameraLiveView {

    private final static String TAG = PanasonicCameraLiveView.class.getSimpleName();

    public static final int PORT = 49152;  // Base port number : 49152
    private LiveFeedReceiver mLiveViewReceiver;
    private PanasonicCamera mCamera;
    protected static final ThreadFactory threadFactory = new NamedThreadFactory("PanasonicCameraLiveView");

    protected static final ExecutorService executor = Executors.newFixedThreadPool(3, threadFactory);

    // Finalize method
    @Override
    protected void finalize() throws Throwable {
        try {
            executor.shutdownNow();
        } finally {
            super.finalize();
        }
    }

    private Integer frames;

    private Integer frameSinceStreamRequest = 0;
    private boolean streamActive;

    private final int MAX_EXCEPTIONS_RESTART = 15;
    private final int MAX_EXCEPTIONS_HALT = 30;

    private int exceptionsRestart = 0;
    private int exceptionsHalt = 0;

    private final int RECEIVE_BUFFER_SIZE = 2000000;
    private final int TIMEOUT_FRAME = 500;

    private boolean requestingStream = false;
    private int requestRetries = 0;

    private final JpegPacket jpegPacket;


    public PanasonicCameraLiveView(PanasonicCamera camera, LiveFeedReceiver receiver) {
        this.mCamera = camera;
        this.mLiveViewReceiver = receiver;
        this.jpegPacket = new JpegPacket();
    }

    /**
     * Opens UDP port and starts receiver thread
     */
    public void start() {
        executor.execute(this::listenToUdpSocket);
    }

    public void stop() {
        streamActive = false;
    }

    public boolean isActive() {
        return streamActive;
    }

    /**
     * Panasonic broadcasts on UDP port
     * DatagramSocket listens to that stream
     */
    private void listenToUdpSocket() {
        Log.d(TAG, "Listening to UDP socket");


        this.withLiveViewReceiver(() -> {
            frames = 0;
            frameSinceStreamRequest = 0;

            DatagramSocket socket = null;
            byte[] buffer = new byte[RECEIVE_BUFFER_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

            try {
                streamActive = true;

                socket = new DatagramSocket(PORT);
                socket.setReuseAddress(true);
                socket.setSoTimeout(TIMEOUT_FRAME);

                while (streamActive && exceptionsHalt < MAX_EXCEPTIONS_HALT) {
                    try {
                        if (frameSinceStreamRequest > 200 || exceptionsRestart > MAX_EXCEPTIONS_RESTART) {
                            Log.d(TAG, "frame:" + frameSinceStreamRequest + " exceptions restart:" + exceptionsRestart);

                            frameSinceStreamRequest = 0;
                            exceptionsRestart = 0;

                            PanasonicCameraControls.executor.execute(this::requestStream);
                        }

                        long refTime = System.currentTimeMillis();

                        // IT will receive only one jpeg no matter the buffer size
                        socket.receive(receivePacket);

                        long refEndTime = System.currentTimeMillis() - refTime;
                        if (refEndTime > 20) {
                            Log.w(TAG, "Long frame packet time:" + refEndTime);
                        }

                        refTime = System.currentTimeMillis();

                        // It used to block this loop, so its better to launch handling
                        // on a separate thread
                        executor.execute(() -> handleImagePacket(receivePacket.getData(), receivePacket.getLength()));

                        refEndTime = System.currentTimeMillis() - refTime;
                        if (refEndTime > 20) {
                            Log.w(TAG, "Long frame process time:" + refEndTime);
                        }

                        frames++;
                        frameSinceStreamRequest++;

                        // This is not the regular latency we're talking about. It's the camera latency
                        // which increases/decreases based on the delay between frame requests

                        // If 33ms (30fps) is used, for some reason it becomes 1sec latency
                        // 33 - Bad latency, but very stable Thread.sleep(Math.max(1L, 33 - timeForFrameRead));
//                        Thread.sleep(frames % 4 == 0 ? 25L : 33L); // Holy grail - no delayed frames, and acceptable latency
                        Thread.sleep(frames % 4 == 0 ? 25L : 33L); // I dont know if sleeping the thread causes inconsistencies

                        exceptionsHalt = 0;
                        exceptionsRestart = 0;
                    } catch (Exception e) {
                        exceptionsHalt++;
                        exceptionsRestart++;

                        if (e instanceof SocketTimeoutException) {
                            mLiveViewReceiver.onWarning("Stream Socket Timed Out:" + exceptionsHalt);
                        } else if (e instanceof InterruptedException) {
                            mLiveViewReceiver.onError("Stream Thread Interrupted.");
                            streamActive = false;
                        } else if (e instanceof SocketException) {
                            mLiveViewReceiver.onError("Could not open/use socket!");
                            streamActive = false;
                        } else if (e instanceof IOException) {
                            mLiveViewReceiver.onError("Could not open socket!");
                            streamActive = false;
                        } else if (e instanceof RejectedExecutionException) {
                            mLiveViewReceiver.onError("Could not execute job");
                            streamActive = false;
                        } else {
                            mLiveViewReceiver.onError("Unknown stream error.");
                            streamActive = false;
                        }
                    }
                }
            } catch (SocketException e) {
                Log.d(TAG, "Socket exception!");
                streamActive = false;
            } finally {
                if (socket != null) {
                    if (!socket.isClosed()) {
                        socket.close();
                    }

                    Log.d(TAG, "Socket closed due to endless timeouts!");
                    mLiveViewReceiver.onInfo("Socket timeout limit reached.");
                }
            }

            mLiveViewReceiver.onError("Stream has finished.");
        });
    }

    private void withLiveViewReceiver(Runnable r) {
        if (mLiveViewReceiver != null) {
            r.run();
        } else {
            Log.e(TAG, "Live view receiver was not set!");
        }
    }

    /**
     * After ~300 frames camera halts the stream
     * it needs to be requested again
     */
    private void requestStream() {
        if (!this.requestingStream) {
            this.requestingStream = true;

            this.mCamera.controls.updateModeState(new ICameraControlListener() {
                @Override
                public void onSuccess() {
                    requestingStream = false;
                    Log.d(TAG, "Stream request prolonged");
                    mLiveViewReceiver.onInfo("Stream has been prolonged.");
                }

                @Override
                public void onFailure() {
                    Log.e(TAG, "Stream request failed");

                    requestingStream = false;
                    requestRetries++;

                    if (requestRetries < 3) {
                        mLiveViewReceiver.onInfo("Stream request failed, retrying..:" + requestRetries);
                        requestStream();
                    } else {
                        mLiveViewReceiver.onError("Stream request failed!");
                        stop();
                    }
                }
            });
        }
    }

    private void handleImagePacket(byte[] packet, int length) {
        Bitmap bitmap = jpegPacket.getBitmapIfAvailable(packet, length);

        if (bitmap != null) {
            mLiveViewReceiver.onNewFrame(bitmap);
        }
    }

}