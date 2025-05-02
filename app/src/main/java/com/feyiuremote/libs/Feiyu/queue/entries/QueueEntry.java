package com.feyiuremote.libs.Feiyu.queue.entries;


import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.controls.commands.GimbalCommand;

public abstract class QueueEntry {


    public Long timeExecutes;

    public QueueEntry(Long timeExecutes) {
        this.timeExecutes = timeExecutes;
    }

    public void updateTimestamp(Long timeExecutes) {
        this.timeExecutes = timeExecutes;
    }

    abstract public GimbalCommand getCommand(BluetoothLeService btService);

    public Long executesAt() {
        return this.timeExecutes;
    }

    public Long executesIn() {
        return this.timeExecutes - System.currentTimeMillis();
    }

}
