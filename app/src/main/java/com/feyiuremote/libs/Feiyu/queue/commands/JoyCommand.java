package com.feyiuremote.libs.Feiyu.queue.commands;

import com.feyiuremote.libs.Feiyu.queue.FeyiuCommandQueue;

public abstract class JoyCommand {

    public final FeyiuCommandQueue.Axis axis;

    public final int value;
    public final int durationMs;
    public long endTime;
    public long startTime;

    protected JoyCommand(FeyiuCommandQueue.Axis axis, int value, int durationMs) {
        if (axis == null) throw new NullPointerException("axis");
        this.axis = axis;
        this.value = value;
        this.durationMs = durationMs;
        this.startTime = System.currentTimeMillis();
        this.endTime = startTime + durationMs;
    }

    protected JoyCommand(FeyiuCommandQueue.Axis axis, int value, int durationMs, long startTime) {
        if (axis == null) throw new NullPointerException("axis");
        this.axis = axis;
        this.value = value;
        this.durationMs = durationMs;
        this.startTime = startTime;
        this.endTime = startTime + durationMs;
    }

    public void updateStartTime(Long time) {
        this.startTime = time;
        this.endTime = this.startTime + this.durationMs;
    }

    public Integer getPanJoy() {
        if (this.axis == FeyiuCommandQueue.Axis.PAN) return this.value;

        return null;
    }

    public Integer getTiltJoy() {
        if (this.axis == FeyiuCommandQueue.Axis.TILT) return this.value;

        return null;
    }

    public FeyiuCommandQueue.Axis getOppositeAxis() {
        return axis == FeyiuCommandQueue.Axis.TILT ? FeyiuCommandQueue.Axis.PAN : FeyiuCommandQueue.Axis.TILT;
    }


}