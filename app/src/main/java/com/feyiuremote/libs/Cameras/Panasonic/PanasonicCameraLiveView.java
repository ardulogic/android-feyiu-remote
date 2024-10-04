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

    private final int MAX_EXCEPTIONS = 1000;
    private final int RECEIVE_BUFFER_SIZE = 2000000;
    private final int TIMEOUT_FRAME = 500;

    private boolean requestingStream = false;
    private int requestRetries = 0;

    final int JPEG_START_INDEX = 188;

    byte[] tempBuffer = new byte[RECEIVE_BUFFER_SIZE];

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
            int exceptions = 0;
            frames = 0;
            frameSinceStreamRequest = 0;

            DatagramSocket socket = null;
            byte[] buffer = new byte[RECEIVE_BUFFER_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

            try {
                streamActive = true;

                Long lastFrameTime = System.currentTimeMillis();
                socket = new DatagramSocket(PORT);
                socket.setReuseAddress(true);
                socket.setSoTimeout(TIMEOUT_FRAME);

                while (streamActive && exceptions < MAX_EXCEPTIONS) {
                    long frameStartRead = System.currentTimeMillis();

                    try {
                        if (frameSinceStreamRequest > 200) {
                            PanasonicCameraControls.executor.execute(() -> {
                                requestStream();
                                frameSinceStreamRequest = 0;
                            });
                        }

                        // IT will receive only one jpeg no matter the buffer size
                        socket.receive(receivePacket);

                        // It used to block this loop, so its better to launch handling
                        // on a separate thread
                        executor.execute(() -> handleImagePacket(receivePacket.getData(), receivePacket.getLength()));

                        exceptions = 0;
                        frames++;
                        frameSinceStreamRequest++;

                        long timeForFrameRead = System.currentTimeMillis() - frameStartRead;
                        if (timeForFrameRead > 10) {
                            Log.w(TAG, "Frame read time:" + timeForFrameRead);
                        }

                        // If 33ms (30fps) is used, for some reason it becomes 1sec latency
                        // 33 - Bad latency, but very stable Thread.sleep(Math.max(1L, 33 - timeForFrameRead));
//                        Thread.sleep(frames % 4 == 0 ? 25L : 33L); // Holy grail - no delayed frames, and acceptable latency
                        Thread.sleep(Math.max(33L - timeForFrameRead, 5L));
//                        Thread.sleep(20L);
                    } catch (SocketTimeoutException e) {
                        Log.d(TAG, "Socket timeout");
                        exceptions++;
//                    mLiveViewListener.onWarning("Stream Socket Timed Out:" + exceptions);
                    } catch (SocketException e) {
                        mLiveViewReceiver.onError("Could not open socket!");
                        e.printStackTrace();
                    } catch (IOException e) {
                        exceptions++;
                        mLiveViewReceiver.onError("Stream IO exception:" + exceptions);
                        e.printStackTrace();
                        break;
                    } catch (RejectedExecutionException e) {
                        mLiveViewReceiver.onError("Could not execute job");
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }


                    long elapsedTime = System.currentTimeMillis() - lastFrameTime;
                    try {
                        if (frames % 30 == 0) {
                            double fps = (double) (1000 / (elapsedTime / frames));
                            Log.d(TAG, "Frames per second: " + fps + " / " + frames);
                        }
                    } catch (ArithmeticException e) {

                    }
                }
            } catch (SocketException e) {
                Log.d(TAG, "Socket exception!");
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