package com.feyiuremote.libs.Feiyu.queue.commands;

import android.util.Log;

import com.feyiuremote.libs.Feiyu.queue.FeyiuCommandQueue;

public class JoyCommandStrictlyTimed extends JoyCommand {
    public final long delayMs;

    public JoyCommandStrictlyTimed(FeyiuCommandQueue.Axis axis, int value, int durationMs, long executeAfterMs) {
        super(axis, value, durationMs, System.currentTimeMillis() + executeAfterMs);

        this.delayMs = executeAfterMs;

        if (delayMs < 0) {
            throw new RuntimeException("Delay cannot be negative!");
        }

        Log.w("JoyStrictCommandCreated", "Executes in:" + executesInMs() + " Params:  " + value + " duration:" + durationMs + " executesAfter:" + executeAfterMs);
    }

    public long executesInMs() {
        return this.startTime - System.currentTimeMillis();
    }

}