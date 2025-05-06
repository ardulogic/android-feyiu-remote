package com.feyiuremote.libs.Feiyu.queue.commands;

import com.feyiuremote.libs.Feiyu.Axes;

public class SingleSensitivityCommand extends SingleCommand {

    public Axes.Axis axis;
    public Integer value;

    public SingleSensitivityCommand(Axes.Axis axis, Integer value) {
        super();

        this.axis = axis;
        this.value = value;
    }

    protected SingleSensitivityCommand(Long startTime) {
        super(startTime);
    }


}