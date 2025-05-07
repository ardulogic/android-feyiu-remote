package com.feyiuremote.libs.Feiyu.queue;

import android.util.Log;

import com.feyiuremote.libs.Feiyu.Axes;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.queue.commands.JoyCommandLooselyTimed;
import com.feyiuremote.libs.Feiyu.queue.commands.JoyCommandStrictlyTimed;
import com.feyiuremote.libs.Feiyu.queue.commands.SingleSensitivityCommand;

/**
 * Scheduler for joystick pan/tilt commands – compatible with JDK6/7 (no lambdas, no java.util.Objects).
 */
public final class FeyiuControls {

    private final static String TAG = FeyiuControls.class.getSimpleName();
    public static int SHORT = 300;
    public static int REGULAR = 1000;

    /**
     * Queues set sensitivity command
     *
     * @param axis
     * @param value
     */
    public static void setSensitivity(Axes.Axis axis, int value) {
        if (FeyiuState.getSensitivity(axis) != value) {
            FeyiuCommandQueue.submitSingle(new SingleSensitivityCommand(axis, value));
        } else {
            Log.i(TAG, "SetSensitivity: Sensitivity is the same, skipping...");
        }
    }

    public static void setPanSensitivity(int value) {
        setSensitivity(Axes.Axis.PAN, value);
    }

    public static void setTiltSensitivity(int value) {
        setSensitivity(Axes.Axis.TILT, value);
    }

    /**
     * Queues a loosely timed joy command
     */
    public static void setLooseJoyFor(Axes.Axis axis, int value, int duration) {
        FeyiuCommandQueue.submit(new JoyCommandLooselyTimed(axis, value, duration));
    }

    public static void setPanJoyFor(int value, int duration) {
        setLooseJoyFor(Axes.Axis.PAN, value, duration);
    }

    public static void setPanJoy(int value) {
        setLooseJoyFor(Axes.Axis.PAN, value, SHORT);
    }

    public static void setTiltJoyFor(int value, int duration) {
        setLooseJoyFor(Axes.Axis.TILT, value, duration);
    }

    public static void setTiltJoy(int value) {
        setLooseJoyFor(Axes.Axis.TILT, value, SHORT);
    }

    /**
     * Queues a strictly timed joy command
     *
     * @param axis
     * @param value
     * @param delay
     * @param duration
     */
    public static void setStrictJoyAfter(Axes.Axis axis, int value, int delay, int duration) {
        FeyiuCommandQueue.submit(new JoyCommandStrictlyTimed(axis, value, duration, delay));
    }

    public static void setPanJoyAfter(int value, int delay, int duration) {
        setStrictJoyAfter(Axes.Axis.PAN, value, delay, duration);
    }

    public static void setTiltJoyAfter(int value, int delay, int duration) {
        setStrictJoyAfter(Axes.Axis.TILT, value, delay, duration);
    }

    public static void clearAll() {
        FeyiuCommandQueue.cancelQueuedCommands();
    }


}
