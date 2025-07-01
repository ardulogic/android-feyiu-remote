package com.feyiuremote.libs.Feiyu.controls.commands;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.ui.connectivity.GimbalEmulator;

abstract public class GimbalCommand {

    private static final String TAG = GimbalCommand.class.getSimpleName();
    ;
    protected final BluetoothLeService mBt;
    public Long timeExecuted;
    public String comment = "";

    public GimbalCommand(BluetoothLeService bt) {
        this.mBt = bt;
    }

    public void run() {
        if (GimbalEmulator.isEnabled) {
            executeEmulated();
        } else {
            if (mBt != null) {
                execute();
            }
        }

        FeyiuState.getInstance().last_command = System.currentTimeMillis();

    }

    abstract void execute();

    public boolean hasExecuted() {
        return this.timeExecuted != null;
    }

    public Long getTimeSinceExcecution() {
        if (hasExecuted()) {
            return System.currentTimeMillis() - timeExecuted;
        }

        return null;
    }

    public void executeEmulated() {
        this.timeExecuted = System.currentTimeMillis();
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
