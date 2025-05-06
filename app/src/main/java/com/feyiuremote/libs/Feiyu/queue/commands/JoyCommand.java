package com.feyiuremote.libs.Feiyu.queue.commands;

import com.feyiuremote.libs.Feiyu.Axes;

public abstract class JoyCommand {

    public final Axes.Axis axis;

    public final int value;
    public final int durationMs;
    public long endTime;
    public long startTime;

    protected JoyCommand(Axes.Axis axis, int value, int durationMs) {
        if (axis == null) throw new NullPointerException("axis");
        this.axis = axis;
        this.value = value;
        this.durationMs = durationMs;
        this.startTime = System.currentTimeMillis();
        this.endTime = startTime + durationMs;
    }

    protected JoyCommand(Axes.Axis axis, int value, int durationMs, long startTime) {
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
        if (this.axis == Axes.Axis.PAN) return this.value;

        return null;
    }

    public Integer getTiltJoy() {
        if (this.axis == Axes.Axis.TILT) return this.value;

        return null;
    }

    public Axes.Axis getOppositeAxis() {
        return axis == Axes.Axis.TILT ? Axes.Axis.PAN : Axes.Axis.TILT;
    }


}