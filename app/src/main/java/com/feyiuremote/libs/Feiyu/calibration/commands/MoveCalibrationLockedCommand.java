package com.feyiuremote.libs.Feiyu.calibration.commands;

import android.util.Log;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.FeyiuUtils;

/**
 * Main thing is not to induce endless update cycle while observing FeyiuState
 * CalibrationMove commands do not change FeyiuState
 */
public class MoveCalibrationLockedCommand extends GimbalCommand {
    private final int joyVal;

    public MoveCalibrationLockedCommand(BluetoothLeService bt, int joy_value) {
        super(bt);

        this.joyVal = joy_value;
    }

    @Override
    void execute() {
        mBt.send(FeyiuUtils.SERVICE_ID, FeyiuUtils.CONTROL_CHARACTERISTIC_ID,
                FeyiuUtils.move(joyVal, joyVal)
        );
    }

    @Override
    public void log() {
        Log.d("MoveLockedCommand", "Locked axis at: " + this.joyVal + " Comment:" + comment);
    }
}
