package com.feyiuremote.libs.Feiyu.queue.entries;


import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.controls.commands.GimbalCommand;
import com.feyiuremote.libs.Feiyu.controls.commands.MoveCommand;
import com.feyiuremote.libs.Feiyu.queue.FeyiuCommandQueue;
import com.feyiuremote.libs.Feiyu.queue.commands.JoyCommand;

public abstract class QueueJoyStateEntry extends QueueEntry {

    public Integer panJoyValue;
    public Integer tiltJoyValue;

    public boolean isMerged = false;
    public boolean isMergedWithPrevious = false;

    public QueueJoyStateEntry(Integer panJoy, Integer tiltJoy, Long timeExecutes) {
        super(timeExecutes);

        panJoyValue = panJoy;
        tiltJoyValue = tiltJoy;
    }

    @Override
    public GimbalCommand getCommand(BluetoothLeService btService) {
        int panJoy = panJoyValue == null ? 0 : panJoyValue;
        int tiltJoy = tiltJoyValue == null ? 0 : tiltJoyValue;

        return new MoveCommand(btService, panJoy, tiltJoy);
    }


    public void mergeWith(QueueJoyStateEntry entry) {
        if (entry.panJoyValue != null) {
            panJoyValue = entry.panJoyValue;
        }

        if (entry.tiltJoyValue != null) {
            tiltJoyValue = entry.tiltJoyValue;
        }

        this.isMerged = true;
    }

    public void copyInitialValuesFrom(QueueJoyStateEntry entry) {
        if (panJoyValue == null) {
            panJoyValue = entry.panJoyValue;
        }

        if (tiltJoyValue == null) {
            tiltJoyValue = entry.tiltJoyValue;
        }

        this.isMergedWithPrevious = true;
    }


    public void overwriteWith(JoyCommand cmd) {
        overwriteWith(cmd.axis, cmd.value);
    }

    public void overwriteWith(FeyiuCommandQueue.Axis axis, Integer value) {
        if (axis == FeyiuCommandQueue.Axis.PAN) {
            this.panJoyValue = value;
        }

        if (axis == FeyiuCommandQueue.Axis.TILT) {
            this.tiltJoyValue = value;
        }
    }

    public boolean axisHasNoValue(FeyiuCommandQueue.Axis axis) {
        if (axis == FeyiuCommandQueue.Axis.PAN) {
            return this.panJoyValue == null;
        } else {
            return this.tiltJoyValue == null;
        }
    }
}
