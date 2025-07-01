package com.feyiuremote.libs.Feiyu.calibration.commands;

import android.util.Log;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.FeyiuUtils;
import com.feyiuremote.ui.connectivity.GimbalEmulator;

public class SetPanSensitivityCommand extends GimbalCommand {

    private static final String TAG = SetPanSensitivityCommand.class.getSimpleName();
    private int pan_sens;

    public SetPanSensitivityCommand(BluetoothLeService bt, Integer pan_sens) {
        super(bt);

        this.pan_sens = pan_sens;
    }

    @Override
    void execute() {
        Log.d(TAG, "Seting Pan Sensitivity: " + pan_sens + " Comment: " + comment);
        mBt.send(FeyiuUtils.SERVICE_ID, FeyiuUtils.CONTROL_CHARACTERISTIC_ID, FeyiuUtils.setPanSensitivity(pan_sens));
        FeyiuState.joy_sens_pan = pan_sens;
    }

    @Override
    public void executeEmulated() {
        super.executeEmulated();

        GimbalEmulator.setPanSensitivity(pan_sens);
        FeyiuState.joy_sens_pan = pan_sens;
    }
}
