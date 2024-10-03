package com.feyiuremote.libs.Cameras.Panasonic;

import android.util.Log;

import com.feyiuremote.libs.Cameras.abstracts.Connection.ICameraControlListener;
import com.feyiuremote.libs.LiveStream.image.LiveFeedReceiver;
import com.feyiuremote.libs.LiveStream.image.RawImage;
import com.feyiuremote.libs.Utils.Debugger;
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

import static java.util.Arrays.copyOfRange;

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

    private RawImage rawImage;


    public PanasonicCameraLiveView(PanasonicCamera camera, LiveFeedReceiver receiver) {
        this.mCamera = camera;
        this.mLiveViewReceiver = receiver;
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


    // Function to count occurrences of a subarray in a byte array
    private static int countOccurrences(byte[] data, byte[] target) {
        int count = 0;
        for (int i = 0; i < data.length - target.length + 1; i++) {
            boolean match = true;
            for (int j = 0; j < target.length; j++) {
                if (data[i + j] != target[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                count++;
            }
        }
        return count;
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
                        Log.d(TAG, "Frame read time:" + timeForFrameRead);
                        // If 33ms (30fps) is used, for some reason it becomes 1sec latency
                        // 33 - Bad latency, but very stable Thread.sleep(Math.max(1L, 33 - timeForFrameRead));
                        Thread.sleep(frames % 4 == 0 ? 25L : 33L); // Holy grail - no delayed frames, and acceptable latency
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
//        byte[] p = new byte[length - JPEG_START_INDEX];
//        System.arraycopy(packet, JPEG_START_INDEX, p, 0, length - JPEG_START_INDEX);

        System.arraycopy(packet, JPEG_START_INDEX, tempBuffer, 0, length - JPEG_START_INDEX);

        if (rawImage != null) {
//            rawImage.update(p, 0, length - JPEG_START_INDEX);
            rawImage.update(tempBuffer, 0, length - JPEG_START_INDEX);
        } else {
            rawImage = new RawImage(tempBuffer, 0, length - JPEG_START_INDEX);
        }

        mLiveViewReceiver.onNewRawFrame(rawImage);
    }

}