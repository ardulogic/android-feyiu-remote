package com.feyiuremote.libs.Cameras.Panasonic;

public class PanasonicCameraState {

    public boolean available;

    public String name;

    public String model;

    public String udn;

    public String url;
    public String battery;
    public String mode;
    public Integer photoCapacity;
    public Integer remCapacityVideoSeconds;
    public boolean isRecording;
    public String temperature;
    public int remainingPhotoCapacity;

    public boolean isStreaming = false;
    public boolean sdCardIsWritable;
    public Boolean sdCardisAvailable;
    public Boolean isBurstOn;
    public Boolean sdAccessActive;
    public String displayType;
    public Integer progressTime;
    public String operateStatus;
    public Integer stopMotionFrames;
    public Boolean stopMotionEnabled;
    public String lensMode;
    public Boolean isGeoTagging;
    public Boolean isIntervalShooting;
    public String sdiState;
    public String warningDisplay;
    public String firmwareVersion;

    public PanasonicCameraState(String ddUrl) {
        this.url = ddUrl;
    }

    public String getBaseUrl() {
        return url.substring(0, url.indexOf(":", 7)) + "/";
    }

}
