package com.feyiuremote.libs.Feiyu.queue.commands;

import com.feyiuremote.libs.Feiyu.queue.FeyiuCommandQueue;

public class JoyCommandLooselyTimed extends JoyCommand {

    public JoyCommandLooselyTimed(FeyiuCommandQueue.Axis axis, int value, int durationMs) {
        super(axis, value, durationMs);
    }
}
