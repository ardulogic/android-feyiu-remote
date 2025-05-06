package com.feyiuremote.libs.Feiyu.controls.commands;

import androidx.annotation.NonNull;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.Axes;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.FeyiuUtils;
import com.feyiuremote.ui.gimbal.GimbalEmulator;

public class SetJoySensitivityCommand extends GimbalCommand {

    private static final String TAG = SetJoySensitivityCommand.class.getSimpleName();
    private final Axes.Axis axis;
    private final Integer value;

    public SetJoySensitivityCommand(BluetoothLeService bt, Axes.Axis axis, Integer sensitivityValue) {
        super(bt);

        this.axis = axis;
        this.value = sensitivityValue;
    }

    @Override
    void execute() {
        String command = "";

        if (axis == Axes.Axis.PAN) {
            command = FeyiuUtils.setPanSensitivity(value);
        } else {
            command = FeyiuUtils.setTiltSensitivity(value);
        }

        mBt.send(FeyiuUtils.SERVICE_ID, FeyiuUtils.CONTROL_CHARACTERISTIC_ID, command);

        if (axis == Axes.Axis.PAN) {
            FeyiuState.joy_sens_pan = value;
        } else {
            FeyiuState.joy_sens_tilt = value;
        }
    }

    @Override
    public void executeEmulated() {
        super.executeEmulated();

        GimbalEmulator.setJoySensitivity(axis, value);

        if (axis == Axes.Axis.PAN) {
            FeyiuState.joy_sens_pan = value;
        } else {
            FeyiuState.joy_sens_tilt = value;
        }
    }

    @NonNull
    @Override
    public String toString() {
        String axisStr = axis == Axes.Axis.PAN ? "Pan" : "Tilt";
        return "SetSensitivityCommand: Axis: " + axisStr + " Value: " + value + " Comment: " + comment;
    }
}
