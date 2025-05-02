package com.feyiuremote.ui.camera.listeners;

import android.util.Log;
import android.view.View;

import com.feyiuremote.libs.AI.trackers.POI;
import com.feyiuremote.libs.Cameras.abstracts.Connection.ICameraControlListener;
import com.feyiuremote.libs.LiveStream.abstracts.FrameProcessor;
import com.feyiuremote.ui.camera.models.CameraViewModel;

public class CameraFocusClickListener implements View.OnClickListener {
    private final String TAG = CameraFocusClickListener.class.getSimpleName();
    private CameraViewModel cameraViewModel;

    public CameraFocusClickListener(CameraViewModel viewModel) {
        this.cameraViewModel = viewModel;
    }

    private void focusOnPOI(POI poi) {
        cameraViewModel.camera.getValue().controls.touchFocus(poi.rect.centerXPercInFrame(), poi.rect.centerYPercInFrame(),
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
            POI poi = null;
            CameraLiveFeedReceiver clfr = cameraViewModel.liveFeedReceiver.getValue();

            if (clfr != null) {
                FrameProcessor fp = clfr.frameProcessorDispatcher.getCurrentProcessor();

                if (fp != null) {
                    boolean processorProvidesPOI = clfr.frameProcessorDispatcher.getCurrentProcessor().providesPOI();

                    if (processorProvidesPOI) {
                        poi = fp.getPOI();
                    }
                }
            }

            if (poi != null) {
                focusOnPOI(poi);
            } else {
                autoFocusMf();
            }
        } else {
            cameraViewModel.status.setValue("Camera is not available, can't focus!");
        }
    }
}