package com.feyiuremote.libs.Cameras.Panasonic.Values;

import java.util.HashMap;
import java.util.Map;

public class PanasonicShutterSpeeds {
    private static final Map<String, String> shutterSpeeds = new HashMap<>();

    static {
        shutterSpeeds.put("4000", "3072/256");
        shutterSpeeds.put("3200", "2987/256");
        shutterSpeeds.put("2500", "2902/256");
        shutterSpeeds.put("2000", "2816/256");
        shutterSpeeds.put("1600", "2731/256");
        shutterSpeeds.put("1300", "2646/256");
        shutterSpeeds.put("1000", "2560/256");
        shutterSpeeds.put("800", "2475/256");
        shutterSpeeds.put("640", "2390/256");
        shutterSpeeds.put("500", "2304/256");
        shutterSpeeds.put("400", "2219/256");
        shutterSpeeds.put("320", "2134/256");
        shutterSpeeds.put("250", "2048/256");
        shutterSpeeds.put("200", "1963/256");
        shutterSpeeds.put("160", "1878/256");
        shutterSpeeds.put("125", "1792/256");
        shutterSpeeds.put("100", "1707/256");
        shutterSpeeds.put("80", "1622/256");
        shutterSpeeds.put("60", "1536/256");
        shutterSpeeds.put("50", "1451/256");
        shutterSpeeds.put("40", "1366/256");
        shutterSpeeds.put("30", "1280/256");
        shutterSpeeds.put("25", "1195/256");
        shutterSpeeds.put("20", "1110/256");
        shutterSpeeds.put("15", "1024/256");
        shutterSpeeds.put("13", "939/256");
        shutterSpeeds.put("10", "854/256");
        shutterSpeeds.put("8", "768/256");
        shutterSpeeds.put("6", "683/256");
        shutterSpeeds.put("5", "598/256");
        shutterSpeeds.put("4", "512/256");
        shutterSpeeds.put("3.2", "427/256");
        shutterSpeeds.put("2.5", "342/256");
        shutterSpeeds.put("2", "256/256");
        shutterSpeeds.put("1.6", "171/256");
        shutterSpeeds.put("1.3", "86/256");
        shutterSpeeds.put("1", "0/256");
        shutterSpeeds.put("1.3s", "-85/256");
        shutterSpeeds.put("1.6s", "-170/256");
        shutterSpeeds.put("2s", "-256/256");
        shutterSpeeds.put("2.5s", "-341/256");
        shutterSpeeds.put("3.2s", "-426/256");
        shutterSpeeds.put("4s", "-512/256");
        shutterSpeeds.put("5s", "-682/256");
        shutterSpeeds.put("6s", "-768/256");
        shutterSpeeds.put("8s", "-853/256");
        shutterSpeeds.put("10s", "-938/256");
        shutterSpeeds.put("13s", "-1024/256");
        shutterSpeeds.put("15s", "-1109/256");
        shutterSpeeds.put("20s", "-1194/256");
        shutterSpeeds.put("25s", "-1280/256");
        shutterSpeeds.put("30s", "-1365/256");
        shutterSpeeds.put("40s", "-1450/256");
        shutterSpeeds.put("50s", "-1536/256");
        shutterSpeeds.put("60s", "16384/256");
        shutterSpeeds.put("B", "256/256");
    }

    public static String getControlValue(String shutter_speed) {
        return shutterSpeeds.get(shutter_speed);
    }
}