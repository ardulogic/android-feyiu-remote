package com.feyiuremote.libs.Cameras.Panasonic;

import static com.feyiuremote.libs.Cameras.abstracts.Connection.SSDPConnection.findParameterValue;

import android.content.Context;
import android.util.Log;

import com.feyiuremote.libs.Cameras.abstracts.Connection.CameraDiscovery;
import com.feyiuremote.libs.Cameras.abstracts.Connection.ISSDPDiscoveryListener;
import com.feyiuremote.libs.Cameras.abstracts.Connection.SSDPConnection;
import com.feyiuremote.libs.Cameras.abstracts.State.Camera;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

public class PanasonicCameraDiscovery extends CameraDiscovery {

    private static final String TAG = PanasonicCameraDiscovery.class.getSimpleName();

    private final ArrayList<String> foundCamUrls = new ArrayList<>();

    public PanasonicCameraDiscovery(ExecutorService executor) {
        super(executor);
    }

    public void clear() {
//        executor.shutdownNow();
        for (Camera cam : foundCameras) {
            cam.close();
        }

        foundCameras.clear();
        foundCamUrls.clear();
    }

    public void search(Context context, IPanasonicCameraDiscoveryListener listener) {
        try {
            SSDPConnection ssdp = new SSDPConnection(context, this.executor);

            ssdp.inquire(new ISSDPDiscoveryListener() {
                @Override
                public void onFinish() {
                    listener.onProgressUpdate("Discovery finished.");
                    listener.onFinish(foundCamUrls);
                }

                @Override
                public void onDeviceFound(String ssdpReplyMessage) {
                    String ddUsn = findParameterValue(ssdpReplyMessage, "USN");

                    Log.v(TAG, "- - - - - - - USN : " + ddUsn);
                    if (ddUsn != null) {
                        String ddLocation = findParameterValue(ssdpReplyMessage, "LOCATION");

                        //// Fetch Device Description XML and parse it.
                        if (ddLocation != null) {
                            if (!foundCamUrls.contains(ddLocation)) {
                                this.onProgressUpdate("New camera found: " + ddLocation);

                                PanasonicCamera cam = new PanasonicCamera(context, ddLocation);
                                foundCamUrls.add(ddLocation);
                                foundCameras.add(cam);

                                listener.onDeviceFound(cam);
                            } else {
                                listener.onProgressUpdate("Previous camera found: " + ddLocation);
                            }
                        }
                    }
                }

                @Override
                public void onProgressUpdate(String response) {
                    listener.onProgressUpdate(response);
                }

                @Override
                public void onFailure(String response) {
                    listener.onFailure(response);
                }
            });
        } catch (Exception e) {
            listener.onFailure(e.toString());
        }
    }

}
