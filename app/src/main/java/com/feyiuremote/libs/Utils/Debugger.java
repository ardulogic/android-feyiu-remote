package com.feyiuremote.libs.Utils;

import android.util.Log;

import java.util.ArrayList;

public class Debugger {
    private static final int MAX_MEASUREMENTS = 10;
    public static long startTime = 0L;

    public static ArrayList<Long> measurements = new ArrayList<>();

    public static void start() {
        startTime = System.currentTimeMillis();
    }

    public static void measure() {
        long time = (System.currentTimeMillis() - startTime);
        Log.d("Debugger", "Execution time:" + time + " ms / " + calculateAverage(time) + " avg ms");
    }

    public static synchronized double calculateAverage(long time) {
        measurements.add(time);
        // 33 - 40
        if (measurements.size() > MAX_MEASUREMENTS) {
            measurements.remove(0);
        }

        int count = Math.min(MAX_MEASUREMENTS, measurements.size());

        long sum = measurements.subList(measurements.size() - count, measurements.size())
                .stream()
                .mapToLong(Long::longValue)
                .sum();

        return count > 0 ? (double) sum / count : 0.0;
    }

    public static void measureFPS() {
        if (startTime != 0L) {
            long time = (System.currentTimeMillis() - startTime);
            double avgTime = calculateAverage(time);
            double fps = 1000 / avgTime;

            Log.d("Debugger", "AVG FPS:" + fps);
        }

        Debugger.start();
    }
}
