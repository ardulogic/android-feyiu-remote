package com.feyiuremote.libs.Feiyu.calibration.commands;

import android.util.Log;

import androidx.annotation.NonNull;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.FeyiuUtils;
import com.feyiuremote.libs.Feiyu.queue.FeyiuCommandQueue;
import com.feyiuremote.libs.Feiyu.queue.commands.JoyCommand;
import com.feyiuremote.ui.gimbal.GimbalEmulator;

public class MoveCommand extends GimbalCommand {

    private static final String TAG = MoveCommand.class.getSimpleName();
    public int tiltJoy;
    public int panJoy;

    public MoveCommand(BluetoothLeService mBt, int panJoy, int tiltJoy) {
        super(mBt);

        this.panJoy = panJoy;
        this.tiltJoy = tiltJoy;
    }

    public MoveCommand(BluetoothLeService mBt, int panJoy, int tiltJoy, String comment) {
        this(mBt, panJoy, tiltJoy);

        this.comment = comment;
    }

    public MoveCommand(BluetoothLeService mBt, JoyCommand cmd) {
        this(mBt, cmd.axis == FeyiuCommandQueue.Axis.PAN ? cmd.value : 0, cmd.axis == FeyiuCommandQueue.Axis.TILT ? cmd.value : 0);
    }

    public int getJoyValue(FeyiuCommandQueue.Axis axis) {
        if (axis == FeyiuCommandQueue.Axis.PAN) {
            return panJoy;
        } else {
            return tiltJoy;
        }
    }

    @Override
    void execute() {
        mBt.send(FeyiuUtils.SERVICE_ID, FeyiuUtils.CONTROL_CHARACTERISTIC_ID,
                FeyiuUtils.move(panJoy, tiltJoy)
        );

        FeyiuState.joy_val_pan = panJoy;
        FeyiuState.joy_val_tilt = tiltJoy;
    }

    @Override
    public void executeEmulated() {
        super.executeEmulated();

        GimbalEmulator.setPanJoy(panJoy);
        GimbalEmulator.setTiltJoy(tiltJoy);

        FeyiuState.joy_val_pan = panJoy;
        FeyiuState.joy_val_tilt = tiltJoy;
    }

    public void log() {
        Log.d(TAG, "Moving: Pan: " + panJoy + " Tilt: " + tiltJoy + " Comment: " + comment);
    }

    @NonNull
    @Override
    public String toString() {
        return "MoveCommand: Pan: " + panJoy + " Tilt: " + tiltJoy + " Comment: " + comment;
    }

    public void updateFrom(JoyCommand cmd) {
        if (cmd.axis == FeyiuCommandQueue.Axis.PAN) {
            panJoy = cmd.value;
        } else {
            tiltJoy = cmd.value;
        }
    }
}
