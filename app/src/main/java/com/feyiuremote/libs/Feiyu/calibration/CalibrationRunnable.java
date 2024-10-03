package com.feyiuremote.libs.Feiyu.calibration;

import android.content.ContentValues;
import android.util.Log;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.FeyiuUtils;
import com.feyiuremote.libs.Feiyu.calibration.commands.FinaliseCommand;
import com.feyiuremote.libs.Feiyu.calibration.commands.GimbalCommand;
import com.feyiuremote.libs.Feiyu.calibration.commands.LogCommand;
import com.feyiuremote.libs.Feiyu.calibration.commands.MoveLockedCommand;
import com.feyiuremote.libs.Feiyu.calibration.commands.StartCommand;
import com.feyiuremote.libs.Feiyu.calibration.commands.StopCommand;

import java.text.DecimalFormat;
import java.util.LinkedList;

public class CalibrationRunnable {

    private String name;
    private final int mJoySens;
    private final int mJoyVal;
    private final BluetoothLeService mBt;
    private final ICalibrationListener mListener;

    private LinkedList<GimbalCommand> commandQueue;
    private int commandIndex = 0;

    private boolean isActive = false;

    public CalibrationRunnable(String name, int joy_sens, int joy_val, BluetoothLeService bt, ICalibrationListener listener) {
        this.name = name;
        this.mJoySens = joy_sens;
        this.mJoyVal = joy_val;
        this.mBt = bt;
        this.mListener = listener;
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
    }

    public void start() {
        buildCalibrationQueue();
        setSensitivity();

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        this.isActive = true;
    }

    public void onGimbalUpdate() {
        if (isActive) {
            if (commandIndex < commandQueue.size()) {
                GimbalCommand command = this.commandQueue.get(commandIndex);
                command.run();
                commandIndex++;
            } else {
                finaliseCalibration();
                isActive = false;
            }
        }
    }

    private void finaliseCalibration() {
        int START = 0;
        int ACCELERATED = 1;
        int STOPPING = 2;
        int STOPPED = 3;

        Float[] pan_angle = new Float[4];
        Float[] tilt_angle = new Float[4];
        Long[] time = new Long[4];

        int move_commands = 0;
        for (GimbalCommand command : commandQueue) {
            if (command instanceof StartCommand) {
                pan_angle[START] = command.pan_angle;
                tilt_angle[START] = command.tilt_angle;
                time[START] = command.actual_execution_time;
            } else if (command instanceof MoveLockedCommand) {
                move_commands++;
                if (move_commands == 5) {
                    pan_angle[ACCELERATED] = command.pan_angle;
                    tilt_angle[ACCELERATED] = command.tilt_angle;
                    time[ACCELERATED] = command.actual_execution_time;
                }
            } else if (command instanceof StopCommand) {
                pan_angle[STOPPING] = command.pan_angle;
                tilt_angle[STOPPING] = command.tilt_angle;
                time[STOPPING] = command.actual_execution_time;
            } else if (command instanceof FinaliseCommand) {
                pan_angle[STOPPED] = command.pan_angle;
                tilt_angle[STOPPED] = command.tilt_angle;
                time[STOPPED] = command.actual_execution_time;
            }
        }

        Log.d("Calibration", pan_angle[0].toString() + pan_angle[1].toString() + pan_angle[2].toString());

        DecimalFormat df = new DecimalFormat("#.###");

        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("joy_sens", mJoySens);
        cv.put("joy_val", mJoyVal);

        // It takes time to accelerate to constant speed
        // Goal will be to choose speed so there's enough room to accelerate and calculate stopping
        // points based on the overshoot from stable speed
        cv.put("pan_angle_diff", df.format(angleDifference(pan_angle[ACCELERATED], pan_angle[STOPPING]))); // Deg
        cv.put("pan_angle_overshoot", df.format(angleDifference(pan_angle[STOPPING], pan_angle[STOPPED]))); // Deg
        cv.put("pan_speed", df.format(calculateSpeed(cv.getAsDouble("pan_angle_diff"), time[STOPPING], time[ACCELERATED]))); // Deg/s

        cv.put("tilt_angle_diff", df.format(angleDifference(tilt_angle[ACCELERATED], tilt_angle[STOPPING]))); // Deg
        cv.put("tilt_angle_overshoot", df.format(angleDifference(tilt_angle[STOPPING], tilt_angle[STOPPED]))); // Deg
        cv.put("tilt_speed", df.format(calculateSpeed(cv.getAsDouble("tilt_angle_diff"), time[STOPPING], time[ACCELERATED]))); // Deg/s

        cv.put("time_ms", Math.abs(time[STOPPING] - time[START]));

        mListener.onCalFinished(cv);
    }

    private double calculateSpeed(double angle_diff, long time_end_ms, long time_start_ms) {
        return angle_diff / (time_end_ms - time_start_ms) * 1000;
    }

    public void buildCalibrationQueue() {
        this.commandQueue = new LinkedList<>();

        commandQueue.add(new StartCommand(mBt, mJoyVal));

        for (int i = 0; i < 12; i++) {
            commandQueue.add(new MoveLockedCommand(mBt, mJoyVal));
        }

        commandQueue.add(new StopCommand(mBt));

        for (int i = 0; i < 5; i++) {
            commandQueue.add(new LogCommand(mBt));
        }

        commandQueue.add(new FinaliseCommand(mBt));
    }

    public static double angleDifference(double angle1, double angle2) {
        return angle2 - angle1;
    }
}
