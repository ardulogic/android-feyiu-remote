package com.feyiuremote.libs.Utils;

public class RangeMapper {
    public static int mapInt(int value, int min, int max, int newMin, int newMax) {
        int range = max - min;
        int newRange = newMax - newMin;

        if (range == 0) {
            return newMin;
        }

        int newValue = newMin + (int) (((double) newRange / range) * (value - min));
        return newValue;
    }

    public static double mapDouble(double value, double min, double max, double newMin, double newMax) {
        double range = max - min;
        double newRange = newMax - newMin;

        if (range == 0) {
            return newMin;
        }

        double newValue = newMin + ((newRange / range) * (value - min));
        return newValue;
    }

    public static float mapFloat(float value, float min, float max, float newMin, float newMax) {
        float range = max - min;
        float newRange = newMax - newMin;

        if (range == 0) {
            return newMin;
        }

        float newValue = newMin + ((newRange / range) * (value - min));
        return newValue;
    }

}
