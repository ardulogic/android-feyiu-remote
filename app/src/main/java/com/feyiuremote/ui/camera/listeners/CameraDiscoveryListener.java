package com.feyiuremote.ui.camera.listeners;

import android.util.Log;

import com.feyiuremote.libs.Cameras.Panasonic.IPanasonicCameraDiscoveryListener;
import com.feyiuremote.libs.Cameras.Panasonic.IPanasonicCameraFocusListener;
import com.feyiuremote.libs.Cameras.Panasonic.PanasonicCamera;
import com.feyiuremote.libs.Cameras.abstracts.Connection.ICameraControlListener;
import com.feyiuremote.ui.camera.CameraViewModel;

import java.util.ArrayList;

public class CameraDiscoveryListener implements IPanasonicCameraDiscoveryListener {
    private final String TAG = CameraDiscoveryListener.class.getSimpleName();

    private final CameraViewModel cameraModel;
    private final Runnable onCameraReady;

    public CameraDiscoveryListener(CameraViewModel cameraModel, Runnable onCameraReady) {
        this.cameraModel = cameraModel;
        this.onCameraReady = onCameraReady;
    }

    @Override
    public void onDeviceFound(PanasonicCamera camera) {
        Log.i(TAG, "Camera Discovered:" + camera.state.url);

        cameraModel.camera.postValue(camera);
        cameraModel.status.postValue("Camera discovered: " + camera.state.url);

        // Requesting basic information from camera
        // this proves that its properly connected
        camera.updateBaseInfo(new ICameraControlListener() {
            @Override
            public void onSuccess() {
                cameraModel.status.postValue("Base info updated.");
                onCameraReady.run();

                camera.setFocusListener(new IPanasonicCameraFocusListener() {
                    @Override
                    public void onUpdate(double position) {
                        cameraModel.focus.postValue(position);
                        cameraModel.status.postValue("Focus has been acquired!");
                    }

                    @Override
                    public void onFailure() {
                        cameraModel.status.postValue("Failed to acquire focus!");
                    }

                    @Override
                    public void onTargetReached(double position) {

                    }
                });
            }

            @Override
            public void onFailure() {
                cameraModel.status.postValue("Could not update base info!");
                cameraModel.streamStarted.postValue(false);
            }
        });
    }


    @Override
    public void onProgressUpdate(String response) {
        cameraModel.status.postValue(response);
    }

    @Override
    public void onFailure(String response) {
        cameraModel.status.postValue(response);
    }


    @Override
    public void onFinish(ArrayList<String> foundCamUrls) {
        // No cameras were found when searching
        if (foundCamUrls.isEmpty()) {
            cameraModel.status.postValue("No cameras found!");
            cameraModel.streamStarted.postValue(false);
        }
    }


}