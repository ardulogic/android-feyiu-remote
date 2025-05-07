package com.feyiuremote.ui.camera.fragments.tracker;

public class DetectorOption {
    public final int id;
    public final String name;
    private final String processorClass;

    public DetectorOption(int id, String name, String className) {
        this.id = id;
        this.name = name;
        this.processorClass = className;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getFrameProcessorClass() {
        return processorClass;
    }
}