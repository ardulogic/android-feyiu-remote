package com.feyiuremote.libs.Feiyu.queue.commands;

import com.feyiuremote.libs.Feiyu.queue.FeyiuCommandQueue;

public class SingleSensitivityCommand extends SingleCommand {

    public FeyiuCommandQueue.Axis axis;
    public Integer value;

    public SingleSensitivityCommand(FeyiuCommandQueue.Axis axis, Integer value) {
        super();

        this.axis = axis;
        this.value = value;
    }

    protected SingleSensitivityCommand(Long startTime) {
        super(startTime);
    }


}