package com.feyiuremote.libs.Cameras.Panasonic.Values;

import java.util.HashMap;
import java.util.Map;

public class PanasonicFocusSpeeds {
    private static final Map<Integer, String> shutterSpeeds = new HashMap<>();

    static {
        shutterSpeeds.put(-2, "wide-fast");
        shutterSpeeds.put(-1, "wide-normal");
        shutterSpeeds.put(1, "tele-normal");
        shutterSpeeds.put(2, "tele-fast");
    }

    public static String getControlValue(Integer shutter_speed) {
        return shutterSpeeds.get(shutter_speed);
    }
}