package com.feyiuremote.ui.camera.listeners;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.View;

import com.feyiuremote.libs.Cameras.Panasonic.IPanasonicCameraFocusControlListener;
import com.feyiuremote.libs.Cameras.Panasonic.PanasonicCamera;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.ui.camera.CameraViewModel;
import com.feyiuremote.ui.camera.waypoints.Waypoint;

public class WaypointAddClickListener implements View.OnClickListener {
    private CameraViewModel cameraViewModel;
    private Activity activity;

    public WaypointAddClickListener(CameraViewModel cameraViewModel, Activity activity) {
        this.cameraViewModel = cameraViewModel;
        this.activity = activity;
    }

    @Override
    public void onClick(View view) {
        Bitmap lastImage = cameraViewModel.getLastImage(activity);

        Waypoint w = new Waypoint(
                lastImage, FeyiuState.getInstance().angle_pan.value(),
                FeyiuState.getInstance().angle_tilt.value(),
                50, 10000, null);

        if (canAcquireFocus()) {
            cameraViewModel.camera.getValue().focus.update(new IPanasonicCameraFocusControlListener() {
                @Override
                public void onSuccess(double position) {
                    w.setFocus(position);
                    cameraViewModel.addWaypoint(w, false);
                }

                @Override
                public void onFailure() {
                    cameraViewModel.status.postValue("Failed to acquire focus!");
                    cameraViewModel.addWaypoint(w, false);
                }
            });
        } else {
            cameraViewModel.addWaypoint(w, false);
        }
    }

    private boolean canAcquireFocus() {
        PanasonicCamera camera = cameraViewModel.camera.getValue();

        return camera != null && camera.focusIsAvailable();
    }
}