package com.feyiuremote.libs.Feiyu.processors;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.util.Log;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.FeyiuControls;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.calibration.CalibrationDbHelper;
import com.feyiuremote.libs.Feiyu.calibration.CalibrationPresetDbHelper;
import com.feyiuremote.libs.LiveStream.interfaces.IPoiUpdateListener;

import org.opencv.core.Rect;

import java.util.ArrayList;

public class GimbalFollowProcessor implements IPoiUpdateListener {

    private final CalibrationPresetDbHelper mDbPreset;
    private final CalibrationDbHelper mDbCal;
    private double ldev_y;
    private double ldev_x;

    private int joy_sens = 25;

    private ArrayList<Float[]> mTrackCurves = new ArrayList<Float[]>() {{
        add(new Float[]{(float) 0.00193939, (float) 0.424242, (float) 1.81818});
        add(new Float[]{(float) 0.00674982, (float) 0.306131, (float) 0.932636});
    }};

    private final String TAG = GimbalFollowProcessor.class.getSimpleName();
    private int curve;
    private Double poi_target_x_perc;
    private Double poi_target_y_perc;

    public GimbalFollowProcessor(BluetoothLeService mBluetoothLeService) {
        this.mDbPreset = new CalibrationPresetDbHelper(mBluetoothLeService.getApplicationContext());
        this.mDbCal = new CalibrationDbHelper(mBluetoothLeService.getApplicationContext());

        this.ldev_x = 0;
        this.ldev_y = 0;
    }

    public void setTrackingCurveParams(int index, Float[] params) {
        this.mTrackCurves.set(index, params);
    }

    public void setTrackingCurve(int index) {
        this.curve = index;
    }

    @Override
    public void onPoiLock(Rect poi) {
        // TODO: Sensitivity is same, idk if its best
        FeyiuControls.setTiltSensitivity(joy_sens);
        FeyiuControls.setPanSensitivity(joy_sens);
    }

    @Override
    public void onPoiUpdate(Bitmap bitmap, org.opencv.core.Rect poi) {
        track(bitmap, poi, this.curve);
    }

    @Override
    public void onPoiCancel() {
        FeyiuControls.setTiltJoy(0);
        FeyiuControls.setPanJoy(0);
    }

    @Override
    public void onPoiTargetPositionUpdate(double x_perc, double y_perc) {
        this.poi_target_x_perc = x_perc;
        this.poi_target_y_perc = y_perc;
    }

    private float mapDeviationToSpeed(int dev, int width) {
        boolean isNegative = dev < 0;

        double minValue = 0.001;  // Minimum value of the range
        double maxValue = 0.05;   // Maximum value of the range

        float mapped = (float) (minValue + ((float) (Math.abs(dev) / width) * (maxValue - minValue)));

        if (isNegative) {
            mapped *= -1;
        }

        return mapped;
    }

    private int getPoiTargetCenterX(Bitmap bitmap) {
        if (this.poi_target_x_perc != null) {
            return (int) (bitmap.getWidth() * this.poi_target_x_perc);
        } else {
            return bitmap.getWidth() / 2;
        }
    }

    private int getPoiTargetCenterY(Bitmap bitmap) {
        if (this.poi_target_y_perc != null) {
            return (int) (bitmap.getHeight() * this.poi_target_y_perc);
        } else {
            return bitmap.getHeight() / 2;
        }
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


    private void track(Bitmap bitmap, org.opencv.core.Rect poi, int curve) {
        double dev_x = calcDeviation(poi.x, poi.width, getPoiTargetCenterX(bitmap), bitmap.getWidth(), true);
        double dev_y = calcDeviation(poi.y, poi.height, getPoiTargetCenterY(bitmap), bitmap.getHeight(), false);

        double ddev_x = Math.abs(dev_x - ldev_x);
        double ddev_y = Math.abs(dev_y - ldev_y);

//        float spd_x = calcSpeed(dev_x, curve) + calcSpeed(ddev_x, curve) * (ddev_x > 0 ? 1 : -1);
//        float spd_y = calcSpeed(dev_y, curve) + calcSpeed(ddev_y, curve) * (ddev_y > 0 ? 1 : -1);
        double spd_x = evaluatePolynomial(Math.abs(dev_x)) * (dev_x > 0 ? 1 : -1);
        double spd_y = evaluatePolynomial(Math.abs(dev_y)) * (dev_y > 0 ? 1 : -1);
//        Log.d(TAG, "Predicted speed for " + dev_x + " devx is " + spd_x);
//        Log.d(TAG, "Predicted speed for " + dev_y + " devy is " + spd_y);


//        Log.d("Deviation:",
//                "Devx: " + String.format("%.3f", dev_x) +
//                        " Devy: " + String.format("%.3f", dev_y) +
//                        " ddevx: " + String.format("%.3f", ddev_x) +
//                        " ddevy: " + String.format("%.3f", ddev_y) +
//                        " spdx: " + String.format("%.3f", spd_x) +
//                        " spdy: " + String.format("%.3f", spd_y)
//        );


        ContentValues panSettings = mDbCal.getByClosestSpeed(mDbCal.AXIS_PAN, joy_sens, spd_x);
        ContentValues tiltSettings = mDbCal.getByClosestSpeed(mDbCal.AXIS_TILT, joy_sens, spd_y);

        if (panSettings == null || tiltSettings == null || FeyiuState.angleIsCritical()) {
            Log.e(TAG, "Please calibrate first!");
            return;
        }

        if (Math.abs(dev_x) < 0.05) {
            panSettings.put("joy_val", 0);
        }

        if (Math.abs(dev_y) < 0.05) {
            tiltSettings.put("joy_val", 0);
        }

        FeyiuControls.setPanJoy(panSettings.getAsInteger("joy_val"));
        FeyiuControls.setTiltJoy(tiltSettings.getAsInteger("joy_val"));

        ldev_x = dev_x;
        ldev_y = dev_y;
    }

    private float calcSpeed(int dev, int curve_index) {
        return (float) (mTrackCurves.get(curve_index)[0] * dev * dev
                + mTrackCurves.get(curve_index)[1] * Math.abs(dev)
                + mTrackCurves.get(curve_index)[2]);
    }

    private float calcDeviation(int point, int point_width, int target_point, int axis_width, boolean invert_sign) {
        float pointCenter = point + point_width / 2;
        float relCenter = target_point;

        int dev = (int) Math.abs(pointCenter - relCenter);
        float perc_dev = (float) dev / axis_width;

        if (pointCenter < relCenter) {
            return invert_sign ? -perc_dev : perc_dev;
        } else {
            return invert_sign ? perc_dev : -perc_dev;
        }
    }

}
