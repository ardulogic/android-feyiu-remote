package com.feyiuremote.libs.Feiyu;

import android.util.Log;

import java.math.BigInteger;
import java.util.HashMap;

public class FeyiuUtils {

    private final static String TAG = FeyiuUtils.class.getSimpleName();

    public static String move(int x_speed, int y_speed) {
        String x_dir = x_speed < 0 ? "00" : "ff";
        String y_dir = y_speed < 0 ? "00" : "ff";

        String hex = "a55a03000e0000050000"
                + toByteHex(x_speed) + x_dir
                + toByteHex(y_speed) + y_dir;

        Log.d(TAG, "MOVE X:" + x_speed + " Y:" + y_speed);
        Log.d(TAG, "MOVE HEX:" + FeyiuCrc.calc(hex));

        return FeyiuCrc.calc(hex);
    }

    public static String toByteHex(int num) {
        byte num_byte = (byte) num;
        int unsigned_num_byte = num_byte & 0xFF;

        return String.format("%02X", unsigned_num_byte).toLowerCase();
    }

}
