package com.feyiuremote.libs.Feiyu.queue.entries;


import com.feyiuremote.libs.Feiyu.queue.FeyiuCommandQueue;
import com.feyiuremote.libs.Feiyu.queue.commands.JoyCommand;

public class QueueJoyStateLooseEntry extends QueueJoyStateEntry {


    public QueueJoyStateLooseEntry(JoyCommand cmd, Long timeExecutes) {
        super(cmd.axis == FeyiuCommandQueue.Axis.PAN ? cmd.value : null, cmd.axis == FeyiuCommandQueue.Axis.TILT ? cmd.value : null, timeExecutes);
    }

}
