package com.feyiuremote.ui.camera.listeners;

import android.view.View;

import com.feyiuremote.libs.Cameras.Panasonic.PanasonicCamera;
import com.feyiuremote.ui.camera.models.CameraViewModel;

public class CameraControlClickListener implements View.OnClickListener {
    private final String TAG = CameraControlClickListener.class.getSimpleName();

    private final CameraViewModel cameraModel;
    private final CameraAction action;

    public CameraControlClickListener(CameraViewModel cameraModel, CameraAction action) {
        this.cameraModel = cameraModel;
        this.action = action;
    }

    @Override
    public void onClick(View view) {
        if (this.cameraModel.camera.getValue() != null) {
            this.action.performAction(this.cameraModel.camera.getValue());
        } else {
            this.cameraModel.status.setValue("Camera is not available!");
        }
    }

    @FunctionalInterface
    public interface CameraAction {
        void performAction(PanasonicCamera camera);
    }

}