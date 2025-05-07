package com.feyiuremote.ui.camera.fragments.tracker;

public class TrackerOption {
    public final int id;
    public final String name;
    private final String processorClass;

    public TrackerOption(int id, String name, String className) {
        this.id = id;
        this.name = name;
        this.processorClass = className;
    }

    @Override
    public String toString() {
        return name; // This is what will be shown in the Spinner
    }

    public String getFrameProcessorClass() {
        return processorClass;
    }
}
