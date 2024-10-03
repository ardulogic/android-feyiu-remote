package com.feyiuremote.libs.Feiyu.processors.position;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.calibration.CalibrationDbHelper;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Objects;

import androidx.annotation.NonNull;

public class GimbalPositionTarget {
    private final String TAG = GimbalPositionTarget.class.getSimpleName();
    private static final int SPEED_NEGATIVE = 0;
    private static final int SPEED_POSITIVE = 1;

    private final double pan_angle;
    private final double tilt_angle;
    private final CalibrationDbHelper mDb;
    private final Double focus;
    private final double pan_diff_at_start;
    private final double tilt_diff_at_start;

    public final int dwell_time_ms;
    private ContentValues pan_speed_cal;
    private ContentValues tilt_speed_cal;

    private boolean pan_stopping = false;
    private boolean tilt_stopping = false;


    public GimbalPositionTarget(Context context, double pan_angle, double tilt_angle, double max_pan_speed, double max_tilt_speed, int dwell_time_ms, Double focus) {
        this.mDb = new CalibrationDbHelper(context);

        // Note that without pan_angle panDiff is wrong!
        this.pan_angle = pan_angle;
        this.pan_diff_at_start = angleDiffInDeg(mDb.AXIS_PAN);
        this.pan_speed_cal = findFastestCalibration(mDb.AXIS_PAN, max_pan_speed);

        // Note that without tilt_angle tiltDiff is wrong!
        this.tilt_angle = tilt_angle;
        this.tilt_diff_at_start = angleDiffInDeg(mDb.AXIS_TILT);
        this.tilt_speed_cal = findFastestCalibration(mDb.AXIS_TILT, max_tilt_speed);

        this.focus = focus;
        this.dwell_time_ms = dwell_time_ms;

        // Slow down faster axis
        String faster_axis = getFasterAxis();
        setCal(faster_axis, findMatchingCalibration(faster_axis));

        Log.d(TAG, "Ready");
    }

    private void setCal(String axis, ContentValues calibration) {
        if (Objects.equals(axis, mDb.AXIS_PAN)) {
            this.pan_speed_cal = calibration;
        } else {
            this.tilt_speed_cal = calibration;
        }
    }

//    private void adjustToInertia() {
//        Float panSpeed = FeyiuState.getInstance().angle_pan.speed();
//        Float tiltSpeed = FeyiuState.getInstance().angle_tilt.speed();
//        ContentValues panCal = mDb.getByClosestPanSpeed(panSpeed);
//        ContentValues tiltCal = mDb.getByClosestTiltSpeed(tiltSpeed);
//
//        float pan_overshoot = panCal.getAsFloat("pan_angle_overshoot") / 2;
//        float tilt_overshoot = tiltCal.getAsFloat("tilt_angle_overshoot") / 2;
//
//        Log.d(TAG, String.format("Initial true speed, Pan: %.2f deg/s (overshoot: %.2f), Tilt: %.2f deg/s (overshoot: %.2f)", panSpeed, pan_overshoot, tiltSpeed, tilt_overshoot));
//
//        this.pan_speed = Math.min(pan_speed, Math.abs(panDiffInDeg()) - Math.abs(pan_overshoot));
//        this.tilt_speed = Math.min(tilt_speed, Math.abs(tiltDiffInDeg()) - Math.abs(tilt_overshoot));
//    }

    private String getFasterAxis() {
        if (Math.abs(getMovementTimeInMs(mDb.AXIS_PAN)) < Math.abs(getMovementTimeInMs(mDb.AXIS_TILT))) {
            return mDb.AXIS_PAN;
        } else {
            return mDb.AXIS_TILT;
        }
    }

    private String otherAxis(String axis) {
        if (axis.equals(mDb.AXIS_PAN)) {
            return mDb.AXIS_TILT;
        } else {
            return mDb.AXIS_PAN;
        }
    }

    private ContentValues findMatchingCalibration(String axis) {

        ArrayList<ContentValues> cals = mDb.getSlowerThan(axis, getDir(angleDiffInDeg(axis)), speed(axis));
        if (cals.size() == 0) {
            Log.e(TAG, "No calibrations slower than " + speed(axis) + " found for " + axis);
        }

        double min_time_diff = 9999999;
        ContentValues bestCal = null;

        for (ContentValues cal : cals) {
            double time_diff = getMovementTimeDiffMsAbs(axis, cal, otherAxis(axis), getCal(otherAxis(axis)));

            if (time_diff < min_time_diff) {
                min_time_diff = time_diff;
                bestCal = cal;
                Log.d(TAG, String.format("Finding matching calibration for %s. Original:  %.1f deg/s in %.1fms, ideal: %.1fms, Better: %.1f deg/s in  %.1fms, diff: %.1fms",
                        axis,
                        speed(axis),
                        getMovementTimeInMs(axis), // original
                        getMovementTimeInMs(otherAxis(axis)), // ideal other axis
                        speed(axis, bestCal), // better deg/s
                        getMovementTimeInMs(axis, bestCal), // better ms
                        time_diff //diff
                ));
            }
        }

        if (bestCal == null) {
            Log.e(TAG, "Could not find a better matching calibration for " + axis);
            return getCal(axis); // Returning same cal
        }

        return bestCal;
    }

    private ContentValues findFastestCalibration(String axis, double max_speed) {
        double angle_diff = angleDiffInDeg(axis);
        max_speed = Math.max(1.5, Math.min(max_speed, Math.abs(angle_diff) * 1.5));
        ArrayList<ContentValues> cals = mDb.getSlowerThan(axis, getDir(angle_diff), max_speed);

        double min_time = (double) FeyiuState.getInstance().getAverageUpdateInterval();
        double max_time = 99999999;
        ContentValues bestCal = null;

        for (ContentValues cal : cals) {
            double time = Math.abs(getMovementTimeInMs(axis, cal));

            if (time > min_time && time < max_time) {
                max_time = time;
                bestCal = cal;
                Log.d(TAG, String.format("Finding fastest cal for %s (%.1f deg diff): %.1f %.1fms",
                        axis, angle_diff, speed(axis, bestCal), time
                ));
            }
        }

        if (bestCal == null) {
            Log.e(TAG, "Could not find " + axis + " calibration for deg diff:" + angleDiffInDeg(mDb.AXIS_PAN));
            return mDb.getByClosestSpeed(axis, max_speed);
        }

        return bestCal;
    }


    public Double getFocus() {
        return this.focus;
    }

    public double angleDiffInDeg(String axis) {
        double target, current;

        if (Objects.equals(axis, mDb.AXIS_PAN)) {
            target = panTarget();
            current = FeyiuState.getInstance().angle_pan.value();
        } else {
            target = tiltTarget();
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


    public double speed(String axis, ContentValues cal) {
        return cal.getAsDouble(axis + "_speed");
    }

    public double speed(String axis) {
        if (Objects.equals(axis, mDb.AXIS_PAN)) {
            return speed(axis, this.pan_speed_cal);
        } else {
            return speed(axis, this.tilt_speed_cal);
        }
    }

    public int getDir(double diff) {
        if (diff < 0) {
            return -1;
        }

        return 1;
    }

    public double panTarget() {
        return this.pan_angle;
    }

    public double tiltTarget() {
        return this.tilt_angle;
    }


    public ContentValues panCal() {
        return pan_speed_cal;
    }

    public ContentValues tiltCal() {
        return tilt_speed_cal;
    }

    private double getMovementTimeDiffMsAbs(String axis1, ContentValues cal1, String axis2, ContentValues cal2) {
        return Math.abs(Math.abs(getMovementTimeInMs(axis1, cal1)) - Math.abs(getMovementTimeInMs(axis2, cal2)));
    }

    public int getPanSensitivity() {
        return panCal().getAsInteger("joy_sens");
    }

    public int getTiltSensitivity() {
        return tiltCal().getAsInteger("joy_sens");
    }

    public float getPanOvershoot() {
        return panCal().getAsFloat("pan_angle_overshoot");
    }

    public float getTiltOvershoot() {
        return tiltCal().getAsFloat("tilt_angle_overshoot");
    }

    public int getPanJoyValue() {
        return panCal().getAsInteger("joy_val");
    }

    public float getPanSpeedDegPerSec() {
        return panCal().getAsFloat("pan_speed");
    }

    public float getTiltSpeedDegPerSec() {
        return tiltCal().getAsFloat("tilt_speed");
    }

    public int getTiltJoyValue() {
        return tiltCal().getAsInteger("joy_val");
    }

    public boolean isReached() {
        return isPanReached() && isTiltReached();
    }

    public boolean isPositionReached() {
        return Math.abs(angleDiffInDeg(mDb.AXIS_PAN)) < 2 && Math.abs(angleDiffInDeg(mDb.AXIS_TILT)) < 2;
    }

    /**
     * TODO: Might be a good idea to multiply the avg update interval here x1.2
     * It should be more reliable
     *
     * @param movement_time
     * @return
     */
    public boolean stoppingPointReached(double movement_time) {
        return Math.abs(movement_time) < FeyiuState.getInstance().getAverageUpdateInterval();
    }

    public boolean nearPointReached(double movement_time) {
        return Math.abs(movement_time) < FeyiuState.getInstance().getAverageUpdateInterval() * 1.5;
    }

    public boolean panIsStopping() {
        return this.pan_stopping;
    }

    public boolean tiltIsStopping() {
        return this.tilt_stopping;
    }


    public boolean tiltShouldMove() {
        return !tiltIsStopping();
    }

    public boolean panShouldMove() {
        return !panIsStopping();
    }

    public void setPanIsStopping() {
        this.pan_stopping = true;
    }

    public void setTiltIsStopping() {
        this.tilt_stopping = true;
    }

    public double getMovementTimeInMs(double angle_difference, double angular_velocity, double overshoot) {
        return Math.abs((angle_difference - overshoot) / angular_velocity) * 1000;
    }

    public double getMovementTimeInMs(String axis, ContentValues calibration) {
        double angle_difference = angleDiffInDeg(axis);
        double overshoot = calibration.getAsDouble(axis + "_angle_overshoot");
        double angular_speed = calibration.getAsDouble(axis + "_speed");

        return getMovementTimeInMs(angle_difference, angular_speed, overshoot);
    }

    public double getMovementTimeInMs(String axis) {
        if (axis.equals(mDb.AXIS_PAN)) {
            return getMovementTimeInMs(axis, this.pan_speed_cal);
        } else {
            return getMovementTimeInMs(axis, this.tilt_speed_cal);
        }
    }

    public double getInterpolatedMovementTimeMs(double angleDiff, double angularVelocity, double overshoot) {
//        overshoot *= 1.1;
        double angleToStop = (float) (angleDiff - overshoot);
        double timeToStop = Math.abs(angleToStop / angularVelocity); // deg / deg/sec
        return (double) (timeToStop) * 1000 - FeyiuState.getInstance().getTimeSinceLastUpdate(); // Convert to milliseconds
    }


    public ContentValues getCal(String axis) {
        if (axis.equals(mDb.AXIS_PAN)) {
            return this.pan_speed_cal;
        } else {
            return this.tilt_speed_cal;
        }
    }

    public double getPanMovementTime() {
        return getInterpolatedMovementTimeMs(
                angleDiffInDeg(mDb.AXIS_PAN),
                getPanSpeedDegPerSec(),
                getPanOvershoot()
        );
    }

    public double getTiltMovementTime() {
        return getInterpolatedMovementTimeMs(
                angleDiffInDeg(mDb.AXIS_TILT),
                getTiltSpeedDegPerSec(),
                getTiltOvershoot()
        );
    }

    public boolean isPanReached() {
        // This if is just for debug purpoises
        return stoppingPointReached(getPanMovementTime()) && panIsStopping();
    }

    public boolean isTiltReached() {
        // This if is just for debug purpoises
        return stoppingPointReached(getTiltMovementTime()) && tiltIsStopping();
    }

    public void logDiffs() {
        Log.d(TAG, String.format("Pan-df: %.2f %.2f | Tilt-df: %.2f %.2f | Pan-reached: %b Tilt-reached: %b",
                angleDiffInDeg(mDb.AXIS_PAN), getPanMovementTime(), angleDiffInDeg(mDb.AXIS_TILT), getTiltMovementTime(), isPanReached(), isTiltReached()));
    }

    public boolean axisIsOvershooting(double currentAngleDiff, double staringAngleDiff) {
        if (currentAngleDiff == 0) {
            return false;
        } else if (Math.signum(currentAngleDiff) != Math.signum(staringAngleDiff)) {
            return true;
        }

        return false;
    }

    public boolean panIsOvershooting() {
        return axisIsOvershooting(angleDiffInDeg(mDb.AXIS_PAN), pan_diff_at_start);
    }

    public boolean tiltIsOvershooting() {
        return axisIsOvershooting(angleDiffInDeg(mDb.AXIS_TILT), tilt_diff_at_start);
    }

    public boolean panShouldStop() {
        boolean stoppingPointReached = stoppingPointReached(getPanMovementTime());
        boolean accurateEnough = Math.abs(angleDiffInDeg(mDb.AXIS_PAN)) < 1;

        return stoppingPointReached || accurateEnough || panIsOvershooting();
    }

    public boolean tiltShouldStop() {
        boolean stoppingPointReached = stoppingPointReached(getTiltMovementTime());
        boolean accurateEnough = Math.abs(angleDiffInDeg(mDb.AXIS_TILT)) < 1;
        boolean tiltIsOvershooting = tiltIsOvershooting();

        return stoppingPointReached || accurateEnough || tiltIsOvershooting;
    }

    public boolean isNearby() {
        boolean nearbyPanPoint = nearPointReached(getPanMovementTime());
        boolean nearbyTiltPoint = nearPointReached(getTiltMovementTime());

        return nearbyPanPoint && nearbyTiltPoint;
    }

    @NonNull
    public String toString() {
        DecimalFormat decimalFormat = new DecimalFormat("#.#");
        String d = String.format("%s, %s, %s",
                FeyiuState.getInstance().angle_pan.speedToString(),
                FeyiuState.getInstance().angle_tilt.speedToString(),
                FeyiuState.getInstance().angle_yaw.speedToString());

        d += "\nTime: P:" + Math.round(getPanMovementTime()) + " T:" + Math.round(getTiltMovementTime());
        d += "\n Stopping: P:" + panIsStopping() + " T:" + tiltIsStopping();
        d += "\n Angle Df: P:" + decimalFormat.format(angleDiffInDeg(mDb.AXIS_PAN)) + " T:" + decimalFormat.format(angleDiffInDeg(mDb.AXIS_TILT));
        d += "\n Angle To: P:" + decimalFormat.format(panTarget()) + " T:" + decimalFormat.format(tiltTarget());
        d += "\n Speed: P:" + decimalFormat.format(getPanSpeedDegPerSec()) + " T:" + decimalFormat.format(getTiltSpeedDegPerSec());
        d += "\n Target Joy: P:" + getPanJoyValue() + " T:" + getTiltJoyValue();
        d += "\n Actual Joy: P:" + FeyiuState.joy_val_pan + " T:" + FeyiuState.joy_val_tilt;
        d += "\n Target Sens: P:" + getPanSensitivity() + " T:" + getTiltSensitivity();
        d += "\n Actual Sens: P:" + FeyiuState.joy_sens_pan + " T:" + FeyiuState.joy_sens_tilt;

        return d;
    }
}
