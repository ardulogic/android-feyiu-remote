package com.feyiuremote.libs.Feiyu.queue.debug;

import android.content.ContentValues;
import android.util.Log;

import com.feyiuremote.libs.Feiyu.Axes;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.calibration.CalibrationDB;
import com.feyiuremote.libs.Feiyu.processors.position.GimbalPositionTarget;

public class DebuggableAxis {
    private static final String TAG = DebuggableAxis.class.getSimpleName();
    private final Axes.Axis axis;

    private double onStoppingMovementTime;
    private double speed;
    private double onStoppingTimestamp;

    private double onStoppedTimestamp;

    private double onStopCommandSent;
    private double onStoppingAngleDiff;
    private double onStoppedAngleDiff;

    private ContentValues calibration;
    private Long onStoppingTimeSinceGimbalUpdate;
    private Double onStoppingTargetAngle;

    public boolean autoCalibrationNudging = true;

    public DebuggableAxis(Axes.Axis axis) {
        this.axis = axis;
    }

    public void onStopping(GimbalPositionTarget target) {
        onStoppingTimestamp = System.currentTimeMillis();
        onStoppingMovementTime = target.getMovementTime(axis);
        onStoppingAngleDiff = target.axes.getAxis(axis).getCurrentAngleDiff();
        onStoppingTimeSinceGimbalUpdate = FeyiuState.getInstance().getTimeSinceLastUpdateMs();
        onStoppingTargetAngle = target.axes.getAxis(axis).getTargetAngle();

        speed = target.axes.getAxis(axis).getSpeedDegPerSec();
        calibration = target.axes.getAxis(axis).getCal();

        if (onStoppingMovementTime < 40) {
            Log.e(TAG, "On Stopping: " + Axes.toString(axis) + " movement time is less than minimum to stop smoothly! (" + target.getMovementTime(axis) + "ms)");
        }
    }

    private Double getCalibrationOvershoot() {
        return calibration.getAsDouble(getCalibrationOvershootColumn());
    }

    private String getCalibrationOvershootColumn() {
        return Axes.toString(axis).toLowerCase() + "_angle_overshoot";
    }

    public void onStopCommandSent() {
        onStopCommandSent = System.currentTimeMillis();
    }

    public void onStopped(GimbalPositionTarget target) {
        onStoppedTimestamp = System.currentTimeMillis();
        onStoppedAngleDiff = target.axes.getAxis(axis).getCurrentAngleDiff();

        if (autoCalibrationNudging) { // Correct overshoot values
            nudgeCalibration();
        }

        log();
    }

    public void nudgeCalibration() {
        double extraAngle = (double) onStoppingTimeSinceGimbalUpdate / 1000 * speed;

        double overshootCalibrated = getCalibrationOvershoot();
        double overshootCalculated = onStoppingAngleDiff - extraAngle - speed * onStoppingMovementTime / 1000;
        double finalAngleError = onStoppedAngleDiff;

        Log.d(TAG, Axes.toString(axis) + " overshoot analysis: Calibrated " + overshootCalibrated + ", Calculated " + overshootCalculated + " Real angle error: " + finalAngleError);

        if (Math.abs(finalAngleError) > 0.8 && Math.abs(finalAngleError) < 10) {
            // Update calibrated values:
            double newOvershoot = overshootCalibrated - finalAngleError / 2;

            calibration.put(getCalibrationOvershootColumn(), newOvershoot);
            CalibrationDB.get().updateOrCreate(calibration);

            Log.w(TAG, String.format(
                    "Will update " + Axes.toString(axis) + " calibraiton: Sensitivity %d JoyVal %d Speed %.2f Overshoot %.2f -> %.2f",
                    calibration.getAsInteger("joy_sens"),
                    calibration.getAsInteger("joy_val"),
                    speed,
                    calibration.getAsDouble(getCalibrationOvershootColumn()),
                    newOvershoot
            ));
        }
    }

    public void log() {
        double stopCommandTimingError = onStoppingMovementTime - onStopCommandSent;
        double systemTimingErrorAngle = speed * (stopCommandTimingError / 1000.0);

        Log.d(TAG, Axes.toString(axis) + " axis systemic stop timing error (" + stopCommandTimingError + ") equivalent to: " + systemTimingErrorAngle + " deg at " + speed + " deg/s");
    }
}
