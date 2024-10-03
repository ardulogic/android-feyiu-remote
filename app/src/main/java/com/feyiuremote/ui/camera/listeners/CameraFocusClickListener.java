package com.feyiuremote.ui.camera.listeners;

import android.util.Log;
import android.view.View;

import com.feyiuremote.libs.AI.trackers.POI;
import com.feyiuremote.libs.Cameras.abstracts.Connection.ICameraControlListener;
import com.feyiuremote.ui.camera.CameraViewModel;

public class CameraFocusClickListener implements View.OnClickListener {
    private final String TAG = CameraFocusClickListener.class.getSimpleName();
    private CameraViewModel cameraViewModel;

    public CameraFocusClickListener(CameraViewModel viewModel) {
        this.cameraViewModel = viewModel;
    }

    private void focusOnPOI(POI poi) {
        cameraViewModel.camera.getValue().controls.touchFocus(poi.centerXPerc(), poi.centerYPerc(),
                new ICameraControlListener() {
                    @Override
                    public void onSuccess() {
                        cameraViewModel.status.postValue("Focused on POI");
                    }

                    @Override
                    public void onFailure() {
                        cameraViewModel.status.postValue("Focus on POI failed!");
                        Log.e(TAG, "POI focus failed");
                    }
                });
    }

    private void autoFocusMf() {
        cameraViewModel.camera.getValue().controls.autoFocusMf(new ICameraControlListener() {
            @Override
            public void onSuccess() {
                cameraViewModel.status.postValue("Focus successful!");
            }

            @Override
            public void onFailure() {
                cameraViewModel.status.postValue("Auto-Focus failed!");
                Log.e(TAG, "Auto-Focus (on MF) failed");
            }
        });
    }

    @Override
    public void onClick(View view) {
        if (cameraViewModel.camera.getValue() != null) {
            if (cameraViewModel.unifiedTrackingProcessor.getValue() != null) {
                POI poi = cameraViewModel.unifiedTrackingProcessor.getValue().getPOI();
                if (poi != null) {
                    focusOnPOI(poi);
                } else {
                    autoFocusMf();
                }
            } else {
                autoFocusMf();
            }
        } else {
            cameraViewModel.status.setValue("Camera is not available!");
        }
    }
}