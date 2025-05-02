package com.feyiuremote.libs.Feiyu.queue.entries;


import com.feyiuremote.libs.Feiyu.queue.FeyiuCommandQueue;
import com.feyiuremote.libs.Feiyu.queue.commands.JoyCommandStrictlyTimed;

public class QueueJoyStateStrictEntry extends QueueJoyStateEntry {

    public QueueJoyStateStrictEntry(JoyCommandStrictlyTimed cmd) {
        super(cmd.getPanJoy(), cmd.getTiltJoy(), cmd.startTime);

        cmd.updateStartTime(cmd.startTime);
    }

}
