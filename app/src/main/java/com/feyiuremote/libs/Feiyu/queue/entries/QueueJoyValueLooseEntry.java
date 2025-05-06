package com.feyiuremote.libs.Feiyu.queue.entries;


import com.feyiuremote.libs.Feiyu.Axes;
import com.feyiuremote.libs.Feiyu.queue.commands.JoyCommand;

public class QueueJoyValueLooseEntry extends QueueJoyValueEntry {


    public QueueJoyValueLooseEntry(JoyCommand cmd, Long timeExecutes) {
        super(cmd.axis == Axes.Axis.PAN ? cmd.value : null, cmd.axis == Axes.Axis.TILT ? cmd.value : null, timeExecutes);
    }

}
