package com.feyiuremote.libs.Feiyu.queue.debug;

import com.feyiuremote.libs.Feiyu.FeyiuState;

public class GimbalEmulatorDebugger {
    private static final String TAG = GimbalEmulatorDebugger.class.getSimpleName();
    public static double panCalSpeed;

    public static double panAngleStart;
    public static double tiltCalSpeed;
    public static double tiltAngleStart;
    public static long timePanStart;
    public static long timeTiltStart;
    public static long timePanEnd;
    public static long timeTiltEnd;

    public static void onPanValuesSet(double calSpeed) {
        tiltAngleStart = FeyiuState.getInstance().angle_pan.value();
        timePanStart = System.currentTimeMillis();
        panCalSpeed = calSpeed;
    }

    public static void onTiltValuesSet(double calSpeed) {
        panAngleStart = FeyiuState.getInstance().angle_pan.value();
        timeTiltStart = System.currentTimeMillis();
        tiltCalSpeed = calSpeed;
    }

    public static void onPanEnd(double pan_angle) {
        timePanEnd = System.currentTimeMillis();
        double panAngleEnd = FeyiuState.getInstance().angle_pan.value();

//        double realAngleChange = pan_angle - panAngleStart;
//        double durationSeconds = (timePanEnd - timePanStart) / 1000.0;
//        double expectedAngleChange = panCalSpeed * durationSeconds;
//        double angleDifference = realAngleChange - expectedAngleChange;
//
//        System.out.println(TAG + ": PAN - Real Change: " + realAngleChange +
//                ", Expected: " + expectedAngleChange + ", Difference: " + angleDifference);
    }

    public static void onTiltEnd(double tilt_angle) {
        timeTiltEnd = System.currentTimeMillis();

    }


}
