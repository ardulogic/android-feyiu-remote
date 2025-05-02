package com.feyiuremote.libs.Feiyu.controls.commands;

import androidx.annotation.NonNull;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.FeyiuUtils;
import com.feyiuremote.libs.Feiyu.queue.FeyiuCommandQueue;
import com.feyiuremote.ui.gimbal.GimbalEmulator;

public class SetJoySensitivityCommand extends GimbalCommand {

    private static final String TAG = SetJoySensitivityCommand.class.getSimpleName();
    private final FeyiuCommandQueue.Axis axis;
    private final Integer value;

    public SetJoySensitivityCommand(BluetoothLeService bt, FeyiuCommandQueue.Axis axis, Integer sensitivityValue) {
        super(bt);

        this.axis = axis;
        this.value = sensitivityValue;
    }

    @Override
    void execute() {
        String command = "";

        if (axis == FeyiuCommandQueue.Axis.PAN) {
            command = FeyiuUtils.setPanSensitivity(value);
        } else {
            command = FeyiuUtils.setTiltSensitivity(value);
        }

        mBt.send(FeyiuUtils.SERVICE_ID, FeyiuUtils.CONTROL_CHARACTERISTIC_ID, command);

        if (axis == FeyiuCommandQueue.Axis.PAN) {
            FeyiuState.joy_sens_pan = value;
        } else {
            FeyiuState.joy_sens_tilt = value;
        }
    }

    @Override
    public void executeEmulated() {
        super.executeEmulated();

        GimbalEmulator.setJoySensitivity(axis, value);

        if (axis == FeyiuCommandQueue.Axis.PAN) {
            FeyiuState.joy_sens_pan = value;
        } else {
            FeyiuState.joy_sens_tilt = value;
        }
    }

    @NonNull
    @Override
    public String toString() {
        String axisStr = axis == FeyiuCommandQueue.Axis.PAN ? "Pan" : "Tilt";
        return "SetSensitivityCommand: Axis: " + axisStr + " Value: " + value + " Comment: " + comment;
    }
}
