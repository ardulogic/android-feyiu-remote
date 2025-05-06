package com.feyiuremote.libs.Feiyu;

public class Axes {

    public enum Axis {PAN, TILT, YAW}

    public static String toString(Axis axis) {
        if (axis == Axis.PAN) {
            return "Pan";
        }

        if (axis == Axis.TILT) {
            return "Tilt";
        }

        if (axis == Axis.YAW) {
            return "Yaw";
        }

        return "Unknown axis!";
    }

}
