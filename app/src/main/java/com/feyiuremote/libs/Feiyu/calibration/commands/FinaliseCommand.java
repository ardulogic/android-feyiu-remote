package com.feyiuremote.libs.Feiyu.calibration.commands;

import android.util.Log;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;

public class FinaliseCommand extends GimbalCommand {

    private static final String TAG = "FinaliseCommand";

    public FinaliseCommand(BluetoothLeService bt) {
        super(bt);
    }

    @Override
    void execute() {
//        mBt.send(FeyiuUtils.SERVICE_ID, FeyiuUtils.CONTROL_CHARACTERISTIC_ID,
//                FeyiuUtils.move(0, 0)
//        );
    }

    @Override
    public void log() {
        Log.d(TAG, "Finalise Command (Nothing is being sent.)");
    }
}
