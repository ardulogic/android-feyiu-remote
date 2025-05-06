package com.feyiuremote.libs.Feiyu.queue.entries;


import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.Axes;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.controls.commands.GimbalCommand;
import com.feyiuremote.libs.Feiyu.controls.commands.SetJoySensitivityCommand;
import com.feyiuremote.libs.Feiyu.queue.commands.SingleSensitivityCommand;

public class QueueJoySensitivityStateEntry extends QueueEntry {

    public Axes.Axis axis;
    public Integer value;

    public QueueJoySensitivityStateEntry(SingleSensitivityCommand cmd) {
        super(cmd.startTime);

        this.axis = cmd.axis;
        this.value = cmd.value;
    }

    @Override
    public GimbalCommand getCommand(BluetoothLeService btService) {
        return new SetJoySensitivityCommand(btService, axis, value);
    }

    public boolean matchesAxis(SingleSensitivityCommand cmd) {
        return this.axis == cmd.axis;
    }

    public boolean needsToExecute() {
        if (this.axis == Axes.Axis.PAN) {
            return this.value != FeyiuState.joy_sens_pan;
        } else {
            return this.value != FeyiuState.joy_sens_tilt;
        }
    }


}
