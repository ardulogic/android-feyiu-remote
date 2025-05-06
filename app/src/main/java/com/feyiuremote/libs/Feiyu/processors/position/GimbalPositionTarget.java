package com.feyiuremote.libs.Feiyu.processors.position;

import android.util.Log;

import androidx.annotation.NonNull;

import com.feyiuremote.libs.Feiyu.Axes;
import com.feyiuremote.libs.Feiyu.FeyiuState;

import java.text.DecimalFormat;

public class GimbalPositionTarget {
    private final String TAG = GimbalPositionTarget.class.getSimpleName();

    public final AxesCalibration axes;
    private final Double focus;
    public final int dwell_time_ms;

    private boolean panIsStopping = false;
    private boolean tiltIsStopping = false;

    private boolean panHasReached = false;
    private boolean tilthasReached = false;


    public GimbalPositionTarget(double pan_angle, double tilt_angle, double max_pan_speed, double max_tilt_speed, int dwell_time_ms, Double focus) {
        this.axes = new AxesCalibration(pan_angle, tilt_angle, max_pan_speed, max_tilt_speed);
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

    public boolean isReached() {
        return panHasReached && tilthasReached;
    }

    public boolean shouldStartStopping(Axes.Axis axis) {
        double movementTime = getMovementTime(axis);

        if (movementTime > 0) {
            boolean stoppingPointReached = shouldAnticipateStop(movementTime);
            boolean isStopping = isStoppingOn(axis);
            // boolean accurateEnough = Math.abs(axes.pan.getCurrentAngleDiff()) < 1;

            return !isStopping && stoppingPointReached;
        }

        return true;  // Movement time is negative, should definitely stop!
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

    public boolean isPositionReached() {
        return Math.abs(axes.pan.getCurrentAngleDiff()) < 1
                && Math.abs(axes.tilt.getCurrentAngleDiff()) < 1;
    }

    public void setHasReachedOn(Axes.Axis axis) {
        if (axis == Axes.Axis.PAN) {
            this.panHasReached = true;
        } else {
            this.tilthasReached = true;
        }
    }

    public boolean isReachedOn(Axes.Axis axis) {
        if (axis == Axes.Axis.PAN) {
            return panHasReached;
        } else {
            return tilthasReached;
        }
    }

    public boolean isStoppingOn(Axes.Axis axis) {
        if (axis == Axes.Axis.PAN) {
            return panIsStopping;
        } else {
            return tiltIsStopping;
        }
    }

    public void setIsStopping(Axes.Axis axis) {
        if (axis == Axes.Axis.PAN) {
            panIsStopping = true;
        } else {
            tiltIsStopping = true;
        }
    }

    @NonNull
    public String toString() {
        DecimalFormat decimalFormat = new DecimalFormat(" #.#; -#.#");
        String d = String.format("%s, %s, %s",
                FeyiuState.getInstance().angle_pan.speedToString(),
                FeyiuState.getInstance().angle_tilt.speedToString(),
                FeyiuState.getInstance().angle_yaw.speedToString());


        d += "\nTime: P:" + Math.round(axes.pan.getMovementTimeInMs()) + " T:" + Math.round(axes.pan.getMovementTimeInMs());
        d += "\n Stopping: P:" + isStoppingOn(Axes.Axis.PAN) + " T:" + isStoppingOn(Axes.Axis.TILT);
        d += "\n Angle Df: P:" + decimalFormat.format(axes.pan.getCurrentAngleDiff()) + " T:" + decimalFormat.format(axes.tilt.getCurrentAngleDiff());
        d += "\n Angle To: P:" + decimalFormat.format(axes.pan.getTargetAngle()) + " T:" + decimalFormat.format(axes.tilt.getTargetAngle());
        d += "\n Speed: P:" + decimalFormat.format(axes.pan.getSpeedDegPerSec()) + " T:" + decimalFormat.format(axes.tilt.getSpeedDegPerSec());
        d += "\n Target Joy: P:" + axes.pan.getJoyVal() + " T:" + axes.tilt.getJoyVal();
        d += "\n Actual Joy: P:" + FeyiuState.joy_val_pan + " T:" + FeyiuState.joy_val_tilt;
        d += "\n Target Sens: P:" + axes.pan.getJoySens() + " T:" + axes.tilt.getJoySens();
        d += "\n Actual Sens: P:" + FeyiuState.joy_sens_pan + " T:" + FeyiuState.joy_sens_tilt;

        return d;
    }

    public double getMovementTime(Axes.Axis axis) {
        return axes.getAxis(axis).getInterpMovementTimeMs();
    }

    public int getSensitivity(Axes.Axis axis) {
        return axes.getAxis(axis).getJoySens();
    }

    public int getJoyValue(Axes.Axis axis) {
        return axes.getAxis(axis).getJoyVal();
    }

}
