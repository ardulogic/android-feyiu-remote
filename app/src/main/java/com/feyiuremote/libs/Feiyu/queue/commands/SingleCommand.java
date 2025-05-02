package com.feyiuremote.libs.Feiyu.queue.commands;


public abstract class SingleCommand {

    public long startTime;

    protected SingleCommand() {
        this.startTime = System.currentTimeMillis();
    }

    protected SingleCommand(Long startTime) {
        this.startTime = startTime;
    }


    public void updateStartTime(Long time) {
        this.startTime = time;
    }

}