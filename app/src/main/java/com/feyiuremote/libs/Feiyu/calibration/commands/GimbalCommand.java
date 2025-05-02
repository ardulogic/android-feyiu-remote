package com.feyiuremote.libs.Feiyu.calibration.commands;

import android.util.Log;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.ui.gimbal.GimbalEmulator;

abstract public class GimbalCommand {

    private static final String TAG = GimbalCommand.class.getSimpleName();
    ;
    protected final BluetoothLeService mBt;
    public Long actual_execution_time;
    public float current_pan_angle;
    public float current_tilt_angle;
    public String comment = "";

    public GimbalCommand(BluetoothLeService bt) {
        this.mBt = bt;
    }

    public void run() {
        actual_execution_time = System.currentTimeMillis();
        current_pan_angle = getCurrentPanAngle();
        current_tilt_angle = getCurrentTiltAngle();

        if (GimbalEmulator.isEnabled) {
            executeEmulated();
        } else {
            execute();
        }

        FeyiuState.getInstance().last_command = System.currentTimeMillis();

    }


    public float getCurrentPanAngle() {
        return FeyiuState.getInstance().angle_pan.value();
    }

    public float getCurrentTiltAngle() {
        return FeyiuState.getInstance().angle_tilt.value();
    }

    public void log() {
        Log.d(TAG, "Execution Time: " + actual_execution_time + " ms, Pan Angle/Value: " + getCurrentPanAngle() + ", Tilt Angle: " + getCurrentTiltAngle());
    }

    abstract void execute();

    public long getTimeSinceExcecution() {
        return System.currentTimeMillis() - actual_execution_time;
    }

    public boolean hasExecuted() {
        return this.actual_execution_time != null;
    }

    public void executeEmulated() {
        this.actual_execution_time = System.currentTimeMillis();
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
