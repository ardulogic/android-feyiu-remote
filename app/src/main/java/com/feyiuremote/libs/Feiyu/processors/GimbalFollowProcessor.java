package com.feyiuremote.libs.Feiyu.processors;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.feyiuremote.libs.AI.trackers.POI;
import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.FeyiuControls;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.calibration.CalibrationDbHelper;
import com.feyiuremote.libs.Feiyu.calibration.CalibrationPresetDbHelper;

import java.util.ArrayList;

public class GimbalFollowProcessor implements IGimbalProcessor {

    private final CalibrationPresetDbHelper mDbPreset;
    private final CalibrationDbHelper mDbCal;
    private double ldev_y;
    private double ldev_x;

    private int joy_sens = 60;

    private ArrayList<Float[]> mTrackCurves = new ArrayList<Float[]>() {{
        add(new Float[]{(float) 0.00193939, (float) 0.424242, (float) 1.81818});
        add(new Float[]{(float) 0.00674982, (float) 0.306131, (float) 0.932636});
    }};

    private final String TAG = GimbalFollowProcessor.class.getSimpleName();
    private int curve;

    /**
     * Target destination for POI might be anywhere in the frame
     */
    private Double poi_target_y_perc;
    private Double poi_target_x_perc;

    public GimbalFollowProcessor(BluetoothLeService mBluetoothLeService) {
        this.mDbPreset = new CalibrationPresetDbHelper(mBluetoothLeService.getApplicationContext());
        this.mDbCal = new CalibrationDbHelper(mBluetoothLeService.getApplicationContext());

        this.ldev_x = 0;
        this.ldev_y = 0;
    }

    /**
     * Where the X has to land of POI
     *
     * @param w int
     * @return x
     */
    private int getPoiDestinationX(int w) {
        if (this.poi_target_x_perc != null) {
            return (int) (w * this.poi_target_x_perc);
        } else {
            return w / 2;
        }
    }

    /**
     * Where the Y has to land of POI
     *
     * @param h int
     * @return y
     */
    private int getPoiDestinationY(int h) {
        if (this.poi_target_y_perc != null) {
            return (int) (h * this.poi_target_y_perc);
        } else {
            return h / 2;
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


    private void moveTowards(POI poi) {
        double dev_x = poi.xCenterDevPercFromPoint(getPoiDestinationX(poi.frame_width), true);
        double dev_y = poi.yCenterDevPercFromPoint(getPoiDestinationY(poi.frame_height), false);

//        double ddev_x = Math.abs(dev_x - ldev_x);
//        double ddev_y = Math.abs(dev_y - ldev_y);

//        float spd_x = calcSpeed(dev_x, curve) + calcSpeed(ddev_x, curve) * (ddev_x > 0 ? 1 : -1);
//        float spd_y = calcSpeed(dev_y, curve) + calcSpeed(ddev_y, curve) * (ddev_y > 0 ? 1 : -1);
        double spd_x = evaluatePolynomial(Math.abs(dev_x)) * (dev_x > 0 ? 1 : -1);
        double spd_y = evaluatePolynomial(Math.abs(dev_y)) * (dev_y > 0 ? 1 : -1);
//        Log.d(TAG, "Predicted speed for " + dev_x + " devx is " + spd_x);
//        Log.d(TAG, "Predicted speed for " + dev_y + " devy is " + spd_y);


//        Log.d("Deviation:",
//                "Devx: " + String.format("%.3f", dev_x) +
//                        " Devy: " + String.format("%.3f", dev_y) +
////                        " ddevx: " + String.format("%.3f", ddev_x) +
////                        " ddevy: " + String.format("%.3f", ddev_y) +
//                        " spdx: " + String.format("%.3f", spd_x) +
//                        " spdy: " + String.format("%.3f", spd_y)
//        );


        ContentValues panSettings = mDbCal.getByClosestSpeed(mDbCal.AXIS_PAN, joy_sens, spd_x);
        ContentValues tiltSettings = mDbCal.getByClosestSpeed(mDbCal.AXIS_TILT, joy_sens, spd_y);

        if (panSettings == null || tiltSettings == null) {
            Log.e(TAG, "Please calibrate first!");
            cancel();
            return;
        }

        if (FeyiuState.angleIsCritical()) {
            Log.e(TAG, "Angle is critical!");
            cancel();
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


    @Override
    public void onPoiLock() {
        FeyiuControls.setTiltSensitivity(joy_sens);
        FeyiuControls.setPanSensitivity(joy_sens);
    }

    @Override
    public void onPoiUpdate(POI poi) {
        try {
            moveTowards(poi);
        } catch (NullPointerException e) {
            Log.e(TAG, "Poi no longer exists");
            cancel();
        }
    }

    @Override
    public void updatePoiDestination(double x_perc, double y_perc) {
        this.poi_target_x_perc = x_perc;
        this.poi_target_y_perc = y_perc;
    }

    public Bitmap drawPoiDestination(Bitmap bitmap) {
        if (poi_target_x_perc != null && poi_target_y_perc != null) {
            Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

            // Get canvas from the mutable bitmap
            Canvas canvas = new Canvas(mutableBitmap);

            // Calculate the actual coordinates based on percentages
            int x = (int) (bitmap.getWidth() * poi_target_x_perc);
            int y = (int) (bitmap.getHeight() * poi_target_y_perc);

            // Set up paint for drawing the "X"
            Paint paint = new Paint();
            paint.setColor(Color.RED); // You can set the color of the "X"
            paint.setStrokeWidth(5);

            // Draw the "X"
            canvas.drawLine(x - 20, y - 20, x + 20, y + 20, paint);
            canvas.drawLine(x + 20, y - 20, x - 20, y + 20, paint);

            return mutableBitmap;
        }

        return bitmap;
    }

    @Override
    public void cancel() {
        FeyiuControls.setTiltJoy(0);
        FeyiuControls.setPanJoy(0);
    }
}
