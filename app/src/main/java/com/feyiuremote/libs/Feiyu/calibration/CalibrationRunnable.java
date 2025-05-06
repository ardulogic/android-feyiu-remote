package com.feyiuremote.libs.Feiyu.calibration;

import android.content.ContentValues;
import android.util.Log;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.Axes;
import com.feyiuremote.libs.Feiyu.FeyiuUtils;
import com.feyiuremote.libs.Feiyu.calibration.commands.FinaliseCommand;
import com.feyiuremote.libs.Feiyu.calibration.commands.GimbalCommand;
import com.feyiuremote.libs.Feiyu.calibration.commands.MoveCalibrationCommand;
import com.feyiuremote.libs.Feiyu.calibration.commands.MoveCalibrationLockedCommand;
import com.feyiuremote.libs.Feiyu.calibration.commands.StartCommand;
import com.feyiuremote.libs.Feiyu.calibration.commands.StopCommand;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CalibrationRunnable {

    private final int mJoySens;
    private final int mJoyVal;
    private final BluetoothLeService mBt;
    private final ICalibrationListener mListener;
    private String type = "default"; // Calibration mode

    private LinkedList<GimbalCommand> commandQueue;

    private LinkedList<CalibrationState> moveStates = new LinkedList<>();
    private int commandIndex = 0;

    private boolean isActive = false;
    private Long lastUpdate;

    private final ScheduledExecutorService txRepeater =
            Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> repeatFuture;        // current repeating task
    private MoveCalibrationCommand lastMoveCommand;    // last “Move” that may be repeated

    public CalibrationRunnable(String id, int joy_sens, int joy_val, LinkedList<GimbalCommand> commandQueue, BluetoothLeService bt, ICalibrationListener listener) {
        this.mJoySens = joy_sens;
        this.mJoyVal = joy_val;
        this.mBt = bt;
        this.commandQueue = commandQueue;
        this.mListener = listener;
        this.type = id;
    }

    private void setSensitivity() {
        mBt.send(FeyiuUtils.SERVICE_ID, FeyiuUtils.CONTROL_CHARACTERISTIC_ID,
                FeyiuUtils.setPanSensitivity(mJoySens)
        );

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mBt.send(FeyiuUtils.SERVICE_ID, FeyiuUtils.CONTROL_CHARACTERISTIC_ID,
                FeyiuUtils.setTiltSensitivity(mJoySens)
        );

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        try {
            Thread.sleep(200);
            setSensitivity();
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        this.isActive = true;
    }

    /**
     * Begin or restart a 100 ms repeating task that just re-runs lastMoveCommand.
     */
    private void startRepeater() {
        stopRepeater();                                 // cancel the previous one, if any
        if (lastMoveCommand == null) return;            // nothing to repeat yet

        repeatFuture = txRepeater.scheduleAtFixedRate(
                () -> {
                    try {
                        (new MoveCalibrationCommand(mBt, lastMoveCommand.pan, lastMoveCommand.tilt, "Extra command.")).run();
                        stopRepeater();
                    } catch (Exception ignore) {
                    }
                },
                100,   // initial delay
                100,   // period
                TimeUnit.MILLISECONDS);
    }

    /**
     * Stop the repeating task.
     */
    private void stopRepeater() {
        if (repeatFuture != null && !repeatFuture.isCancelled()) {
            repeatFuture.cancel(true);
        }
    }

    public synchronized void onGimbalUpdate() {
        if (!isActive) return;

        // any update → stop the duplicate-sender
        stopRepeater();

        // guard: don’t consume queue too quickly
        if (lastUpdate != null && System.currentTimeMillis() - lastUpdate < 100) return;
        lastUpdate = System.currentTimeMillis();

        if (commandIndex >= commandQueue.size()) {                 // finished
            isActive = false;
            finaliseCalibration();
            txRepeater.shutdown();                                 // tidy up
            return;
        }

        GimbalCommand cmd = commandQueue.get(commandIndex++);
        cmd.setComment(this.type);
        cmd.run();

        // remember only real motion commands
        if (cmd instanceof MoveCalibrationCommand
                || cmd instanceof MoveCalibrationLockedCommand) {
            lastMoveCommand = (MoveCalibrationCommand) cmd;
            startRepeater();                                       // fire the 100 ms ticker
        } else {
            lastMoveCommand = null;                                // don’t echo Start/Stop/Finalise
            stopRepeater();
        }
    }

    private Axes.Axis getAxis() {
        return Objects.equals(this.type, "pan_only") ? Axes.Axis.PAN : Axes.Axis.TILT;
    }

    private CalibrationState findAccelerationEndState() {
        moveStates.sort(Comparator.comparingLong(state -> state.timestamp));

        for (int i = 1; i < moveStates.size(); i++) {
            moveStates.get(i).calculateSpeed(moveStates.get(i - 1));
        }

        // Normally its the 3rd entry that is stable even on fastest speeds
        return moveStates.get(2);
    }

    private synchronized void finaliseCalibration() {
        int START = 0;
        int ACCELERATED = 1;
        int STOPPING = 2;
        int STOPPED = 3;

        Float[] pan_angle = new Float[4];
        Float[] tilt_angle = new Float[4];
        Long[] time = new Long[4];

        for (GimbalCommand command : commandQueue) {
            if (command instanceof StartCommand) {
                pan_angle[START] = command.panAngleAtExecution;
                tilt_angle[START] = command.tiltAngleAtExecution;
                time[START] = command.timeExecuted;
                moveStates.push(new CalibrationState(command));
            } else if ((command instanceof MoveCalibrationLockedCommand) || (command instanceof MoveCalibrationCommand)) {
                moveStates.push(new CalibrationState(command));
            } else if (command instanceof StopCommand) {
                pan_angle[STOPPING] = command.panAngleAtExecution;
                tilt_angle[STOPPING] = command.tiltAngleAtExecution;
                time[STOPPING] = command.timeExecuted;
            } else if (command instanceof FinaliseCommand) {
                pan_angle[STOPPED] = command.panAngleAtExecution;
                tilt_angle[STOPPED] = command.tiltAngleAtExecution;
                time[STOPPED] = command.timeExecuted;
            }
        }

        CalibrationState acceleratedState = findAccelerationEndState();
        pan_angle[ACCELERATED] = (float) acceleratedState.pan_angle;
        tilt_angle[ACCELERATED] = (float) acceleratedState.tilt_angle;
        time[ACCELERATED] = acceleratedState.timestamp;

        Log.d("Calibration", pan_angle[0].toString() + pan_angle[1].toString() + pan_angle[2].toString());

        DecimalFormat df = new DecimalFormat("#.###");

        ContentValues cv = new ContentValues();
        cv.put("preset", "default");

        if (type.equals("pan_only")) {
            cv.put("pan_only", 1);
        } else {
            cv.put("pan_only", 0);
        }

        if (type.equals("tilt_only")) {
            cv.put("tilt_only", 1);
        } else {
            cv.put("tilt_only", 0);
        }

        if (type.equals("locked")) {
            cv.put("locked", 1);
        } else {
            cv.put("locked", 0);
        }

        cv.put("joy_sens", mJoySens);
        cv.put("joy_val", mJoyVal);
        cv.put("dir", cv.getAsInteger("joy_val") > 0 ? 1 : -1);

        // It takes time to accelerate to constant speed
        // Goal will be to choose speed so there's enough room to accelerate and calculate stopping
        // points based on the overshoot from stable speed
        double panAngleDiff = angleDifference(pan_angle[ACCELERATED], pan_angle[STOPPING]); // Deg
        cv.put("pan_angle_overshoot", df.format(angleDifference(pan_angle[STOPPING], pan_angle[STOPPED]))); // Deg
        cv.put("pan_speed", df.format(calculateSpeed(panAngleDiff, time[STOPPING], time[ACCELERATED]))); // Deg/s

        double tiltAngleDiff = angleDifference(tilt_angle[ACCELERATED], tilt_angle[STOPPING]); // Deg
        cv.put("tilt_angle_overshoot", df.format(angleDifference(tilt_angle[STOPPING], tilt_angle[STOPPED]))); // Deg
        cv.put("tilt_speed", df.format(calculateSpeed(tiltAngleDiff, time[STOPPING], time[ACCELERATED]))); // Deg/s

//        cv.put("time_ms", Math.abs(time[STOPPING] - time[START]));
        cv.put("time_to_accel", Math.abs(time[START] - time[ACCELERATED]));

        double pan_angle_to_accel = Math.abs(Math.abs(pan_angle[ACCELERATED]) - Math.abs(pan_angle[START]));
        double tilt_angle_to_accel = Math.abs(Math.abs(tilt_angle[ACCELERATED]) - Math.abs(tilt_angle[START]));

        if (pan_angle_to_accel < 1 && tilt_angle_to_accel < 1) {
            Log.w("RuntimeRunnable", "Both accel angles are below zero");
        }

        cv.put("pan_angle_to_accel", Math.abs(Math.abs(pan_angle[ACCELERATED]) - Math.abs(pan_angle[START])));
        cv.put("tilt_angle_to_accel", Math.abs(Math.abs(tilt_angle[ACCELERATED]) - Math.abs(tilt_angle[START])));

        Log.d("CalibrationRunnable", String.valueOf(Math.abs(time[START] - time[ACCELERATED])));

        mListener.onCalFinished(cv);
    }

    private double calculateSpeed(double angle_diff, long time_end_ms, long time_start_ms) {
        return angle_diff / (time_end_ms - time_start_ms) * 1000;
    }

    public static double angleDifference(double angle1, double angle2) {
        return angle2 - angle1;
    }
}
