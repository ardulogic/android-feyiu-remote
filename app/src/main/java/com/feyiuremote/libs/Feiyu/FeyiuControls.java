package com.feyiuremote.libs.Feiyu;

import android.util.Log;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.calibration.commands.GimbalCommand;
import com.feyiuremote.libs.Feiyu.calibration.commands.MoveCommand;
import com.feyiuremote.libs.Feiyu.calibration.commands.SetPanSensitivityCommand;
import com.feyiuremote.libs.Feiyu.calibration.commands.SetTiltSensitivityCommand;
import com.feyiuremote.libs.Feiyu.controls.JoystickState;
import com.feyiuremote.libs.Feiyu.controls.SensitivityState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FeyiuControls {
    private final static String TAG = FeyiuControls.class.getSimpleName();
    static private BluetoothLeService mBt;
    static private long time_last_bt_command = 0;
    static private long time_last_request = 0;

    static private boolean tick_scheduled = false;

    static Timer timer = new Timer();

    static int THRESHOLD_MS = 30;

    static private boolean stopped = false;

    static private ArrayList<JoystickState> queuedJoyStates = new ArrayList<>();
    static private ArrayList<SensitivityState> queuedSensitivityStates = new ArrayList<>();

    static private JoystickState currentJoyState = new JoystickState(0, 0, null);

    private static ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private synchronized static long minDelayToNextCommand() {
        return Math.max(0, THRESHOLD_MS - timeSinceLastBtCommand());
    }

    public static synchronized void tick() {
        consolidateQueuedStates();
        consolidateQueuedSensitivityChanges();

        if (timeSinceLastBtCommand() > THRESHOLD_MS) {
            tick_scheduled = false;
            executeCommands();
        } else {
            if (!tick_scheduled) {
                Log.d(TAG, "Delaying tick since its too close to prev command:" + timeSinceLastBtCommand() + "ms");

                executorService.schedule(FeyiuControls::tick, minDelayToNextCommand(), TimeUnit.MILLISECONDS);
                //TODO: Does not work correctly! Sensitivy is not being set as it should!
                // Again problem with states/concurrency
                tick_scheduled = true;
            }
        }
    }

    public synchronized static long timeSinceLastBtCommand() {
        return System.currentTimeMillis() - time_last_bt_command;
    }

    public synchronized static long timeSinceLastRequest() {
        return System.currentTimeMillis() - time_last_request;
    }

    private synchronized static void updateTimeSinceLastBtCommand() {
        time_last_bt_command = System.currentTimeMillis();
    }

    private synchronized static void updateTimeSinceLastRequest() {
        time_last_request = System.currentTimeMillis();
    }

    public synchronized static void setPanJoy(int value) {
        updateTimeSinceLastRequest();
        queuedJoyStates.add(new JoystickState(value, null, 0));
    }

    public synchronized static void setTiltJoy(int value) {
        updateTimeSinceLastRequest();
        queuedJoyStates.add(new JoystickState(null, value, 0));
    }

    public synchronized static void setTiltJoyAfter(int value, int delay) {
        updateTimeSinceLastRequest();

        queuedJoyStates.add(new JoystickState(null, value, delay));
    }


    public synchronized static void setPanJoyAfter(int value, int delay) {
        updateTimeSinceLastRequest();

        queuedJoyStates.add(new JoystickState(value, null, delay));
    }

    public synchronized static void setPanSensitivity(int sensitivity) {
        queuedSensitivityStates.add(new SensitivityState(sensitivity, SensitivityState.TYPE_PAN, null));
        updateTimeSinceLastRequest();
    }

    public synchronized static void setTiltSensitivity(int sensitivity) {
        updateTimeSinceLastRequest();

        queuedSensitivityStates.add(new SensitivityState(sensitivity, SensitivityState.TYPE_TILT, null));
    }

    public synchronized static void consolidateQueuedSensitivityChanges() {
        ArrayList<SensitivityState> consolidatedSensitivityStates = (ArrayList<SensitivityState>) queuedSensitivityStates.clone();

        consolidatedSensitivityStates.sort(new SensitivityState.SortByTypeAndTime());


        SensitivityState prevState = null;
        Iterator<SensitivityState> iterator = consolidatedSensitivityStates.iterator();

        while (iterator.hasNext()) {
            SensitivityState targetState = iterator.next();
            if (targetState.differsFromCurrent()) {
                if (prevState == null) {
                    prevState = targetState;
                } else {
                    if (targetState.isSameTypeAs(prevState) && (targetState.timeDiffWith(prevState) < THRESHOLD_MS)) {
                        prevState.mergeWith(targetState);
                        iterator.remove(); // Safely remove the current element
                    } else {
                        prevState = targetState;
                    }
                }
            } else {
                // Is the same
                iterator.remove();
            }
        }

        consolidatedSensitivityStates.sort(new SensitivityState.SortByTime());

        queuedSensitivityStates = consolidatedSensitivityStates;
    }

    public synchronized static void consolidateQueuedStates() {
        // Copying values to avoid concurrent exception
        ArrayList<JoystickState> consolidatedJoyStates = (ArrayList<JoystickState>) queuedJoyStates.clone();

        consolidatedJoyStates.sort(new JoystickState.SortByTime());

        // Merge states if time diff is close
        JoystickState prevState = null;
        Iterator<JoystickState> iterator = consolidatedJoyStates.iterator();

        while (iterator.hasNext()) {
            JoystickState targetState = iterator.next();

            if (prevState == null) {
                prevState = targetState;
            } else {
                if (targetState.timeDiffWith(prevState) < THRESHOLD_MS) {
                    prevState.mergeWith(targetState);
                    iterator.remove(); // Safely remove the current element
                } else {
                    prevState = targetState;
                }
            }
        }

        // Apply immediate states to current state
        iterator = consolidatedJoyStates.iterator();

        while (iterator.hasNext()) {
            JoystickState targetState = iterator.next();

            if (targetState.executesIn() < THRESHOLD_MS) {
                currentJoyState.mergeWith(targetState);
                iterator.remove(); // Safely remove the current element
            }
        }

        queuedJoyStates = consolidatedJoyStates;

        // Whats left should be only queued states
        for (JoystickState targetState : queuedJoyStates) {
            timer.schedule(new TimerTask() {
                @Override
                public synchronized void run() {
                    currentJoyState.mergeWith(targetState);
                    queuedJoyStates.remove(targetState);
                    tick();
                }
            }, targetState.executesIn());
        }
    }

    public synchronized static void executeCommands() {
        if (sensitivityChangesArePending()) {
            changeSensitivity();
        } else {
            moveIfNecessary();
        }
    }

    public synchronized static boolean sensitivityChangesArePending() {
        return queuedSensitivityStates.size() > 0;
    }

    public synchronized static void changeSensitivity() {
        SensitivityState st = queuedSensitivityStates.get(0);

        if (st.differsFromCurrent()) {
            updateTimeSinceLastBtCommand();

            GimbalCommand c;
            if (st.type == SensitivityState.TYPE_PAN) {
                c = new SetPanSensitivityCommand(mBt, st.sens);
            } else {
                c = new SetTiltSensitivityCommand(mBt, st.sens);
            }

            c.run();
        }

        queuedSensitivityStates.remove(st);

        // Sensitivity changes can be done quicker
        tick();
    }

    public synchronized static void moveIfNecessary() {
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
        return currentJoyState.panJoy == 0 && currentJoyState.tiltJoy == 0;
    }

    public synchronized static void cancelQueuedCommands() {
        // Cancel previous timer first
        timer.cancel();
        timer = new Timer();
        queuedJoyStates.clear();
        queuedSensitivityStates.clear();
    }


    public synchronized static void init(BluetoothLeService mBluetoothLeService) {
        mBt = mBluetoothLeService;
    }
}