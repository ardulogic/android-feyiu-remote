package com.feyiuremote.libs.Feiyu.queue.debug;

import android.util.Log;

import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.processors.position.GimbalPositionTarget;

import java.util.ArrayList;

public class QueueStopDebugger {
    private static final String TAG = QueueStopDebugger.class.getSimpleName();
    private static final int MAX_MEASUREMENTS = 10;
    public static long startTime = 0L;

    public static ArrayList<WaypointState> measurements = new ArrayList<>();

    public static WaypointState startStopTiltState;
    public static WaypointState startStopPanState;
    public static WaypointState targetReachedState;

    public static Long timePanStopCommandSent;
    public static Long timeTiltStopCommandSent;
    private static float finalPanAngle;
    private static float finalTiltAngle;

    public static void onPanStartStopping(GimbalPositionTarget target) {
        startStopPanState = new WaypointState(target);

        if (startStopPanState.panTime < 40) {
            Log.e(TAG, "PAN time is less than minimum to stop smoothly! (" + startStopPanState.panTime + "ms)");
//            throw new RuntimeException("Pan time is less than minimum to stop smoothly!");
        }
    }

    public static void onTiltStartStopping(GimbalPositionTarget target) {
        startStopTiltState = new WaypointState(target);

        if (startStopTiltState.tiltTime < 40) {
            Log.e(TAG, "Tilt time is less than minimum to stop smoothly! (" + startStopTiltState.tiltTime + "ms)");
//            throw new RuntimeException("Tilt time is less than minimum to stop smoothly!");
        }
    }

    public static void onPanStopCommand() {
        timePanStopCommandSent = System.currentTimeMillis();
    }

    public static void onTiltStopCommand() {
        timeTiltStopCommandSent = System.currentTimeMillis();
    }

    public static void analyseAndDebug() {
        Log.d(TAG, "------- ANALYSIS -------");

        // --- Pan timing ---
        long expectedPanDelay = startStopPanState.panTime;
        long actualPanDelay = timePanStopCommandSent - startStopPanState.timestamp;
        long panDelayError = expectedPanDelay - actualPanDelay;
        double targetPanAngle = startStopPanState.targetPanAngle;
        double finishPanAngle = finalPanAngle;
        double panAngleError = targetPanAngle - finishPanAngle;
        double panAcceptableAngleError = startStopPanState.panCalSpeed * ((double) panDelayError / 1000.0);
        double unaccountedPanError = Math.abs(panAngleError) - Math.abs(panAcceptableAngleError);
        double errorPanAsInMovementMs = unaccountedPanError / startStopPanState.panCalSpeed * 1000.0;

        Log.d(TAG, String.format(
                "Pan stop command delay error: %dms (expected: %dms, actual: %dms)",
                panDelayError, expectedPanDelay, actualPanDelay
        ));
        Log.d(TAG, String.format(
                "\tangle: target: %.2f°, finish: %.2f°, error: %.2f°",
                targetPanAngle, finishPanAngle, panAngleError
        ));
        Log.d(TAG, String.format(
                "\tspeed: %.2f deg/s, %.4f deg/ms, acceptable-angle-error: %.2f°",
                startStopPanState.panCalSpeed,
                startStopPanState.panCalSpeed / 1000.0,
                panAcceptableAngleError
        ));
        // --- New pan “unaccounted” and ms‐equivalent computations ---
        Log.w(TAG, String.format(
                "\tConclusion: %.2f° remain unaccounted for, as in ms: %.2fms",
                unaccountedPanError,
                errorPanAsInMovementMs
        ));

        if (startStopPanState.panTime < 40) {
            Log.e(TAG,
                    "\tSystem tried to stop in time when it was impossible! Panning time left was only:"
                            + startStopPanState.panTime + "ms!");
        }

        // --- Tilt timing ---
        long expectedTiltDelay = startStopTiltState.tiltTime;
        long actualTiltDelay = timeTiltStopCommandSent - startStopTiltState.timestamp;
        long tiltDelayError = expectedTiltDelay - actualTiltDelay;
        double targetTiltAngle = startStopTiltState.targetTiltAngle;
        double finishTiltAngle = finalTiltAngle;
        double tiltAngleError = targetTiltAngle - finishTiltAngle;
        double tiltAcceptableAngleError = startStopTiltState.tiltCalSpeed * ((double) tiltDelayError / 1000.0);
        double unaccountedTiltError = Math.abs(tiltAngleError) - Math.abs(tiltAcceptableAngleError);
        double errorTiltAsInMovementMs = unaccountedTiltError / startStopTiltState.tiltCalSpeed * 1000.0;

        Log.d(TAG, String.format(
                "Tilt stop command delay error: %dms (expected: %dms, actual: %dms)",
                tiltDelayError, expectedTiltDelay, actualTiltDelay
        ));
        Log.d(TAG, String.format(
                "\tangle: target: %.2f°, finish: %.2f°, error: %.2f°",
                targetTiltAngle, finishTiltAngle, tiltAngleError
        ));
        Log.d(TAG, String.format(
                "\tspeed: %.2f deg/s, %.4f deg/ms, acceptable-angle-error: %.2f°",
                startStopTiltState.tiltCalSpeed,
                startStopTiltState.tiltCalSpeed / 1000.0,
                tiltAcceptableAngleError
        ));
        Log.w(TAG, String.format(
                "\tConclusion: %.2f° remain unaccounted for, as in ms: %.2fms",
                unaccountedTiltError,
                errorTiltAsInMovementMs
        ));
        if (startStopTiltState.tiltTime < 40) {
            Log.e(TAG,
                    "\tSystem tried to stop in time when it was impossible! Panning time left was only:"
                            + startStopTiltState.tiltTime + "ms!");
        }
    }


    public static void onTargetReached(GimbalPositionTarget target) {
        targetReachedState = new WaypointState(target);
        finalPanAngle = FeyiuState.getInstance().angle_pan.value();
        finalTiltAngle = FeyiuState.getInstance().angle_tilt.value();

        analyseAndDebug();
    }
}
