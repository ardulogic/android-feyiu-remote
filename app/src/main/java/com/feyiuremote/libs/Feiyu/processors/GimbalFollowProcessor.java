package com.feyiuremote.libs.Feiyu.processors;

import android.content.ContentValues;
import android.util.Log;

import com.feyiuremote.libs.AI.trackers.POI;
import com.feyiuremote.libs.Feiyu.Axes;
import com.feyiuremote.libs.Feiyu.calibration.CalibrationDB;
import com.feyiuremote.libs.Feiyu.queue.FeyiuControls;

public class GimbalFollowProcessor implements IGimbalProcessor {

    private final String TAG = GimbalFollowProcessor.class.getSimpleName();
    private final CalibrationDB mDbCal;

    private final int joy_sens = 60;

    private final int MIN_POI_UPDATE_INTERVAL = 150;

    private long timePoiUpdated = 0L;

    public GimbalFollowProcessor() {
        this.mDbCal = CalibrationDB.get();
    }

    public static double evaluatePolynomial(double x) {
        double result = 0.0;
        result += -185.2251 * Math.pow(x, 5);
        result += 453.9713 * Math.pow(x, 4);
        result += -369.9565 * Math.pow(x, 3);
        result += 105.8335 * Math.pow(x, 2);
        result += 4.8634 * x;
        result += 0.5173;

        return result;
    }


    private void moveTowards(POI poi) {
        Log.d(TAG, "MOving towards POI");

        FeyiuControls.setSensitivity(Axes.Axis.PAN, 25);
        FeyiuControls.setSensitivity(Axes.Axis.TILT, 25);

        double dev_x = poi.rect.xDevPercFromFrameCenter();
        double dev_y = poi.rect.yDevPercFromFrameCenter();

        double spd_x = evaluatePolynomial(Math.abs(dev_x)) * (dev_x > 0 ? 1 : -1);
        double spd_y = evaluatePolynomial(Math.abs(dev_y)) * (dev_y > 0 ? 1 : -1);

        ContentValues panSettings = mDbCal.getClosestToSpeed(CalibrationDB.AXIS_PAN, spd_x);
        ContentValues tiltSettings = mDbCal.getClosestToSpeed(CalibrationDB.AXIS_TILT, spd_y);

        if (panSettings == null || tiltSettings == null) {
            Log.e(TAG, "Please calibrate first!");
            stop();
            return;
        }

//        if (FeyiuState.angleIsCritical()) {
//            Log.e(TAG, "Angle is critical!");
//            stop();
//            return;
//        }

        if (Math.abs(dev_x) < 0.05) {
            panSettings.put("joy_val", 0);
        }

        if (Math.abs(dev_y) < 0.05) {
            tiltSettings.put("joy_val", 0);
        }

        FeyiuControls.setPanJoy(panSettings.getAsInteger("joy_val"));
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
