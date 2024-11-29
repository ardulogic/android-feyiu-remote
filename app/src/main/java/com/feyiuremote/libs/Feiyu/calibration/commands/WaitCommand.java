package com.feyiuremote.libs.Feiyu.calibration.commands;

import android.util.Log;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;

public class WaitCommand extends GimbalCommand {

    private static final String TAG = "WaitCommand";

    public WaitCommand(BluetoothLeService bt) {
        super(bt);
    }

    @Override
    void execute() {

    }

    @Override
    public void log() {
        Log.d(TAG, "Waiting... (nothing is being sent");
    }
}
