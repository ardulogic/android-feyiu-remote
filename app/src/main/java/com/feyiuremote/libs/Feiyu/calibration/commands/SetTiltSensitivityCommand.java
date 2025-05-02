package com.feyiuremote.libs.Feiyu.calibration.commands;

import android.util.Log;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.FeyiuUtils;
import com.feyiuremote.ui.gimbal.GimbalEmulator;

public class SetTiltSensitivityCommand extends GimbalCommand {

    private static final String TAG = SetTiltSensitivityCommand.class.getSimpleName();
    private int tilt_sens;

    public SetTiltSensitivityCommand(BluetoothLeService bt, Integer tilt_sens) {
        super(bt);

        this.tilt_sens = tilt_sens;
    }

    @Override
    void execute() {
        if (FeyiuState.joy_val_tilt != tilt_sens) {
            Log.d(TAG, "Seting Tilt Sensitivity: " + tilt_sens + " Comment: " + comment);
            mBt.send(FeyiuUtils.SERVICE_ID, FeyiuUtils.CONTROL_CHARACTERISTIC_ID, FeyiuUtils.setTiltSensitivity(tilt_sens));
            FeyiuState.joy_sens_tilt = tilt_sens;
        } else {
            Log.d(TAG, "Setting Tilt Sensitivity: " + tilt_sens + " (Skipped) Comment: " + comment);
        }
    }

    @Override
    public void executeEmulated() {
        super.executeEmulated();

        GimbalEmulator.setTiltSensitivity(tilt_sens);
        FeyiuState.joy_sens_tilt = tilt_sens;
    }
}
