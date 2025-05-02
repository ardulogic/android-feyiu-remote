package com.feyiuremote.libs.Feiyu.processors.position;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.calibration.CalibrationDB;

import java.text.DecimalFormat;
import java.util.Objects;

public class GimbalPositionTarget {
    private final String TAG = GimbalPositionTarget.class.getSimpleName();

    private final double pan_angle;
    private final double tilt_angle;
    private final CalibrationDB calDB;
    private final Double focus;
    private final double pan_diff_at_start;
    private final double tilt_diff_at_start;

    public final int dwell_time_ms;
    public final AxesCalibration axes;

    private boolean pan_stopping = false;
    private boolean tilt_stopping = false;

    private boolean panHasReached = false;
    private boolean tilthasReached = false;


    public GimbalPositionTarget(Context context, double pan_angle, double tilt_angle, double max_pan_speed, double max_tilt_speed, int dwell_time_ms, Double focus) {
        this.calDB = new CalibrationDB(context);

        this.pan_angle = pan_angle;
        this.pan_diff_at_start = angleDiffInDeg(calDB.AXIS_PAN);

        this.tilt_angle = tilt_angle;
        this.tilt_diff_at_start = angleDiffInDeg(calDB.AXIS_TILT);

        this.axes = new AxesCalibration(context, pan_angle, tilt_angle, max_pan_speed, max_tilt_speed);
        this.axes.calculate();

        this.focus = focus;
        this.dwell_time_ms = dwell_time_ms;

        Log.d(TAG, "Ready");
    }

    public Double getFocus() {
        if (focus != null) {
            Log.d(TAG, "Focus value:" + focus);
        }

        return this.focus;
    }

    public double angleDiffInDeg(String axis) {
        double target, current;

        if (Objects.equals(axis, CalibrationDB.AXIS_PAN)) {
            target = getPanTargetAngle();
            current = FeyiuState.getInstance().angle_pan.value();
        } else {
            target = getTiltTargetAngle();
            current = FeyiuState.getInstance().angle_tilt.value();
        }

        // If rotation is 360 and target 0, it will stay put
        boolean use_shortcut = true;
        if (use_shortcut) {
            // Calculate the raw difference
            double diff = target - current;

            // Normalize the difference to the range [-180, 180] degrees
            diff = (diff + 180) % 360 - 180;

            if (diff < -180) {
                diff += 360;
            }

            return diff;
        } else {
            return target - current;
        }
    }

    public double getPanTargetAngle() {
        return this.pan_angle;
    }

    public double getTiltTargetAngle() {
        return this.tilt_angle;
    }

    public boolean isReached() {
        return panHasReached && tilthasReached;
    }

    public boolean isPositionReached() {
        return Math.abs(angleDiffInDeg(CalibrationDB.AXIS_PAN)) < 2
                && Math.abs(angleDiffInDeg(CalibrationDB.AXIS_TILT)) < 2;
    }

    public boolean shouldAnticipateStop(double movement_time) {
        double longestUpdateInterval = FeyiuState.getInstance().getAverageUpdateIntervalMs() * 2;
        boolean isShorterThanLongestUpdateInterval = Math.abs(movement_time) < longestUpdateInterval;

        boolean answer = movement_time < 0 || isShorterThanLongestUpdateInterval;

        if (answer) {
            Log.d(TAG, "shouldAnticipateStop: Should stop in: " + movement_time);
        }

        return answer;
    }

    public boolean panIsStopping() {
        return this.pan_stopping;
    }

    public boolean tiltIsStopping() {
        return this.tilt_stopping;
    }

    public void setPanIsStopping() {
        this.pan_stopping = true;
    }

    public void setTiltIsStopping() {
        this.tilt_stopping = true;
    }


    public boolean isPanReached() {
        return panHasReached;
    }

    public boolean isTiltReached() {
        return tilthasReached;
    }

    public boolean panShouldStop() {
        boolean stoppingPointReached = shouldAnticipateStop(axes.pan.getInterpMovementTimeMs());
//        boolean accurateEnough = Math.abs(axes.pan.getCurrentAngleDiff()) < 1;

        return !panIsStopping() && (stoppingPointReached);
    }

    public boolean tiltShouldStop() {
        boolean stoppingPointReached = shouldAnticipateStop(axes.tilt.getInterpMovementTimeMs());
//        boolean accurateEnough = Math.abs(axes.tilt.getCurrentAngleDiff()) < 1;

        return !tiltIsStopping() && (stoppingPointReached);
    }

    public void setPanHasReached() {
        this.panHasReached = true;
    }

    public void setTiltHasReached() {
        this.tilthasReached = true;
    }

    @NonNull
    public String toString() {
        DecimalFormat decimalFormat = new DecimalFormat(" #.#; -#.#");
        String d = String.format("%s, %s, %s",
                FeyiuState.getInstance().angle_pan.speedToString(),
                FeyiuState.getInstance().angle_tilt.speedToString(),
                FeyiuState.getInstance().angle_yaw.speedToString());


        d += "\nTime: P:" + Math.round(axes.pan.getMovementTimeInMs()) + " T:" + Math.round(axes.pan.getMovementTimeInMs());
        d += "\n Stopping: P:" + panIsStopping() + " T:" + tiltIsStopping();
        d += "\n Stopping: P:" + panIsStopping() + " T:" + tiltIsStopping();
        d += "\n Angle Df: P:" + decimalFormat.format(angleDiffInDeg(calDB.AXIS_PAN)) + " T:" + decimalFormat.format(angleDiffInDeg(calDB.AXIS_TILT));
        d += "\n Angle To: P:" + decimalFormat.format(getPanTargetAngle()) + " T:" + decimalFormat.format(getTiltTargetAngle());
        d += "\n Speed: P:" + decimalFormat.format(axes.pan.getSpeedDegPerSec()) + " T:" + decimalFormat.format(axes.tilt.getSpeedDegPerSec());
        d += "\n Target Joy: P:" + axes.pan.getJoyVal() + " T:" + axes.tilt.getJoyVal();
        d += "\n Actual Joy: P:" + FeyiuState.joy_val_pan + " T:" + FeyiuState.joy_val_tilt;
        d += "\n Target Sens: P:" + axes.pan.getJoySens() + " T:" + axes.tilt.getJoySens();
        d += "\n Actual Sens: P:" + FeyiuState.joy_sens_pan + " T:" + FeyiuState.joy_sens_tilt;

        return d;
    }

    public void logDiffs() {
//        Log.d(TAG, String.format("Pan-df: %.2f %.2f | Tilt-df: %.2f %.2f | Pan-reached: %b Tilt-reached: %b",
//                angleDiffInDeg(mDb.AXIS_PAN), getPanMovementTime(), angleDiffInDeg(mDb.AXIS_TILT), getTiltMovementTime(), isPanReached(), isTiltReached()));
    }

    public double getPanMovementTime() {
        return axes.pan.getInterpMovementTimeMs();
    }

    public double getTiltMovementTime() {
        return axes.tilt.getInterpMovementTimeMs();
    }

    public double getPanSpeedDegPerSec() {
        return axes.pan.getSpeedDegPerSec();
    }

    public double getTiltSpeedDegPerSec() {
        return axes.tilt.getSpeedDegPerSec();
    }

    public int getPanJoyValue() {
        return axes.pan.getJoyVal();
    }

    public int getTiltJoyValue() {
        return axes.tilt.getJoyVal();
    }

    public int getPanSensitivity() {
        return axes.pan.getJoySens();
    }

    public int getTiltSensitivity() {
        return axes.tilt.getJoySens();
    }
}
