
package com.feyiuremote.libs.Feiyu;

import java.math.BigInteger;

public class FeyiuState {

    private final static String TAG = FeyiuState.class.getSimpleName();

    public FeyiuAngle angle_pan = new FeyiuAngle();
    public FeyiuAngle angle_tilt = new FeyiuAngle();
    public FeyiuAngle angle_yaw = new FeyiuAngle();
    public Long last_update = 0L;
    public Long update_interval = 0L;

    public long last_command = 0L;

    private static final FeyiuState mInstance = new FeyiuState();

    public static int joy_sens_pan = 0;

    public static int joy_sens_tilt = 0;

    public static int joy_val_pan = 0;

    public static int joy_val_tilt = 0;

    public static FeyiuState getInstance() {
        return mInstance;
    }

    public static FeyiuAngle getAngle(Axes.Axis axis) {
        if (axis == Axes.Axis.PAN) {
            return getInstance().angle_pan;
        } else if (axis == Axes.Axis.TILT) {
            return getInstance().angle_tilt;
        } else if (axis == Axes.Axis.YAW) {
            return getInstance().angle_yaw;
        }

        return null;
    }

    public static boolean angleIsCritical() {
        if (Math.abs(getInstance().angle_pan.value()) > 260 ||
                Math.abs(getInstance().angle_tilt.value()) > 260) {
            return true;
        }

        return false;
    }

    public static Integer getSensitivity(Axes.Axis axis) {
        if (axis == Axes.Axis.PAN) {
            return joy_sens_pan;
        } else if (axis == Axes.Axis.TILT) {
            return joy_val_tilt;
        } else {
            throw new RuntimeException("That axis does not exist!");
        }
    }

    public void update(byte[] data) {
        if (last_update > 0) {
            update_interval = System.currentTimeMillis() - last_update;
        }

        if (data.length > 15) {
            angle_tilt.update((new BigInteger(new byte[]{data[11], data[10]})).intValue());
            angle_pan.update((new BigInteger(new byte[]{data[15], data[14]})).intValue());
            angle_yaw.update((new BigInteger(new byte[]{data[13], data[12]})).intValue());
        }

        last_update = System.currentTimeMillis();
    }

    public void update(int joy_sens_pan, int joy_sens_tilt) {
        FeyiuState.joy_sens_pan = joy_sens_pan;
        FeyiuState.joy_sens_tilt = joy_sens_tilt;
    }

    public Long getUpdateInterval() {
        return this.update_interval;
    }

    public Long nextUpdateInMs() {
        if (this.update_interval > 0 && this.last_update > 0) {

            return this.update_interval - getTimeSinceLastUpdateMs();
        } else {
            return getAverageUpdateIntervalMs();
        }
    }

    public Long getAverageUpdateIntervalMs() {
        return 243L;
    }

    public Long getTimeSinceLastUpdateMs() {
        if (last_update > 0) {
            return System.currentTimeMillis() - last_update;
        }

        return 0L;
    }

    public long getTimeSinceLastCommandMs() {
        return System.currentTimeMillis() - last_command;
    }

    public boolean isStationary() {
        return angle_pan.isStationary() && angle_tilt.isStationary() && angle_yaw.isStationary();
    }
}

