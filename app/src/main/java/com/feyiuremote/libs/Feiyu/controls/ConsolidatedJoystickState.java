package com.feyiuremote.libs.Feiyu.controls;


import android.util.Log;

import com.feyiuremote.libs.Feiyu.FeyiuState;

import java.util.LinkedList;

public class ConsolidatedJoystickState {

    private static final String TAG = "ConsolidatedJoystickState";

    public int joyPan;
    public int joyTilt;


    LinkedList<JoystickState> panStates = new LinkedList<>();

    LinkedList<JoystickState> tiltStates = new LinkedList<>();
    private JoystickState executablePanState;
    private JoystickState executableTiltState;

    public ConsolidatedJoystickState() {
        joyPan = FeyiuState.joy_val_pan;
        joyTilt = FeyiuState.joy_val_tilt;
    }

    public void prepare() {
        joyPan = 0;
        joyTilt = 0;

        executablePanState = preparePanState();
        executableTiltState = prepareTiltState();

        if (executablePanState != null) {
            joyPan = executablePanState.joy_value;
        }

        if (executableTiltState != null) {
            joyTilt = executableTiltState.joy_value;
        }

        if (executablePanState == null && executableTiltState == null) {
            Log.e(TAG, "Scheduling: Cant create consolidated state, empty.");
        }
    }

    public void addPanStateContender(JoystickState state) {
        panStates.add(state);
    }

    public void addTiltStateContender(JoystickState state) {
        tiltStates.add(state);
    }

    public JoystickState preparePanState() {
        return prepareState(panStates);
    }

    public JoystickState prepareTiltState() {
        return prepareState(tiltStates);
    }


    private JoystickState prepareState(LinkedList<JoystickState> states) {
        if (states.isEmpty()) return null;

        int lastIndex = states.size() - 1;
        for (int i = 0; i < states.size(); i++) {
            JoystickState s = states.get(i);

            if (i < lastIndex) {
                s.onCancelled();
            } else {
                Log.w(TAG, "Prepared State (" + states.size() + "): " + s);
                return s;
            }
        }

        return null;
    }


    public void onExecuted() {
        if (executablePanState != null) {
            executablePanState.onExecution();
        }

        if (executableTiltState != null) {
            executableTiltState.onExecution();
        }

        log();
    }

    /**
     * Make sure to log before execution since hasStrictTiming changes
     */
    public void log() {
        long panTimeError = 0;
        long tiltTimeError = 0;

        if (executableTiltState != null) {
            if (executableTiltState.getFirstExecutionTimingError() != null) {
                tiltTimeError = executableTiltState.getFirstExecutionTimingError();
            }
        }


        if (executablePanState != null) {
            if (executablePanState.getFirstExecutionTimingError() != null) {
                panTimeError = executablePanState.getFirstExecutionTimingError();
            }
        }

        if (panTimeError > 0 || tiltTimeError > 0) {
            Log.w(TAG, "Moving: Timing error: Pan(" + panTimeError + "ms) Tilt(" + tiltTimeError + "ms) "
                    + "\n" + executablePanState +
                    "\n" + executableTiltState);
        } else {
            Log.d(TAG, "Moving:"
                    + "\n" + executablePanState +
                    "\n" + executableTiltState);
        }

//        if (panTimeError > 30 || tiltTimeError > 30) {
//            Log.w(TAG, "Moving: Pan(" + joyPan + ")  Tilt(" + joyTilt + ") \t Timing error: Pan(" + panTimeError + "ms) Tilt(" + tiltTimeError + "ms)");
//        } else {
//            Log.d(TAG, "Moving: Pan(" + joyPan + ")  Tilt(" + joyTilt + ") \t Timing error: Pan(" + panTimeError + "ms) Tilt(" + tiltTimeError + "ms)");
//        }
    }

    public boolean isNotEmpty() {
        return !panStates.isEmpty() || !tiltStates.isEmpty();
    }
}
