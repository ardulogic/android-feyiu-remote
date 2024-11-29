package com.feyiuremote.libs.Feiyu.calibration.commands;

import android.util.Log;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.FeyiuUtils;

/**
 * Main thing is not to induce endless update cycle while observing FeyiuState
 * CalibrationMove commands do not change FeyiuState
 */
public class MoveCalibrationCommand extends GimbalCommand {

    private static final String TAG = MoveCalibrationCommand.class.getSimpleName();
    private int tilt;
    private int pan;

    public MoveCalibrationCommand(BluetoothLeService bt, int joy_pan_value, int joy_tilt_value) {
        super(bt);

        this.pan = joy_pan_value;
        this.tilt = joy_tilt_value;
    }

    public MoveCalibrationCommand(BluetoothLeService mBt, int panJoy, int tiltJoy, String currentComment) {
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
    }

    public void log() {
        Log.d(TAG, "Moving (no state update): Pan: " + pan + " Tilt: " + tilt + " Comment: " + comment);
    }

}
