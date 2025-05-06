package com.feyiuremote.libs.Feiyu.queue.commands;


import com.feyiuremote.libs.Feiyu.Axes;

public class JoyCommandStrictlyTimed extends JoyCommand {
    public final long delayMs;

    public JoyCommandStrictlyTimed(Axes.Axis axis, int value, int durationMs, long executeAfterMs) {
        super(axis, value, durationMs, System.currentTimeMillis() + executeAfterMs);

        this.delayMs = executeAfterMs;

        if (delayMs < 0) {
            throw new RuntimeException("Delay cannot be negative!");
        }
    }

    public long executesInMs() {
        return this.startTime - System.currentTimeMillis();
    }

}