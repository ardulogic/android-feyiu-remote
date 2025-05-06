package com.feyiuremote.libs.Feiyu.calibration.commands;

import android.util.Log;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.FeyiuUtils;

public class StartCommand extends GimbalCommand {


    private static final String TAG = "StartCommand";
    private final int joyPan;
    private final int joyTilt;

    public StartCommand(BluetoothLeService bt, int joyPan, int joyTilt) {
        super(bt);

        this.joyPan = joyPan;
        this.joyTilt = joyTilt;
    }

    @Override
    void execute() {
        mBt.send(FeyiuUtils.SERVICE_ID, FeyiuUtils.CONTROL_CHARACTERISTIC_ID,
                FeyiuUtils.move(joyPan, joyTilt)
        );
    }

    @Override
    public void log() {
        Log.d(TAG, "Start command initiated, joyPan:" + joyPan + " joy TIlt:" + joyTilt);
    }
}
