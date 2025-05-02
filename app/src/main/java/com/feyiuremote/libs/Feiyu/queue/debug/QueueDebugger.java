package com.feyiuremote.libs.Feiyu.queue.debug;

import android.util.Log;

import com.feyiuremote.libs.Feiyu.processors.position.GimbalPositionTarget;

import java.util.ArrayList;

public class QueueDebugger {
    private static final int MAX_MEASUREMENTS = 10;
    public static long startTime = 0L;

    public static ArrayList<WaypointState> measurements = new ArrayList<>();

    public static void start() {
        measurements.clear();
        startTime = System.currentTimeMillis();
    }

    public static void add(GimbalPositionTarget target) {

    }

    public static void finish() {

    }


}
