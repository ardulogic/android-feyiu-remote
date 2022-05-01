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
    public Integer videoCapacity;
    public boolean isRecording;
    public String temperature;

    public PanasonicCameraState(String ddUrl) {
        this.url = ddUrl;
    }

    public String getBaseUrl() {
        return url.substring(0, url.indexOf(":", 7)) + "/";
    }

}
