package com.feyiuremote.libs.Feiyu.calibration.commands;

import android.util.Log;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.FeyiuUtils;

public class StopCommand extends GimbalCommand {

    private static final String TAG = "Stop Command";

    public StopCommand(BluetoothLeService bt) {
        super(bt);
    }

    @Override
    void execute() {
        mBt.send(FeyiuUtils.SERVICE_ID, FeyiuUtils.CONTROL_CHARACTERISTIC_ID,
                FeyiuUtils.move(0, 0)
        );
    }

    @Override
    public void log() {
        Log.d(TAG, "Stop Command (move 0,0)");
    }
}
