package com.feyiuremote.libs.Feiyu;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.calibration.commands.GimbalCommand;
import com.feyiuremote.libs.Feiyu.calibration.commands.MoveCommand;
import com.feyiuremote.libs.Feiyu.calibration.commands.SetPanSensitivityCommand;
import com.feyiuremote.libs.Feiyu.calibration.commands.SetTiltSensitivityCommand;
import com.feyiuremote.libs.Feiyu.controls.JoystickState;
import com.feyiuremote.libs.Feiyu.controls.SensitivityState;

import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class FeyiuControls {
    private final static String TAG = FeyiuControls.class.getSimpleName();
    static private BluetoothLeService mBt;
    static private long time_last_bt_command = 0;
    static private long time_last_request = 0;

    static int THRESHOLD_MS = 30;

    static int BLUETOOTH_COMMAND_MIN_DELAY = 60;

    static private boolean stopped = false;

    static private final LinkedList<JoystickState> queuedJoyStates = new LinkedList<>();
    static private final LinkedList<SensitivityState> queuedSensitivityStates = new LinkedList<>();

    static private final JoystickState currentJoyState = new JoystickState(0, 0, null, "default state");

    private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    static Handler backgroundHandler = new Handler(Looper.myLooper());

    private synchronized static long minDelayToNextCommand() {
        return Math.max(0, THRESHOLD_MS - timeSinceLastBtCommand());
    }


    // Step 1: Create a background thread
    private static final Thread backgroundThread = new Thread(() -> {
        Looper.prepare();

        // Step 4: Create a Runnable that performs the task
        Runnable task = new Runnable() {
            @Override
            public void run() {
                tick(); // Call your processing method

                Long timeToNextOp = timeToNextOperation();
                long delay = Math.max(BLUETOOTH_COMMAND_MIN_DELAY, timeToNextOp == null ? 0 : timeToNextOp);

                if (!queuedJoyStates.isEmpty()) {
                    Log.d(TAG, "JoystickStates - Scheduling process in: " + delay + "  Ttop:" + timeToNextOp);
                }

                backgroundHandler.postDelayed(this, delay);
            }
        };

        // Initial post to start the loop
        long delay = FeyiuState.getInstance().nextUpdateInMs();
        backgroundHandler.postDelayed(task, delay); // 10 milliseconds delay

        // Start the message loop for this thread
        Looper.loop();
    });

    private static Long timeToNextOperation() {
        Long timeMin = null;

        for (JoystickState j : queuedJoyStates) {
            if (timeMin == null || (j.executesInMs() < timeMin)) {
                timeMin = j.executesInMs();
            }
        }

        return timeMin;
    }

    public static void logQueuedJoyCommands() {
        for (JoystickState j : queuedJoyStates) {
            Log.d(TAG, j.toString());
        }
    }

    public static synchronized void tick() {
        consolidateQueuedJoystickStates();
        consolidateQueuedSensitivityChanges();
        executeCommands();
    }

    public static long timeSinceLastBtCommand() {
        return System.currentTimeMillis() - time_last_bt_command;
    }

    public static long timeSinceLastRequest() {
        return System.currentTimeMillis() - time_last_request;
    }

    private static void updateTimeSinceLastBtCommand() {
        time_last_bt_command = System.currentTimeMillis();
    }

    private static void updateTimeSinceLastRequest() {
        time_last_request = System.currentTimeMillis();
    }

    public static void setPanJoy(int value, String reason) {
        updateTimeSinceLastRequest();
        queuedJoyStates.add(new JoystickState(value, null, 0, reason));
    }

    public static void setTiltJoy(int value, String reason) {
        updateTimeSinceLastRequest();


        queuedJoyStates.add(new JoystickState(null, value, 0, reason));
    }

    public static void setTiltJoyAsFinal(int value, String reason) {
        updateTimeSinceLastRequest();


        queuedJoyStates.add(new JoystickState(null, value, 0, true, reason));
    }

    public static void setPanJoyAsFinal(int value, String reason) {
        updateTimeSinceLastRequest();


        queuedJoyStates.add(new JoystickState(value, null, 0, true, reason));
    }

    public static void setTiltJoyAfter(int value, int delay, String reason) {
        updateTimeSinceLastRequest();

        queuedJoyStates.add(new JoystickState(null, value, delay, reason));
    }


    public static void setPanJoyAfter(int value, int delay, String reason) {
        updateTimeSinceLastRequest();

        queuedJoyStates.add(new JoystickState(value, null, delay, reason));
    }

    public static void setPanSensitivity(int sensitivity) {
        queuedSensitivityStates.add(new SensitivityState(sensitivity, SensitivityState.TYPE_PAN, null));
        updateTimeSinceLastRequest();
    }

    public static void setTiltSensitivity(int sensitivity) {
        updateTimeSinceLastRequest();

        queuedSensitivityStates.add(new SensitivityState(sensitivity, SensitivityState.TYPE_TILT, null));
    }

    public static void consolidateQueuedSensitivityChanges() {
        // Transfer the elements to a temporary list for sorting and consolidation
        LinkedList<SensitivityState> sensitivityStates = new LinkedList<>(queuedSensitivityStates);

        // Sort by type and time first
        sensitivityStates.sort(new SensitivityState.SortByTypeAndTime());

        // Clear the original deque as we're going to replace it with consolidated data
        queuedSensitivityStates.clear();

        // Iterate through the sorted list and merge sensitivity states that are similar
        for (SensitivityState targetState : sensitivityStates) {
            if (targetState.differsFromCurrent()) {
                queuedSensitivityStates.add(targetState);
            }
        }
    }

    private static boolean ignoreStateBecauseOfPreviousFinalState(Integer finalPan, Integer finalTilt, JoystickState joyState) {
        if (finalPan != null) {
            if (joyState.tiltJoy == null) {
                return true;
            }
        }

        if (finalTilt != null) {
            if (joyState.panJoy == null) {
                return true;
            }
        }

        return false;
    }

    public static synchronized void consolidateQueuedJoystickStates() {
        // Transfer the elements to a temporary list for sorting and consolidation
        if (queuedJoyStates.isEmpty()) {
            return;
        }

        LinkedList<JoystickState> joyStates = new LinkedList<JoystickState>(queuedJoyStates);
        LinkedList<JoystickState> newStates = new LinkedList<JoystickState>();

        // Sort by type and time first
        joyStates.sort(new JoystickState.SortByTimeAsc());

        Integer finalPan = null;
        Integer finalTilt = null;

        try {
            for (int i = 0; i < joyStates.size(); i++) {
                JoystickState joyState = joyStates.get(i);

                if (joyState.matchesCurrentState()) {
                    continue;
                }

                if (joyState.isFinal) {
                    finalPan = joyState.panJoy == null ? finalPan : joyState.panJoy;
                    finalTilt = joyState.tiltJoy == null ? finalTilt : joyState.tiltJoy;
                } else {
                    if (ignoreStateBecauseOfPreviousFinalState(finalPan, finalTilt, joyState)) {
                        continue;
                    }

                    joyState.tiltJoy = finalTilt == null ? joyState.tiltJoy : finalTilt;
                    joyState.panJoy = finalPan == null ? joyState.panJoy : finalPan;
                }

                if (joyState.executesInMs() < THRESHOLD_MS) {
                    if (newStates.isEmpty()) {
                        newStates.add(joyState);
                    } else {
                        newStates.getFirst().mergeWith(joyState);
                    }
                } else {
                    if (!newStates.isEmpty() && newStates.getLast().timeDiffMsWith(joyState) < THRESHOLD_MS) {
                        newStates.getLast().mergeWith(joyState);
                    } else {
                        newStates.add(joyState);
                    }
                }
            }
        } catch (NullPointerException e) {
            Log.w(TAG, "State no longer exists");
        }

        // Clear the original deque as we're going to replace it with consolidated data
        queuedJoyStates.clear();
        queuedJoyStates.addAll(newStates);
    }

    public synchronized static void executeCommands() {
        executorService.execute(() -> {
            if (sensitivityChangesArePending()) {
                changeSensitivity();
            } else {
                logQueuedJoyCommands();
                moveIfNecessary();
            }
        });
    }

    public synchronized static boolean sensitivityChangesArePending() {
        return queuedSensitivityStates.size() > 0;
    }

    public synchronized static void changeSensitivity() {
        SensitivityState st = queuedSensitivityStates.poll();  // Retrieve and remove the first state

        if (st != null && st.differsFromCurrent()) {
            GimbalCommand c = (st.type == SensitivityState.TYPE_PAN)
                    ? new SetPanSensitivityCommand(mBt, st.sens)
                    : new SetTiltSensitivityCommand(mBt, st.sens);

            updateTimeSinceLastBtCommand();
            c.run();
        }
    }

    public synchronized static void moveIfNecessary() {
        JoystickState state = queuedJoyStates.peek();
        if (state != null && state.executesInNs() <= 0) {
            queuedJoyStates.poll();

            if (state.panJoy != null) {
                currentJoyState.panJoy = state.panJoy;
            }

            if (state.tiltJoy != null) {
                currentJoyState.tiltJoy = state.tiltJoy;
            }

            Log.d(TAG, "Acquired next state");
        }

        if (!shouldBeStopped()) {
            stopped = false;
            move();
        } else {
            if (!stopped) {
                move();
            }

            stopped = true;
        }
    }

    public synchronized static void move() {
        if ((timeSinceLastRequest() > 2000) && (currentJoyState.panJoy != 0) && (currentJoyState.tiltJoy != 0)) {
            Log.e(TAG, "Emergency stop! Pan or Tilt was not stopped in time!");
        }

        updateTimeSinceLastBtCommand();
        MoveCommand c = new MoveCommand(mBt, currentJoyState.panJoy, currentJoyState.tiltJoy);
        c.run();
    }

    public synchronized static boolean shouldBeStopped() {
        try {
            if (currentJoyState != null) {
                return currentJoyState.panJoy == 0 && currentJoyState.tiltJoy == 0;
            }
        } catch (NullPointerException e) {
            Log.d(TAG, "Wtf happened?");
        }

        return false;
    }

    public synchronized static void cancelQueuedCommands() {
        queuedJoyStates.clear();
        queuedSensitivityStates.clear();
    }


    public synchronized static void init(BluetoothLeService mBluetoothLeService) {
        mBt = mBluetoothLeService;

        try {
            backgroundThread.start();
        } catch (IllegalThreadStateException e) {
            backgroundHandler.getLooper().quitSafely();
            backgroundThread.start();
        }
    }

}