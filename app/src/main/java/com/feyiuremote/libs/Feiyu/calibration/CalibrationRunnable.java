package com.feyiuremote.libs.Feiyu.calibration;

import android.content.ContentValues;
import android.util.Log;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.FeyiuUtils;
import com.feyiuremote.libs.Feiyu.calibration.commands.FinaliseCommand;
import com.feyiuremote.libs.Feiyu.calibration.commands.GimbalCommand;
import com.feyiuremote.libs.Feiyu.calibration.commands.MoveCalibrationCommand;
import com.feyiuremote.libs.Feiyu.calibration.commands.MoveCalibrationLockedCommand;
import com.feyiuremote.libs.Feiyu.calibration.commands.StartCommand;
import com.feyiuremote.libs.Feiyu.calibration.commands.StopCommand;

import java.text.DecimalFormat;
import java.util.LinkedList;

public class CalibrationRunnable {

    private final int mJoySens;
    private final int mJoyVal;
    private final BluetoothLeService mBt;
    private final ICalibrationListener mListener;
    private String type = "default"; // Calibration mode

    private LinkedList<GimbalCommand> commandQueue;
    private int commandIndex = 0;

    private boolean isActive = false;

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

    public synchronized void onGimbalUpdate() {
        if (isActive) {
            if (commandIndex < commandQueue.size()) {
                GimbalCommand command = this.commandQueue.get(commandIndex);
                command.setComment(this.type);
                command.run();
                commandIndex++;
            } else {
                isActive = false;
                finaliseCalibration();
            }
        }
    }

    private synchronized void finaliseCalibration() {
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
            } else if ((command instanceof MoveCalibrationLockedCommand) || (command instanceof MoveCalibrationCommand)) {
                move_commands++;
                if (move_commands == 6) {
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

    public static double angleDifference(double angle1, double angle2) {
        return angle2 - angle1;
    }
}
