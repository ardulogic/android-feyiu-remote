package com.feyiuremote.libs.Cameras.Panasonic;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.feyiuremote.libs.Cameras.abstracts.Connection.ICameraControlListener;
import com.feyiuremote.libs.LiveStream.abstracts.LiveFeedReceiver;
import com.feyiuremote.libs.LiveStream.image.JpegPacket;
import com.feyiuremote.libs.Utils.NamedThreadFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PanasonicCameraLiveView {
    private static final String TAG = "PanasonicCameraLiveView";
    public static final int PORT = 49152;
    private static final int RECEIVE_BUFFER_SZ = 150_000;
    private static final int TIMEOUT_MS = 500;
    private static final int PACKET_QUEUE_CAP = 128;
    private static final int FRAME_QUEUE_CAP = 4;
    private static final int MAX_SOCKET_EXCEPTIONS_UNTIL_STOP = 6;

    // Executors for 4-stage pipeline
    private ExecutorService networkExecutor;
    private ExecutorService decodeExecutor;
    private ExecutorService displayExecutor;

    // Bounded queues for back-pressure
    private final BlockingQueue<DatagramPacket> decodingQueue =
            new ArrayBlockingQueue<>(PACKET_QUEUE_CAP);
    private final BlockingQueue<PanasonicCameraFrame> frameForDisplayQueue =
            new ArrayBlockingQueue<>(FRAME_QUEUE_CAP);
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final JpegPacket jpegPacket = new JpegPacket();

    private final PanasonicCamera mCamera;
    private final LiveFeedReceiver mLiveViewReceiver;

    private volatile boolean streamActive = false;

    // Stream-restart & error-counting fields
    private int frameSinceStreamRequest = 0;
    private int framesSinceSleepPrevention = 0;
    private boolean prolongingStream = false;
    private int prolongingStreamRetries = 0;
    private int restartingStreamRetries = 0;
    private boolean preventingSleep = false;
    private int preventingSleepRetries = 0;
    private final boolean autoRestartStream = true;

    public PanasonicCameraLiveView(PanasonicCamera camera,
                                   LiveFeedReceiver receiver) {
        this.mCamera = camera;
        this.mLiveViewReceiver = receiver;
    }

    protected void setStreamActive(boolean value) {
        if (this.streamActive != value) {
            this.streamActive = value;
            this.mLiveViewReceiver.onIsStreamingChanged(value);
        }
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
        if (streamActive) return;
        setStreamActive(true);

        // (re)create executors
        networkExecutor = Executors.newSingleThreadExecutor(
                new NamedThreadFactory("Net-I/O"));
        decodeExecutor = Executors.newSingleThreadExecutor(
                new NamedThreadFactory("Camera Frame Decode"));
        displayExecutor = Executors.newSingleThreadExecutor(
                new NamedThreadFactory("Camera Frame Display"));

        // reset queues & flags
        decodingQueue.clear();
        frameForDisplayQueue.clear();
        frameSinceStreamRequest = 0;
        prolongingStream = false;
        prolongingStreamRetries = restartingStreamRetries = 0;

        startNetworkLoop();
        startDecoderLoop();
        startDisplayLoop();
    }

    public void stop() {
        if (!streamActive) return;
        setStreamActive(false);

        if (mLiveViewReceiver != null) {
            mLiveViewReceiver.onStop("Complete stop.");
        }

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
        framesSinceSleepPrevention = 0;

        networkExecutor.execute(() -> {
            Log.d(TAG, "Network loop starting");

            int socketLoopExceptions = 0;

            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket(PORT);
                socket.setReuseAddress(true);
                socket.setSoTimeout(TIMEOUT_MS);
                byte[] buf = new byte[RECEIVE_BUFFER_SZ];
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);

                long sleepTime = 0;
                long pktRecvTimeStart, pktRecvTime = 0;

                while (streamActive && socketLoopExceptions < MAX_SOCKET_EXCEPTIONS_UNTIL_STOP) {
                    try {
                        sleepTime = 25;

                        pktRecvTimeStart = System.currentTimeMillis();
                        socket.receive(pkt);
                        pktRecvTime = System.currentTimeMillis() - pktRecvTimeStart;

                        if (pktRecvTime > 60) {
                            Log.w(TAG, "Long frame packet time:" + pktRecvTime);
                            sleepTime = 0;
                        }

                        if (decodingQueue.isEmpty()) {
                            byte[] data = Arrays.copyOf(pkt.getData(), pkt.getLength());
                            DatagramPacket copy = new DatagramPacket(data, data.length);
                            decodingQueue.offer(copy);
                        } else {
                            // Connection seems more stable if we receive the packet
                            Log.w(TAG, "Skipped frame due to decoding:" + decodingQueue.size());
                        }

                        frameSinceStreamRequest++;
                        framesSinceSleepPrevention++;
                        socketLoopExceptions = 0;

                        // Prolong the camera stream periodically
                        if (frameSinceStreamRequest > 100 && !prolongingStream) {
                            Log.w(TAG, "Prolonging stream...");
                            prolongStream();
                        }

                        if (framesSinceSleepPrevention > 5000 && !preventingSleep) {
                            Log.w(TAG, "Preventing from sleeping...");
                            preventFromSleeping();
                        }

                        Thread.sleep(sleepTime);
                    } catch (SocketTimeoutException e) {
                        socketLoopExceptions++;
                        mLiveViewReceiver.onWarning("Stream Socket Timed Out:" + socketLoopExceptions);
                    } catch (IOException e) {
                        socketLoopExceptions++;
                        mLiveViewReceiver.onError("Network I/O error: " + e.getMessage());
                    } catch (Exception e) {
                        socketLoopExceptions++;
                        mLiveViewReceiver.onError("Unknown stream error: " + e.getMessage());
                    }
                }
            } catch (SocketException e) {
                mLiveViewReceiver.onError("Could not open socket: " + e.getMessage());
            } finally {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }

                mLiveViewReceiver.onStop("Stream has finished. Auto restart: " + autoRestartStream + " isActive:" + isActive());

                if (autoRestartStream && isActive()) {
                    restartStream();
                } else {
                    stop();
                }
            }

            Log.d(TAG, "Network loop exiting");
        });
    }


    private void startNetworkLoopNio() {
        networkExecutor.execute(() -> {
            Log.d(TAG, "Network loop (NIO) starting");

            int socketLoopExceptions = 0;

            try (DatagramChannel channel = DatagramChannel.open()) {
                channel.configureBlocking(false);
                channel.socket().bind(new InetSocketAddress(PORT));
                channel.socket().setReuseAddress(true);
                channel.socket().setReceiveBufferSize(RECEIVE_BUFFER_SZ);

                Selector selector = Selector.open();
                channel.register(selector, SelectionKey.OP_READ);

                ByteBuffer buffer = ByteBuffer.allocate(RECEIVE_BUFFER_SZ); // Use heap buffer

                while (streamActive && socketLoopExceptions < MAX_SOCKET_EXCEPTIONS_UNTIL_STOP) {
                    try {
                        int readyChannels = selector.select(10); // Short timeout

                        if (readyChannels == 0) {
                            socketLoopExceptions++;
                            mLiveViewReceiver.onWarning("NIO timeout: " + socketLoopExceptions);
                            continue;
                        }

                        Set<SelectionKey> selectedKeys = selector.selectedKeys();
                        Iterator<SelectionKey> iter = selectedKeys.iterator();

                        while (iter.hasNext()) {
                            SelectionKey key = iter.next();
                            iter.remove(); // Remove key early to avoid stale entries

                            if (key.isReadable()) {
                                while (true) {
                                    buffer.clear();
                                    SocketAddress sender = channel.receive(buffer);
                                    if (sender == null) break; // No more packets

                                    buffer.flip();
                                    byte[] data = new byte[buffer.remaining()];
                                    buffer.get(data);

                                    DatagramPacket packet = new DatagramPacket(data, data.length);

                                    if (!decodingQueue.offer(packet)) {
                                        decodingQueue.poll(); // Drop oldest
                                        decodingQueue.offer(packet);
                                        Log.w(TAG, "Dropped old packet: queue full");
                                    }

                                    frameSinceStreamRequest = 0;
                                    socketLoopExceptions = 0;
                                }
                            }
                        }

                        // Prolong stream check
                        if (frameSinceStreamRequest > 200 && !prolongingStream) {
                            Log.w(TAG, "Prolonging stream...");
                            prolongStream();
                        }

                        frameSinceStreamRequest++;

                    } catch (IOException e) {
                        socketLoopExceptions++;
                        mLiveViewReceiver.onError("NIO I/O error: " + e.getMessage());
                    } catch (Exception e) {
                        socketLoopExceptions++;
                        mLiveViewReceiver.onError("Unknown NIO error: " + e.getMessage());
                    }
                }

                selector.close();

            } catch (IOException e) {
                mLiveViewReceiver.onError("Failed to open NIO DatagramChannel: " + e.getMessage());
            }

            mLiveViewReceiver.onStop("Stream has finished. Auto restart: " + autoRestartStream + " isActive: " + isActive());

            if (autoRestartStream && isActive()) {
                restartStream();
            } else {
                stop();
            }

            Log.d(TAG, "Network loop (NIO) exiting");
        });
    }


    //-------------------------------------------------------------------------
    // 2) Decoder: turn raw JPEG packets into Frame objects
    //-------------------------------------------------------------------------
    private void startDecoderLoop() {
        decodeExecutor.execute(() -> {
            Log.d(TAG, "Decoder loop starting");
            DatagramPacket pkt;
            while (streamActive) {
                try {
                    pkt = decodingQueue.take();
                    Bitmap bmp = jpegPacket.getBitmapIfAvailable(pkt.getData(), pkt.getLength());
                    if (bmp == null) {
                        mLiveViewReceiver.onInfo("Could not decode jpeg packet :(");
                        continue;
                    }

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
            while (streamActive) {
                try {
                    PanasonicCameraFrame f = frameForDisplayQueue.take();
                    uiHandler.post(() -> mLiveViewReceiver.onNewFrame(f));
//                    Log.d(TAG, "New frame received");

                    // Throttle display to balance latency vs. frame-rate
                    Thread.sleep(5L);
                } catch (InterruptedException e) {
                    break;
                }
            }
            Log.d(TAG, "Display loop exiting");
        });
    }

    private void restartStream() {
        mCamera.controls.startStream(new ICameraControlListener() {
            @Override
            public void onSuccess() {
                startNetworkLoop();
                // Non-blocking method, but seems to drop more frames
//                startNetworkLoopNio();
            }

            @Override
            public void onFailure() {
                restartingStreamRetries++;

                Log.e(TAG, "Stream restart failed, retries: " + restartingStreamRetries);
                if (restartingStreamRetries < 60) {
                    mLiveViewReceiver.onInfo("Trying to restart stream (" + restartingStreamRetries + "/60)");
                    restartStream();
                } else {
                    mLiveViewReceiver.onError("Stream restart attempt failed!");
                    stop();
                }
            }
        });
    }

    /**
     * After ~200 frames or repeated errors, re-request the live stream.
     */
    private void prolongStream() {
        if (!prolongingStream) {
            prolongingStream = true;
            mCamera.controls.updateModeState(new ICameraControlListener() {
                @Override
                public void onSuccess() {
                    prolongingStream = false;
                    frameSinceStreamRequest = 0;
                    Log.d(TAG, "Stream request prolonged");
                    mLiveViewReceiver.onInfo("Stream has been prolonged.");
                }

                @Override
                public void onFailure() {
                    prolongingStream = false;
                    prolongingStreamRetries++;
                    Log.e(TAG, "Stream request failed, retry " + prolongingStreamRetries);
                    if (prolongingStreamRetries < 3) {
                        mLiveViewReceiver.onInfo("Stream request failed, retrying: " + prolongingStreamRetries);
                        prolongStream();
                    } else {
                        mLiveViewReceiver.onError("Stream request failed!");
                        stop();
                    }
                }
            });
        }
    }

    private void preventFromSleeping() {
        if (!preventingSleep) {
            preventingSleep = true;
            mCamera.controls.keepStreamAlive(new ICameraControlListener() {
                @Override
                public void onSuccess() {
                    preventingSleep = false;
                    framesSinceSleepPrevention = 0;
                    Log.d(TAG, "Stream request prolonged");
                    mLiveViewReceiver.onInfo("Stream has been prolonged.");
                }

                @Override
                public void onFailure() {
                    preventingSleep = false;
                    preventingSleepRetries++;
                    Log.e(TAG, "Stream request failed, retry " + preventingSleepRetries);
                    if (preventingSleepRetries < 3) {
                        mLiveViewReceiver.onInfo("Stream sleep prevention failed, retrying: " + preventingSleepRetries);
                        preventFromSleeping();
                    } else {
                        mLiveViewReceiver.onError("Stream sleep prevention ignored...");
                        framesSinceSleepPrevention = 0;
                    }
                }
            });
        }
    }

}
