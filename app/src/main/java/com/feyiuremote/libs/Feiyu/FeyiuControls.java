package com.feyiuremote.libs.Feiyu;

import android.os.Handler;
import android.os.HandlerThread;
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

    static int THRESHOLD_MS = 10;

    static int DEFAULT_JOY_DURATION = 1000;

    static int BLUETOOTH_COMMAND_MIN_DELAY = 40;

    static public int BLUETOOTH_COMMAND_DEFAULT_DELAY = 100;


    static public final LinkedList<JoystickState> queuedJoyStates = new LinkedList<>();
    static public final LinkedList<SensitivityState> queuedSensitivityStates = new LinkedList<>();

    private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private static HandlerThread handlerThread;

    private static Handler backgroundHandler;

    private static String currentComment = "";


    private static Handler getBackgroundThreadHandler() {
        handlerThread = new HandlerThread("FeyiuControlsHandlerThread");

        if (backgroundHandler == null) {
            handlerThread.start();
            backgroundHandler = new Handler(handlerThread.getLooper());
        }

        return backgroundHandler;
    }

    private static final Thread backgroundThread = new Thread(() -> {
        Looper.prepare();

        // Step 4: Create a Runnable that performs the task
        Runnable task = new Runnable() {
            @Override
            public void run() {
                tick(); // Call your processing method

                long delay = BLUETOOTH_COMMAND_DEFAULT_DELAY;

                JoystickState js = nextJoystickState();
                if (js != null && js.hasDelay()) {
                    if ((js.executesInMs() >= BLUETOOTH_COMMAND_MIN_DELAY) && (js.executesInMs() < BLUETOOTH_COMMAND_DEFAULT_DELAY * 1.5)) {
                        delay = js.executesInMs();
//                        Log.d(TAG, "Move - Scheduling next process in: " + delay + " (orig: " + js.executesInMs() + ")");
                    } else {
//                        Log.d(TAG, "Move - Scheduling next process in default delay (orig: " + js.executesInMs() + ")");
                    }
                }

                if (!queuedJoyStates.isEmpty()) {
//                    Log.d(TAG, "JoystickStates - Scheduling process in: " + delay);
                }

                getBackgroundThreadHandler().postDelayed(this, delay);
            }
        };

        // Initial post to start the loop
        long delay = FeyiuState.getInstance().nextUpdateInMs();
        getBackgroundThreadHandler().postDelayed(task, delay); // 10 milliseconds delay

        // Start the message loop for this thread
        Looper.loop();
    });

    public synchronized static void init(BluetoothLeService mBluetoothLeService) {
        mBt = mBluetoothLeService;

        // Check if the thread is already started and alive
        if (!backgroundThread.isAlive()) {
            try {
                backgroundThread.start();
            } catch (IllegalThreadStateException e) {
                Log.e(TAG, "Background thread already started: " + e.getMessage());
            }
        } else {
            Log.d(TAG, "Background thread is already running.");
        }
    }


    private synchronized static long minDelayToNextCommand() {
        return Math.max(0, THRESHOLD_MS - timeSinceLastBtCommand());
    }


    // Step 1: Create a background thread

    private synchronized static Long timeToNextOperation() {
        Long timeMin = null;

        for (JoystickState j : queuedJoyStates) {
            if (timeMin == null || (j.executesInMs() < timeMin)) {
                timeMin = j.executesInMs();
            }
        }

        return timeMin;
    }

    private synchronized static JoystickState nextJoystickState() {
        if (!queuedJoyStates.isEmpty()) {
            for (JoystickState queuedJoyState : queuedJoyStates) {
                if (!queuedJoyState.isExecuting()) {
                    return queuedJoyState;
                }
            }
        }

        return null;
    }

    public static void logQueuedJoyCommands() {
        for (JoystickState j : queuedJoyStates) {
            Log.d(TAG, j.toString());
        }
    }

    public static synchronized void tick() {
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
        queuePanState(value, DEFAULT_JOY_DURATION, 0, reason);
    }

    public static void setTiltJoy(int value, String reason) {
        queueTiltState(value, DEFAULT_JOY_DURATION, 0, reason);
    }

    public static void setPanJoyAfter(int value, int delay_ms, String reason) {
        queuePanState(value, DEFAULT_JOY_DURATION, delay_ms, reason);
    }

    public static void setTiltJoyAfter(int value, int delay_ms, String reason) {
        queueTiltState(value, DEFAULT_JOY_DURATION, delay_ms, reason);
    }

    public static void queuePanState(Integer joyValue, int duration, int delay, String reason) {
        queueState(joyValue, duration, delay, JoystickState.AXIS_PAN, reason);
    }

    public static void queueTiltState(Integer joyValue, int duration, int delay, String reason) {
        queueState(joyValue, duration, delay, JoystickState.AXIS_TILT, reason);
    }

    public static synchronized void queueState(Integer joyValue, int duration, int delay, int axis, String reason) {
        updateTimeSinceLastRequest();

        JoystickState newState = new JoystickState(joyValue, duration, axis, delay, reason);
//        Log.d(TAG, "Queueing new joystick state:" + newState);

        // Remove any existing joystick states with the same axis
        queuedJoyStates.removeIf((state) ->
                state.overlappingWith(newState)
        );

        if (!newState.matchesStoppedState()) {
            queuedJoyStates.add(newState);
        }

        // Transfer the elements to a temporary list for sorting and consolidation
        queuedJoyStates.sort(new JoystickState.SortByTimeAsc());

        Log.d(TAG, "Moving:" + queuedJoyStates.toString());
    }

    public static void setPanSensitivity(int sensitivity) {
        updateTimeSinceLastRequest();
        queuedSensitivityStates.add(new SensitivityState(sensitivity, SensitivityState.TYPE_PAN, null));
        consolidateQueuedSensitivityChanges();

    }

    public static void setTiltSensitivity(int sensitivity) {
        updateTimeSinceLastRequest();
        queuedSensitivityStates.add(new SensitivityState(sensitivity, SensitivityState.TYPE_TILT, null));
        consolidateQueuedSensitivityChanges();
    }

    public static void consolidateQueuedSensitivityChanges() {
        // Sort by type and time first
        queuedSensitivityStates.sort(new SensitivityState.SortByTypeAndTime());
        queuedSensitivityStates.removeIf((state) -> !state.differsFromCurrent());
    }

    public static int[] getCurrentJoyValues() {
        // Repeat previous state if no command is given
        int joyPan = FeyiuState.joy_val_pan;
        int joyTilt = FeyiuState.joy_val_tilt;

        LinkedList<JoystickState> expiredStates = new LinkedList<>();

        currentComment = "";
        String panComment = "Default value";
        String tiltComment = "Default value";

        if (!queuedJoyStates.isEmpty()) {
            for (JoystickState queuedJoyState : queuedJoyStates) {
                if (!queuedJoyState.executionEnded()) {
                    if (queuedJoyState.executesInMs() < THRESHOLD_MS) {
                        if (queuedJoyState.axis == JoystickState.AXIS_PAN) {
                            joyPan = queuedJoyState.joy_value;
                            panComment = queuedJoyState.reason;
                        } else {
                            joyTilt = queuedJoyState.joy_value;
                            tiltComment = queuedJoyState.reason;
                        }
                    } else {
//                        Log.e(TAG, "Moving / skipping:" + queuedJoyState.toString());
                    }
                } else {
                    expiredStates.add(queuedJoyState);
//                    Log.w(TAG, "Moving (Execution ended for " + queuedJoyState.toString());
                }
            }
        } else {
            joyPan = 0;
            joyTilt = 0;
        }

        Log.d(TAG, joyPan + " : " + joyTilt);

        // delete expired states from queuedJoyStates
        queuedJoyStates.removeAll(expiredStates);

        currentComment = panComment + " | " + tiltComment;

        return new int[]{joyPan, joyTilt};
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
        int[] states = getCurrentJoyValues();

        boolean requestToStop = (states[0] == 0) && (states[1] == 0);
        boolean stateIsStopped = FeyiuState.joy_val_pan == 0 && FeyiuState.joy_val_tilt == 0;

        if (!requestToStop || !stateIsStopped) {
            move(states[0], states[1]);
        }
    }

    public synchronized static void move(int panJoy, int tiltJoy) {
        updateTimeSinceLastBtCommand();

        MoveCommand c = new MoveCommand(mBt, panJoy, tiltJoy, currentComment);
        c.run();
    }

    public synchronized static void cancelQueuedCommands() {
        queuedJoyStates.clear();
        queuedSensitivityStates.clear();
    }

    public synchronized static String commandsToString() {
        StringBuilder d = new StringBuilder();

        for (SensitivityState targetState : queuedSensitivityStates) {
            d.append("\n" + targetState.toString());
        }

        for (JoystickState targetState : queuedJoyStates) {
            d.append("\n" + targetState.toString());
        }

        return d.toString();
    }

}