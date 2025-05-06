package com.feyiuremote.libs.Feiyu.queue.commands;

import com.feyiuremote.libs.Feiyu.Axes;

public class JoyCommandLooselyTimed extends JoyCommand {

    public JoyCommandLooselyTimed(Axes.Axis axis, int value, int durationMs) {
        super(axis, value, durationMs);
    }
}
