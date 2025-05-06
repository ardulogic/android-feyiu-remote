package com.feyiuremote.libs.Feiyu.calibration.commands;

import android.util.Log;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.ui.gimbal.GimbalEmulator;

abstract public class GimbalCommand {

    private static final String TAG = GimbalCommand.class.getSimpleName();
    ;
    protected final BluetoothLeService mBt;
    public Long timeExecuted;
    public float panAngleAtExecution;
    public float tiltAngleAtExecution;

    public String comment = "";

    public GimbalCommand(BluetoothLeService bt) {
        this.mBt = bt;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void run() {
        timeExecuted = System.currentTimeMillis();
        panAngleAtExecution = getCurrentPanAngle();
        tiltAngleAtExecution = getCurrentTiltAngle();

        if (GimbalEmulator.isEnabled) {
            executeEmulated();
        } else {
            execute();
        }

        FeyiuState.getInstance().last_command = System.currentTimeMillis();

    }

    abstract void execute();

    public void executeEmulated() {
        this.timeExecuted = System.currentTimeMillis();
    }

    public boolean hasExecuted() {
        return this.timeExecuted != null;
    }

    public long getTimeSinceExecution() {
        return System.currentTimeMillis() - timeExecuted;
    }

    public float getCurrentPanAngle() {
        return FeyiuState.getInstance().angle_pan.value();
    }

    public float getCurrentTiltAngle() {
        return FeyiuState.getInstance().angle_tilt.value();
    }

    public void log() {
        Log.d(TAG, "Gimbal Command Execution Timestamp: " + timeExecuted + " ms. Comment: " + comment);
    }

}
