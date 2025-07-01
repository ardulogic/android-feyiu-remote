package com.feyiuremote.libs.Feiyu.processors;

import android.content.ContentValues;
import android.util.Log;

import com.feyiuremote.libs.AI.trackers.POI;
import com.feyiuremote.libs.Feiyu.calibration.CalibrationDB;
import com.feyiuremote.libs.Feiyu.queue.FeyiuControls;
import com.feyiuremote.ui.camera.models.CameraViewModel;
public class GimbalFollowProcessor implements IGimbalProcessor {

    private final String TAG = GimbalFollowProcessor.class.getSimpleName();
    private final CalibrationDB mDbCal;
    private final CameraViewModel mCameraViewModel;

    private final int MIN_POI_UPDATE_INTERVAL = 150;

    private long timePoiUpdated = 0L;

    public GimbalFollowProcessor(CameraViewModel cameraViewModel) {
        this.mDbCal = CalibrationDB.get();
        this.mCameraViewModel = cameraViewModel;
    }

    public double getSpeed(double error) {
        if (Math.abs(error) < 0.3) {
            return error * 20;
        } else {
            return error * 50;
        }
    }

    private void moveTowards(POI poi) {
        Log.d(TAG, "Moving towards POI");

        double desiredPanOffset = mCameraViewModel.panOffset.getValue();  // e.g. 0.33
        double desiredTiltOffset = mCameraViewModel.tiltOffset.getValue();  // e.g. 0.33

        double dev_x = poi.rect.xDevPercFromFrameCenter();  // +ve → right
        double dev_y = poi.rect.yDevPercFromFrameCenter();  // +ve → down

        // <-- Only these two lines changed (sign fix) ------------------------
        double err_x = dev_x - desiredPanOffset;
        double err_y = dev_y - desiredTiltOffset;
        // --------------------------------------------------------------------

        // calculate angle speed based on the error.  It should be ~ 1
        double spd_x = getSpeed(err_x);
        double spd_y = getSpeed(err_y);

        Log.d(TAG, String.format("Err: %.2f SpdX: %.2f, Erry: %.2f SpdY: %.2f", err_x, spd_x, err_y, spd_y));

        ContentValues panSettings = mDbCal.getClosestToSpeed(CalibrationDB.AXIS_PAN, spd_x);
        ContentValues tiltSettings = mDbCal.getClosestToSpeed(CalibrationDB.AXIS_TILT, spd_y);

        if (panSettings == null || tiltSettings == null) {
            Log.e(TAG, "Please calibrate first!");
            stop();
            return;
        }

        // Dead-zone (±5 %)
        if (Math.abs(err_x) < 0.05) panSettings.put("joy_val", 0);
        if (Math.abs(err_y) < 0.05) tiltSettings.put("joy_val", 0);

        FeyiuControls.setPanSensitivity(panSettings.getAsInteger("joy_sens"));
        FeyiuControls.setPanJoy(panSettings.getAsInteger("joy_val"));
        FeyiuControls.setTiltSensitivity(tiltSettings.getAsInteger("joy_sens"));
        FeyiuControls.setTiltJoy(tiltSettings.getAsInteger("joy_val"));
    }


    @Override
    public void onPoiUpdate(POI poi) {
        try {
            if (System.currentTimeMillis() - timePoiUpdated > MIN_POI_UPDATE_INTERVAL) {
                moveTowards(poi);
                timePoiUpdated = System.currentTimeMillis();
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Poi no longer exists");
            stop();
        }
    }


    @Override
    public void stop() {
        FeyiuControls.setTiltJoy(0);
        FeyiuControls.setPanJoy(0);
    }
}
