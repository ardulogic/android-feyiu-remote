package com.feyiuremote.libs.Cameras.Panasonic;

import android.util.Log;

import com.feyiuremote.libs.Cameras.abstracts.Connection.ICameraControlListener;
import com.feyiuremote.libs.LiveStream.image.LiveFeedReceiver;
import com.feyiuremote.libs.LiveStream.image.RawImage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;

import static java.util.Arrays.copyOfRange;

public class PanasonicCameraLiveView {

    private final static String TAG = PanasonicCameraLiveView.class.getSimpleName();

    public static final int PORT = 49152;  // Base port number : 49152
    private LiveFeedReceiver mLiveViewReceiver;
    private PanasonicCamera mCamera;
    private ExecutorService executor;
    private Integer frames;
    private boolean streamActive;

    private final int MAX_EXCEPTIONS = 1000;
    private final int RECEIVE_BUFFER_SIZE = 1024 * 1024 * 4;
    private final int TIMEOUT_FRAME = 33;

    private boolean requestingStream = false;
    private int requestRetries = 0;


    public PanasonicCameraLiveView(ExecutorService executor, PanasonicCamera camera, LiveFeedReceiver receiver) {
        this.executor = executor;
        this.mCamera = camera;
        this.mLiveViewReceiver = receiver;
    }

    /**
     * Opens UDP port and starts receiver thread
     */
    public void start() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                listenToUdpSocket();
            }
        });
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
        int exceptions = 0; frames = 0;

        DatagramSocket socket = null;
        byte[] buffer = new byte[RECEIVE_BUFFER_SIZE];
        DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

        try {
            socket = new DatagramSocket(PORT);
            socket.setReuseAddress(true);
            streamActive = true;

            while (streamActive && exceptions < MAX_EXCEPTIONS) {
                try {
                    socket.setSoTimeout(TIMEOUT_FRAME);
                    socket.receive(receivePacket);

                    onImagePacketReceived(receivePacket);
                    exceptions = 0;
                    frames++;

                    if (frames > 250) {
                        this.requestStream();
                    }

                } catch (SocketTimeoutException e) {
                    exceptions++;
//                    mLiveViewListener.onWarning("Stream Socket Timed Out:" + exceptions);
                } catch (IOException e) {
                    exceptions++;
                    mLiveViewReceiver.onError("Stream IO exception:" + exceptions);
                    e.printStackTrace();
                    break;
                }
            }
        } catch (SocketException e) {
            mLiveViewReceiver.onError("Could not open socket!");
            e.printStackTrace();
        } finally {
            if (socket != null) {
                if (!socket.isClosed()) {
                    socket.close();
                }

                mLiveViewReceiver.onInfo("Socket has been closed.");
            }
        }

        mLiveViewReceiver.onError("Stream has finished.");
    }

    /**
     * After ~300 frames camera halts the stream
     * it needs to be requested again
     */
    private void requestStream() {
        if (!this.requestingStream) {
            this.requestingStream = true;

            this.mCamera.updateModeState(new ICameraControlListener() {
                @Override
                public void onSuccess() {
                    frames = 0;
                    requestingStream = false;
                    mLiveViewReceiver.onInfo("Stream has been prolonged.");
                }

                @Override
                public void onFailure() {
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

    /**
     * Handles raw UDP packet
     *
     * @param packet
     */
    private void onImagePacketReceived(DatagramPacket packet) {
        int dataLength = packet.getLength();
        int searchIndex = 0;
        int startPosition = 0;

        byte[] startMarker = new byte[]{(byte) 255, (byte) 216};
        byte[] receivedData = packet.getData();

        // Wait for the first jpeg "header/marking"
        while (startPosition < dataLength) {
            if (receivedData[startPosition++] == startMarker[searchIndex]) {
                searchIndex++;
                if (searchIndex >= startMarker.length) {
                    break;
                }
            } else {
                searchIndex = 0;
            }
        }

        int offset = startPosition - startMarker.length;

        mLiveViewReceiver.onNewRawFrame(
                new RawImage(copyOfRange(receivedData, offset, dataLength), 0)
        );
    }

}