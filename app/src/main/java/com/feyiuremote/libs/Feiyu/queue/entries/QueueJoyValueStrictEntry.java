package com.feyiuremote.libs.Feiyu.queue.entries;


import com.feyiuremote.libs.Feiyu.queue.commands.JoyCommandStrictlyTimed;

public class QueueJoyValueStrictEntry extends QueueJoyValueEntry {

    public QueueJoyValueStrictEntry(JoyCommandStrictlyTimed cmd) {
        super(cmd.getPanJoy(), cmd.getTiltJoy(), cmd.startTime);

        cmd.updateStartTime(cmd.startTime);
    }

}
