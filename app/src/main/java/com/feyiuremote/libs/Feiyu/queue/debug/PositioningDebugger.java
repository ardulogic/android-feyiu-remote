package com.feyiuremote.libs.Feiyu.queue.debug;

import com.feyiuremote.libs.Feiyu.Axes;
import com.feyiuremote.libs.Feiyu.processors.position.GimbalPositionTarget;

public class PositioningDebugger {
    private static final String TAG = PositioningDebugger.class.getSimpleName();

    public static Long timePanStopCommandSent;
    public static Long timeTiltStopCommandSent;

    static DebuggableAxis[] axes = new DebuggableAxis[2];

    public static void init() {
        axes[Axes.Axis.PAN.ordinal()] = new DebuggableAxis(Axes.Axis.PAN);
        axes[Axes.Axis.TILT.ordinal()] = new DebuggableAxis(Axes.Axis.TILT);
    }

    public static void onAxisStartStopping(Axes.Axis axis, GimbalPositionTarget target) {
        axes[axis.ordinal()].onStopping(target);
    }

    public static void onAxisStopped(Axes.Axis axis, GimbalPositionTarget target) {
        axes[axis.ordinal()].onStopped(target);
    }

    public static void onAxisStopCommand(Axes.Axis axis) {
        axes[axis.ordinal()].onStopCommandSent();
    }

    public static void onPanStopCommand() {
        timePanStopCommandSent = System.currentTimeMillis();
    }

    public static void onTiltStopCommand() {
        timeTiltStopCommandSent = System.currentTimeMillis();
    }


    public static void onTargetReached(GimbalPositionTarget target) {
        axes[Axes.Axis.PAN.ordinal()].log();
        axes[Axes.Axis.TILT.ordinal()].log();
    }

}
