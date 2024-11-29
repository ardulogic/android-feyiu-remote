package com.feyiuremote.libs.Feiyu.calibration.commands;

import android.util.Log;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.FeyiuState;

abstract public class GimbalCommand {

    private static final String TAG = GimbalCommand.class.getSimpleName();
    ;
    protected final BluetoothLeService mBt;
    public Long actual_execution_time;

    public float pan_angle;
    public float tilt_angle;

    private Long delayed_execution_time;
    public String comment = "";

    public GimbalCommand(BluetoothLeService bt) {
        this.mBt = bt;
    }

    public void run() {
        actual_execution_time = System.currentTimeMillis();
        pan_angle = getCurrentPanAngle();
        tilt_angle = getCurrentTiltAngle();
        execute();
        log();
    }

    public float getCurrentPanAngle() {
        return FeyiuState.getInstance().angle_pan.value();
    }

    public float getCurrentTiltAngle() {
        return FeyiuState.getInstance().angle_tilt.value();
    }

    public void log() {
        Log.d(TAG, "Execution Time: " + actual_execution_time + " ms, Pan Angle/Value: " + getCurrentPanAngle() + ", Tilt Angle: " + getCurrentTiltAngle());
    }


    public void executeAfter(Long time) {
        this.delayed_execution_time = time + System.currentTimeMillis();
    }

    abstract void execute();

    public boolean delayedExecutionTimeIsSet() {
        return this.delayed_execution_time != null;
    }

    public long timeAfterActualExecution() {
        return System.currentTimeMillis() - actual_execution_time;
    }

    public boolean hasExecuted() {
        return this.actual_execution_time != null;
    }

    public Long delayedExecutionTime() {
        return this.delayed_execution_time;
    }

    public Long timeLeftToDelayedExecution() {
        long diff = this.delayed_execution_time - System.currentTimeMillis();

        if (diff < 0) {
            Log.e(TAG, "Not in time for delayed execution:" + diff + "ms lag");
            return 0L;
        }

        return diff;
    }

    public boolean canBeImmediatelyExecuted() {
        return !hasExecuted() && !delayedExecutionTimeIsSet();
    }

    public boolean canBeExecutedAfterDelay() {
        return !hasExecuted() && delayedExecutionTimeIsSet();
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
