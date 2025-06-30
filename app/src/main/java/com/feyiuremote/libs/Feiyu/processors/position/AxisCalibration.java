package com.feyiuremote.libs.Feiyu.processors.position;

import android.content.ContentValues;
import android.util.Log;

import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.calibration.CalibrationDB;

import java.util.Objects;

public class AxisCalibration {
    private final String TAG = AxisCalibration.class.getSimpleName();

    public static final String AXIS_PAN = "pan";
    public static final String AXIS_TILT = "tilt";

    private final CalibrationDB mDb;

    private Double init_angle_diff;
    private String axis;

    private ContentValues cal;
    private Double target_angle;

    public AxisCalibration(CalibrationDB mDb, String axis, Double target_angle) {
        this.mDb = mDb;
        this.axis = axis;

        this.target_angle = target_angle;
        this.init_angle_diff = getCurrentAngleDiff();
    }

    public ContentValues getFastest(Double angleDiff, Double max_speed) {
        return mDb.getFastest(this.axis, angleDiff, max_speed);
    }

    public ContentValues getFastestForStartingAngleDiff(Double max_speed) {
        return getFastest(this.init_angle_diff, max_speed);
    }

    public double getInitAngleDiff() {
        return init_angle_diff;
    }

    public ContentValues getCal() {
        return cal;
    }

    public boolean matchCalibrationTo(AxisCalibration target, Double max_speed) {
        // Compute the desired movement time from the target axis (in ms)
        double targetTimeMs = target.getMovementTimeInMs();

        // if your max_speed is in deg/sec, convert ms→s:
        double angleDiff = getCurrentAngleDiff();
        double idealSpeedDegPerSec = angleDiff / (targetTimeMs / 1_000.0);

        // Cap by the provided maximum:
        double chosenSpeed = Math.copySign(
                Math.min(Math.abs(idealSpeedDegPerSec), max_speed),
                angleDiff
        );

        if (Double.isNaN(chosenSpeed) || Double.isInfinite(chosenSpeed)) {
            Log.e(TAG, "Problem while matching calibration, computed speed is invalid");
            chosenSpeed = Math.copySign(1, angleDiff); // Or a safe fallback value
        }

        // How can I pass the sign of angleDiff?
        this.cal = mDb.getClosestToSpeed(axis, chosenSpeed);

        if (this.cal == null) {
            Log.e(TAG, "Could not find calibration!");

            return false;
        } else {
            double error = getMovementTimeInMs() - targetTimeMs;
            Log.d(TAG, "Matched calibration with " + error + " ms deviation");

            return true;
        }
    }

    public int getJoySens() {
        return cal.getAsInteger("joy_sens");
    }

    public int getJoyVal() {
        return cal.getAsInteger("joy_val");
    }

    public double getSpeedDegPerSec() {
        return cal.getAsDouble(axis + "_speed");
    }

    /**
     * Does not account for time passed since last gimbal update
     * Useful for initiation
     *
     * @return
     */
    public double getMovementTimeInMs() {
        double angle_difference = getCurrentAngleDiff();

        if (cal != null) {
            double overshoot = cal.getAsDouble(axis + "_angle_overshoot");
            double angular_speed = cal.getAsDouble(axis + "_speed");

            return Math.abs((angle_difference - overshoot) / angular_speed) * 1000;
        }

        Log.e(TAG, "Could not calculate movement time, since cal is null!");
        return 0;
    }

    public double getCurrentAngle() {
        if (Objects.equals(this.axis, AXIS_PAN)) {
            return FeyiuState.getInstance().angle_pan.value();
        } else {
            return FeyiuState.getInstance().angle_tilt.value();
        }
    }

    public double getCurrentSpeed() {
        if (Objects.equals(this.axis, AXIS_PAN)) {
            return FeyiuState.getInstance().angle_pan.speed();
        } else {
            return FeyiuState.getInstance().angle_tilt.speed();
        }
    }

    public double getLiveSpeed() {
        if (axis.equals(AXIS_PAN)) {
            return FeyiuState.getInstance().angle_pan.instantSpeed();
        } else {
            return FeyiuState.getInstance().angle_tilt.instantSpeed();
        }
    }

    /**
     * Accounts for the time passed since last gimbal command
     * Uses current data
     *
     * @return
     */
    public double getInterpMovementTimeMs() {
        double angleDiff = getCurrentAngleDiff();
        double angleSpeedDegPerSec = getSpeedDegPerSec();
        double overshoot = getOvershoot();
        double realSpeed = getLiveSpeed();

        Log.d(TAG, axis + "Angle Diff: " + angleDiff + " AngleSpeed:" + angleSpeedDegPerSec + " Overshoot:" + overshoot);
        double extraAngle = ((double) FeyiuState.getInstance().getTimeSinceLastUpdateMs() / 1000 * angleSpeedDegPerSec);

        // Accounts for time passed since last update
        angleDiff -= extraAngle;
        Log.d(TAG, axis + "Extra Angle: " + extraAngle + " Final diff:" + angleDiff);

        double timeToStop = angleDiff / angleSpeedDegPerSec * 1000.0; // deg / deg/sec
        Log.d(TAG, axis + "Time To stop: " + timeToStop);

//        if (Math.abs(realSpeed) > Math.abs(angleSpeedDegPerSec) * 0.3) {
        double overshootCompensation = Math.abs(overshoot / angleSpeedDegPerSec * 1000.0);
        timeToStop -= overshootCompensation;

        Log.d(TAG, axis + "Final time to stop: " + timeToStop + "(" + overshootCompensation + ") compensation)");
//        }
//        Log.d(TAG, "Real "  + axis + " speed: " + realSpeed + " calSpeed:" + angleSpeedDegPerSec);

        return timeToStop;
    }

    public double getInitialAngleDiff() {
        return init_angle_diff;
    }

    public double getCurrentAngleDiff() {
        double target, current;

        target = this.target_angle;
        current = getCurrentAngle();

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

    public void assign(ContentValues calibration) {
        this.cal = calibration;
    }

    public float getOvershoot() {
        return this.cal.getAsFloat(axis + "_angle_overshoot");
    }

    public Double getTargetAngle() {
        return target_angle;
    }
}
