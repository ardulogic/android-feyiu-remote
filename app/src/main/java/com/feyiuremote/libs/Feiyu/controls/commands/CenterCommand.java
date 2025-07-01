package com.feyiuremote.libs.Feiyu.controls.commands;

import android.util.Log;

import androidx.annotation.NonNull;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.FeyiuUtils;
import com.feyiuremote.ui.connectivity.GimbalEmulator;

public class CenterCommand extends GimbalCommand {

    private static final String TAG = CenterCommand.class.getSimpleName();

    public CenterCommand(BluetoothLeService mBt) {
        super(mBt);
    }

    @Override
    void execute() {
        mBt.send(FeyiuUtils.SERVICE_ID, FeyiuUtils.CONTROL_CHARACTERISTIC_ID,
                FeyiuUtils.center()
        );
    }

    @Override
    public void executeEmulated() {
        super.executeEmulated();

        GimbalEmulator.setPanJoy(0);
        GimbalEmulator.setTiltJoy(0);

        FeyiuState.getInstance().angle_pan.update(0);
        FeyiuState.getInstance().angle_tilt.update(0);
        FeyiuState.getInstance().angle_yaw.update(0);
    }

    public void log() {
        Log.d(TAG, "CenterCommand: Comment: " + comment);
    }

    @NonNull
    @Override
    public String toString() {
        return "CenterCommand: Comment: " + comment;
    }

}
