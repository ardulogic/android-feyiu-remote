package com.feyiuremote.libs.Feiyu;

import android.util.Log;

import java.util.ArrayList;
import java.util.Optional;

import androidx.annotation.NonNull;

public class FeyiuAngle {
    private final static String TAG = FeyiuAngle.class.getSimpleName();

    // This angle is direct reading from sensor
    private float angle = 0;
    private int angle_cycles = 0;
    // This angle is direct reading from sensor
    private float last_angle = 0;

    private Long last_update = 0L;

    // This angle is calculated accounting full rotations
    private float last_true_angle = 0;

    private ArrayList<DataPoint> history = new ArrayList<>();
    private final int HISTORY_LEN = 5;


    public void update(int new_angle) {
        float fl_angle = (float) new_angle / 100;

        // Calculate the change in angle
        float angle_change = fl_angle - last_angle;

        // Check if the angle crossed the boundary from -180 to 180 or vice versa
        if (angle_change > 180) {
            angle_cycles--;
        } else if (angle_change < -180) {
            angle_cycles++;
        }

        // Update the angle with full rotations taken into account
        angle = fl_angle + (angle_cycles * 360);

        last_angle = fl_angle;
        last_true_angle = angle;
        last_update = System.currentTimeMillis();

        // update speed here
        addToHistory(System.currentTimeMillis(), angle);
    }

    public float value() {
        return angle;
    }

    public Float speed(Integer prev_values) {
        // Add your data points to the list here

        if (history.size() >= prev_values) {
            DataPoint firstDataPoint = history.get(history.size() - prev_values);
            DataPoint lastDataPoint = history.get(history.size() - 1);

            long totalTimeDifference = lastDataPoint.timestamp - firstDataPoint.timestamp;
            float totalValueDifference = lastDataPoint.value - firstDataPoint.value;

            return (float) totalValueDifference / totalTimeDifference;
        }

        return null;
    }

    public Float speed() {
        return speed(HISTORY_LEN);
    }

    public Float instantSpeed() {
        return history.get(history.size() - 1).value;
    }

    @NonNull
    public String posToString() {
        String s = String.format("%.1f°", value());
        if (Math.abs(value()) > 180) {
            s = "<font color='#FFA500'>" + s + "</font>";
        }
        return s;
    }

    public String speedToString() {
        return Optional.ofNullable(speed()).map(speed -> {
                    return String.format("%.1f°", speed() * 1000);
                })
                .orElse("-.-°");
    }

    public void addToHistory(long timestamp, float angle) {
        history.add(new DataPoint(timestamp, angle));

        if (history.size() > HISTORY_LEN) {
            history.remove(0);
        }
    }

    class DataPoint {
        public final long timestamp;
        public final float value;

        public DataPoint(long timestamp, float value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }
}