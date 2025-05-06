package com.feyiuremote.libs.Feiyu.processors.position;

import android.util.Log;

import com.feyiuremote.libs.Feiyu.Axes;
import com.feyiuremote.libs.Feiyu.calibration.CalibrationDB;

public class AxesCalibration {
    private static final String TAG = AxesCalibration.class.getSimpleName();
    private final CalibrationDB mDb;
    private final double max_pan_speed;
    private final double max_tilt_speed;

    public AxisCalibration pan;
    public AxisCalibration tilt;

    public AxesCalibration(double pan_angle, double tilt_angle, double max_pan_speed, double max_tilt_speed) {
        this.mDb = CalibrationDB.get();

        this.max_pan_speed = max_pan_speed + 1;
        this.max_tilt_speed = max_tilt_speed + 1;

        this.pan = new AxisCalibration(mDb, AxisCalibration.AXIS_PAN, pan_angle);
        this.tilt = new AxisCalibration(mDb, AxisCalibration.AXIS_TILT, tilt_angle);
    }

    public AxisCalibration getAxis(Axes.Axis axis) {
        if (axis == Axes.Axis.PAN) {
            return this.pan;
        }

        if (axis == Axes.Axis.TILT) {
            return this.tilt;
        }

        return null;
    }


    public void calculate() {
        Log.d(TAG, "Calculating axes calibration...");

        // compute how long each axis would take at max speed
        double minPanningTime = Math.abs(pan.getInitAngleDiff() / max_pan_speed);
        double minTiltingTime = Math.abs(tilt.getInitAngleDiff() / max_tilt_speed);

        if (minPanningTime > minTiltingTime) {
            // Pan is the longer move → give pan its fastest calibration...
            pan.assign(pan.getFastestForStartingAngleDiff(max_pan_speed));

            tilt.matchCalibrationTo(pan, max_tilt_speed);
        } else {
            tilt.assign(tilt.getFastestForStartingAngleDiff(max_tilt_speed));

            pan.matchCalibrationTo(tilt, max_pan_speed);
        }

    }


}
