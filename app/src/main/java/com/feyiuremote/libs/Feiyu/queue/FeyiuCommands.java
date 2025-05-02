package com.feyiuremote.libs.Feiyu.queue;

import com.feyiuremote.libs.Feiyu.queue.commands.JoyCommandLooselyTimed;
import com.feyiuremote.libs.Feiyu.queue.commands.JoyCommandStrictlyTimed;
import com.feyiuremote.libs.Feiyu.queue.commands.SingleSensitivityCommand;

/**
 * Scheduler for joystick pan/tilt commands – compatible with JDK6/7 (no lambdas, no java.util.Objects).
 */
public final class FeyiuCommands {
    public static int SHORT = 300;
    public static int REGULAR = 1000;

    public static void setPanSensitivity(int value) {
        FeyiuCommandQueue.submitSingle(new SingleSensitivityCommand(FeyiuCommandQueue.Axis.PAN, value));
    }

    public static void setTiltSensitivity(int value) {
        FeyiuCommandQueue.submitSingle(new SingleSensitivityCommand(FeyiuCommandQueue.Axis.TILT, value));
    }

    public static void setPanJoyFor(int value, int duration) {
        FeyiuCommandQueue.submit(new JoyCommandLooselyTimed(FeyiuCommandQueue.Axis.PAN, value, duration));
    }

    public static void setPanJoy(int value) {
        FeyiuCommandQueue.submit(new JoyCommandLooselyTimed(FeyiuCommandQueue.Axis.PAN, value, SHORT));
    }

    public static void setTiltJoyFor(int value, int duration) {
        FeyiuCommandQueue.submit(new JoyCommandLooselyTimed(FeyiuCommandQueue.Axis.TILT, value, duration));
    }

    public static void setTiltJoy(int value) {
        FeyiuCommandQueue.submit(new JoyCommandLooselyTimed(FeyiuCommandQueue.Axis.TILT, value, SHORT));
    }


    public static void clearAll() {
        FeyiuCommandQueue.cancelQueuedCommands();
    }

    public static void setPanJoyAfter(int value, int delay, int duration) {
        FeyiuCommandQueue.submit(new JoyCommandStrictlyTimed(FeyiuCommandQueue.Axis.PAN, value, duration, delay));
    }

    public static void setTiltJoyAfter(int value, int delay, int duration) {
        FeyiuCommandQueue.submit(new JoyCommandStrictlyTimed(FeyiuCommandQueue.Axis.TILT, value, duration, delay));
    }

}
