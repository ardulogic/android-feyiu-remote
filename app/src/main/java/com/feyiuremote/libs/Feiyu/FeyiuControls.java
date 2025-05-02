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
import com.feyiuremote.libs.Feiyu.controls.ConsolidatedJoystickState;
import com.feyiuremote.libs.Feiyu.controls.JoystickState;
import com.feyiuremote.libs.Feiyu.controls.SensitivityState;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class FeyiuControls {
    private final static String TAG = FeyiuControls.class.getSimpleName();
    static private BluetoothLeService mBt;
    static private long time_next_execution = 0;

    static int DEFAULT_JOY_DURATION = 1000;

    private static final int SHORT_JOY_DURATION = 80;

    static int BLUETOOTH_COMMAND_MIN_DELAY = 40;

    static public int BLUETOOTH_COMMAND_DEFAULT_DELAY = 100;

    static public final LinkedList<JoystickState> queuedJoyStates = new LinkedList<>();
    static public final LinkedList<SensitivityState> queuedSensitivityStates = new LinkedList<>();

    private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private static HandlerThread handlerThread;

    private static Handler backgroundHandler;

    private static ConsolidatedJoystickState consolidatedJoystickState;
    private static long time_tick = 0;


    private static Handler getBackgroundThreadHandler() {
        handlerThread = new HandlerThread("FeyiuControlsHandlerThread");

        if (backgroundHandler == null) {
            handlerThread.start();
            backgroundHandler = new Handler(handlerThread.getLooper());
        }

        return backgroundHandler;
    }

    // Step 4: Create a Runnable that performs the task
    static Runnable executionTask = () -> {
        Log.w(TAG, "Scheduling: ------ TICK -------");

        tick(); // Call your processing method

        long delay = BLUETOOTH_COMMAND_DEFAULT_DELAY;

        JoystickState js = nextJoystickTimedState();
        if (js != null) {
            Long shouldExecuteIn = js.executesInMs();

            if (shouldExecuteIn < BLUETOOTH_COMMAND_DEFAULT_DELAY) {
                delay = Math.max(BLUETOOTH_COMMAND_MIN_DELAY, shouldExecuteIn);
                Log.w(TAG, "Scheduling: (Post Tick) next " + js.axisToString() + " command at delay:" + delay + "ms (ideally: " + shouldExecuteIn + "ms)");
            } else {
                if (shouldExecuteIn < BLUETOOTH_COMMAND_DEFAULT_DELAY + BLUETOOTH_COMMAND_MIN_DELAY) {
                    delay = shouldExecuteIn;
                    Log.w(TAG, "Scheduling: (Post Tick) Prolonging next " + js.axisToString() + " command to: " + delay + ". Task should execute in:" + shouldExecuteIn + "ms");
                } else {
                    Log.w(TAG, "Scheduling: (Post Tick) next " + js.axisToString() + " command at default delay. Task should execute in:" + shouldExecuteIn + "ms");
                }
            }
        } else {
            Log.d(TAG, "Scheduling: (Post Tick) Default timing, no timed state in queue: " + queuedJoyStates.size());
        }

        runTaskAfter(delay);
    };

    private static final Thread backgroundThread = new Thread(() -> {
        Looper.prepare();

        // Initial post to start the loop
        long delay = FeyiuState.getInstance().nextUpdateInMs();
        runTaskAfter(delay);

        // Start the message loop for this thread
        Looper.loop();
    });

    public static long nextExecutionInMs() {
        return time_next_execution - System.currentTimeMillis();
    }

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

    private synchronized static JoystickState nextJoystickTimedState() {
        if (!queuedJoyStates.isEmpty()) {
            for (JoystickState queuedJoyState : queuedJoyStates) {
                if (queuedJoyState.executesInFuture()
                        && queuedJoyState.hasStrictTiming()) {
                    return queuedJoyState;
                }
            }
        }

        return null;
    }

    private synchronized static JoystickState nextJoystickTimedStateOnAxis(JoystickState newState) {
        if (!queuedJoyStates.isEmpty()) {
            for (JoystickState queuedJoyState : queuedJoyStates) {
                if ((Objects.equals(queuedJoyState.axis, newState.axis))
                        && queuedJoyState.executesInFuture()
                        && queuedJoyState.hasStrictTiming()) {
                    return queuedJoyState;
                }
            }
        }

        return null;
    }

    public static void logQueuedSensitivityCommands() {
        for (SensitivityState j : queuedSensitivityStates) {
            Log.d(TAG, j.toString());
        }
    }

    public static void logQueuedJoyCommands() {
        for (JoystickState j : queuedJoyStates) {
            Log.d(TAG, j.toString());
        }
    }

    public static synchronized void tick() {
        time_tick = System.currentTimeMillis();

        executeCommands();
    }

    public static void setPanJoy(int value, String reason) {
        queuePanState(value, DEFAULT_JOY_DURATION, null, reason);
    }

    public static void setTiltJoy(int value, String reason) {
        queueTiltState(value, DEFAULT_JOY_DURATION, null, reason);
    }

    public static void setPanJoyAfter(int value, int delay_ms, String reason) {
        queuePanState(value, SHORT_JOY_DURATION, delay_ms, reason);
    }

    public static void setTiltJoyAfter(int value, int delay_ms, String reason) {
        queueTiltState(value, SHORT_JOY_DURATION, delay_ms, reason);
    }

    public static void queuePanState(Integer joyValue, int duration, Integer delay, String reason) {
        queueState(joyValue, duration, delay, JoystickState.AXIS_PAN, reason);
    }

    public static void queueTiltState(Integer joyValue, int duration, Integer delay, String reason) {
        queueState(joyValue, duration, delay, JoystickState.AXIS_TILT, reason);
    }

    /**
     * Adding state to existing queue
     * We replace states that are already executing and have the same parameters
     *
     * @param newState
     */
    public static synchronized void addStateToQueue(JoystickState newState) {
        boolean newIsStrict = newState.hasStrictTiming();
        boolean cancelCurrentExecutingState = false;
        boolean rescheduleNextTick = false;
        boolean addToQueue = true;

        if (newIsStrict && queueHasStrictTimingStates()) {
            JoystickState futureTimedStateOnSameAxis = nextJoystickTimedStateOnAxis(newState);
            if (futureTimedStateOnSameAxis != null) {
                if (newState.executesInMs() > BLUETOOTH_COMMAND_MIN_DELAY + 10) {
                    futureTimedStateOnSameAxis.onCancelled("replace by fresher timed state: " + newState);
                } else {
                    newState.setComment("Future state (" + futureTimedStateOnSameAxis + ") is already timed and this one wants to execute too soon.");
                    addToQueue = false;
                }
            } else {
                newState.setComment("There are timed states on other axis already.");
                addToQueue = false;
            }
        } else {
            if (newIsStrict) {
                // It shouldnt replace the current executing state
                // because then there is no state between cancellation and the scheduled state
                if (taskExecutesSoonerThanNextTick(newState) ||
                        taskExecutesTooSoonAfterNextTick(newState)) {
                    rescheduleNextTick = true;
                }
            } else {
                cancelCurrentExecutingState = true;
            }
        }

        Iterator<JoystickState> it = queuedJoyStates.iterator();
        while (it.hasNext()) {
            JoystickState existing = it.next();
            boolean sameAxis = existing.axis.equals(newState.axis);
            boolean sameJoyVal = existing.joy_value.equals(newState.joy_value);

            if (cancelCurrentExecutingState) {
                if (sameAxis && existing.isExecuting()) {
                    existing.onCancelled("Replaced by new state:" + newState);
                    it.remove();
                }
            }
        }


        if (addToQueue) {
            queuedJoyStates.add(newState);
            queuedJoyStates.sort(new JoystickState.SortByExecutionTimeAsc());

            Log.w(TAG, "Scheduling: ACCEPTED for " + newState);

            if (rescheduleNextTick) {
                runNextTaskInBestTiming(newState);
            }
        } else {
            Log.e(TAG, "Scheduling: DECLINED for " + newState);
        }
    }

    public static synchronized void queueState(Integer joyValue, int duration, Integer delay, int axis, String reason) {
        JoystickState newState = new JoystickState(joyValue, duration, axis, delay, reason);
        Log.w(TAG, "Scheduling: Queuing new state:" + newState);

        if (!newState.matchesStoppedState()) {
            addStateToQueue(newState);
        } else {
            Log.e(TAG, "Scheduling: DECLINED (Matches stopped state) for " + newState);
        }
    }

    private static boolean taskExecutesSoonerThanNextTick(JoystickState newState) {
        return newState.executesInMs() < nextExecutionInMs();
    }

    private static boolean taskExecutesTooSoonAfterNextTick(JoystickState newState) {
        long timeAfterNextExecution = newState.executesInMs() - nextExecutionInMs();

        return timeAfterNextExecution < BLUETOOTH_COMMAND_MIN_DELAY + 10;
    }

    private static void runNextTaskInBestTiming(JoystickState newState) {
        if (taskExecutesSoonerThanNextTick(newState)) {
            long minDelay = BLUETOOTH_COMMAND_MIN_DELAY - FeyiuState.getInstance().getTimeSinceLastCommandMs();

            getBackgroundThreadHandler().removeCallbacks(executionTask);
            runTaskAfter(Math.max(newState.executesInMs(), minDelay));

            Log.w(TAG, "Scheduling: (Best timing) immediate " + newState.axisToString() + " command of " + newState.executesInMs() + " ms in " + minDelay + "ms");
        } else if (taskExecutesTooSoonAfterNextTick(newState)) {
            long timeAfterNextExecution = newState.executesInMs() - nextExecutionInMs();
            long timeDeficit = BLUETOOTH_COMMAND_MIN_DELAY - timeAfterNextExecution;
            long newDelay = nextExecutionInMs() - timeDeficit - 10;

            getBackgroundThreadHandler().removeCallbacks(executionTask);
            runTaskAfter(newDelay);
            Log.w(TAG, "Scheduling: (Best timing) intermediate " + newState.axisToString() + " command of " + newDelay + "ms for state in:" + newState.executesInMs() + " ms, since time left after next execution:" + timeAfterNextExecution + "ms");
        } else {
            Log.w(TAG, "Scheduling: (Best timing) as default for " + newState.axisToString() + " state");
        }
    }

    public static synchronized boolean queueHasStrictTimingStates() {
        Iterator<JoystickState> it = queuedJoyStates.iterator();
        while (it.hasNext()) {
            JoystickState existing = it.next();

            if (existing.hasStrictTiming()) {
                return true;
            }
        }

        return false;
    }

    private static void runTaskAfter(long delay) {
        getBackgroundThreadHandler().removeCallbacks(executionTask);
        getBackgroundThreadHandler().postDelayed(executionTask, delay);
        time_next_execution = System.currentTimeMillis() + delay;
    }

    public static void setPanSensitivity(int sensitivity) {
        queuedSensitivityStates.add(new SensitivityState(sensitivity, SensitivityState.TYPE_PAN, null));
        consolidateQueuedSensitivityChanges();

    }

    public static void setTiltSensitivity(int sensitivity) {
        queuedSensitivityStates.add(new SensitivityState(sensitivity, SensitivityState.TYPE_TILT, null));
        consolidateQueuedSensitivityChanges();
    }

    public static void consolidateQueuedSensitivityChanges() {
        // Sort by type and time first
        queuedSensitivityStates.sort(new SensitivityState.SortByTypeAndTime());
        queuedSensitivityStates.removeIf((state) -> !state.differsFromCurrent());
    }

    public static ConsolidatedJoystickState getConsolidatedJoystickState() {
        LinkedList<JoystickState> expiredStates = new LinkedList<>();

        consolidatedJoystickState = new ConsolidatedJoystickState();

        if (!queuedJoyStates.isEmpty()) {
            for (JoystickState queuedJoyState : queuedJoyStates) {
                boolean stateQualifies = !queuedJoyState.isExpired() // Can just be never executed
                        && !queuedJoyState.isExecuted()
                        && !queuedJoyState.isCancelled();

                if (stateQualifies) {
                    if (queuedJoyState.executesInMs() < BLUETOOTH_COMMAND_MIN_DELAY) {
                        if (queuedJoyState.axis == JoystickState.AXIS_PAN) {
                            consolidatedJoystickState.addPanStateContender(queuedJoyState);
                        } else {
                            consolidatedJoystickState.addTiltStateContender(queuedJoyState);
                        }
                    }
                } else {
                    expiredStates.add(queuedJoyState);
                }
            }
        } else {
//            Log.d(TAG, "Scheduling: Cant make consolidated joystick state, queue is empty.");
        }

        // delete expired states from queuedJoyStates
        queuedJoyStates.removeAll(expiredStates);

        return consolidatedJoystickState;
    }

    public synchronized static void executeCommands() {
        if (sensitivityChangesArePending()) {
            logQueuedSensitivityCommands();
            changeSensitivity();
        } else {
            logQueuedJoyCommands();
            moveIfNecessary();
        }
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

            c.run();
        }
    }

    public synchronized static void moveIfNecessary() {
        ConsolidatedJoystickState cState = getConsolidatedJoystickState();

        if (cState.isNotEmpty()) {
            cState.prepare();
            move(cState.joyPan, cState.joyTilt);
            cState.onExecuted();
        } else {
            Log.w(TAG, "Not moving consolidated state is empty!");
        }
    }

    public synchronized static void stop() {
        queuedJoyStates.clear();

        queuePanState(0, 200, 0, "Stop requested");
        queueTiltState(0, 200, 0, "Stop requested");
    }

    private static long timeSinceLastTick() {
        return System.currentTimeMillis() - time_tick;
    }

    public static void move(int panJoy, int tiltJoy) {
        long msSinceLastCommand = FeyiuState.getInstance().getTimeSinceLastCommandMs();
        long msSinceLastTick = timeSinceLastTick();

        Log.d(TAG, "Moving: ---- Since BT(" + msSinceLastCommand + ") Since Tick(" + msSinceLastTick + ") ---------");

        if (msSinceLastCommand < BLUETOOTH_COMMAND_MIN_DELAY) {
            Log.e(TAG, "Moving: About to send BT command too soon!");
        }

        if (msSinceLastTick > 5) {
            Log.e(TAG, "Moving: About to send command thats lagging " + msSinceLastTick + "ms from tick start!");
        }

        executorService.execute(() -> {
            MoveCommand c = new MoveCommand(mBt, panJoy, tiltJoy);
            c.run();
        });
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