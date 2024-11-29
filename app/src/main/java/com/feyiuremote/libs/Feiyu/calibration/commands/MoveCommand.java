package com.feyiuremote.libs.Feiyu.calibration.commands;

import android.util.Log;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.FeyiuUtils;

public class MoveCommand extends GimbalCommand {

    private static final String TAG = MoveCommand.class.getSimpleName();
    private int tilt;
    private int pan;

    public MoveCommand(BluetoothLeService bt, int joy_pan_value, int joy_tilt_value) {
        super(bt);

        this.pan = joy_pan_value;
        this.tilt = joy_tilt_value;
    }

    public MoveCommand(BluetoothLeService mBt, int panJoy, int tiltJoy, String currentComment) {
        super(mBt);

        this.pan = panJoy;
        this.tilt = tiltJoy;

        this.comment = currentComment;
    }

    @Override
    void execute() {
        mBt.send(FeyiuUtils.SERVICE_ID, FeyiuUtils.CONTROL_CHARACTERISTIC_ID,
                FeyiuUtils.move(pan, tilt)
        );

        FeyiuState.joy_val_pan = pan;
        FeyiuState.joy_val_tilt = tilt;
    }

    public void log() {
        Log.d(TAG, "Moving: Pan: " + pan + " Tilt: " + tilt + " Comment: " + comment);
    }

    public void setPanAngle(int pan) {
        this.pan = pan;
    }

    public void setTiltAngle(int tilt) {
        this.tilt = tilt;
    }


}
