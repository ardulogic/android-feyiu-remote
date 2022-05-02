package com.feyiuremote.libs.Feiyu;

import android.util.Log;

import java.math.BigInteger;
import java.util.HashMap;

public class FeyiuUtils {

    private final static String TAG = FeyiuUtils.class.getSimpleName();

    public final static String CONTROL_CHARACTERISTIC_ID = "0000ff01-0000-1000-8000-00805f9b34fb";
    public final static String NOTIFICATION_CHARACTERISTIC_ID = "0000ff02-0000-1000-8000-00805f9b34fb";

    public final static String SERVICE_ID = "0000ffff-0000-1000-8000-00805f9b34fb";

    public static String tick() {
        return "a55a03047200000100017ee0";
    }

    /**
     * | SpdX  | SpdY | CRC
     * a55a03000e0000050000 | 5d00  | 0000 | 7109
     *
     * @param x_speed
     * @param y_speed
     * @return
     */
    public static String move(int x_speed, int y_speed) {
        String hex = "a55a03000e0000050000"
                + intToTwoByteReversedHex(x_speed)
                + intToTwoByteReversedHex(y_speed);

        Log.d(TAG, "MOVE X:" + x_speed + " Y:" + y_speed);
        Log.d(TAG, "MOVE HEX:" + FeyiuCrc.calc(hex));

        return FeyiuCrc.calc(hex);
    }

    /**
     * Type | Sensitivity | ?? | Crc
     * a55a03022020000300 0d 35 00 d534 | Pan-Only  | Pan 50
     * a55a03022020000300 0d 0d 00 e9b8 | Pan-Only  | Pan 10
     *
     * @param sens
     * @return
     */
    public static String setPanSensitivity(int sens) {
        String hex = "a55a03022020000300"
                + "0d" // Pan or Tilt
                + intToUnsignedOneByteHex(sens)
                + "00"; // ??

        return FeyiuCrc.calc(hex);
    }

    /**
     * Type | Sensitivity | ?? | Crc
     * a55a03022020000300 0c 00 00 85f9 | Tilt-Only | Tilt 0
     * a55a03022020000300 0c 64 00 6b3e | Tilt-Only | Tilt Max
     *
     * @param sens
     * @return
     */
    public static String setTiltSensitivity(int sens) {
        String hex = "a55a03022020000300"
                + "0c" // Pan or Tilt
                + intToUnsignedOneByteHex(sens)
                + "00"; // ??

        return FeyiuCrc.calc(hex);
    }

    /**
     * Gimbal is weird. It receives integer bytes in reverse order
     * Speed is controlled by two bytes
     *
     * @param number
     * @return
     */
    public static String intToTwoByteReversedHex(int number) {
        byte[] y_speed_bytes = new byte[2];
        y_speed_bytes[0] = (byte) (number & 0xFF);
        y_speed_bytes[1] = (byte) ((number >> 8) & 0xFF);

        StringBuilder sb = new StringBuilder();
        for (byte b : y_speed_bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }

        return sb.toString();
    }

    public static String intToUnsignedOneByteHex(int number) {
        byte num_byte = (byte) number;
        int unsigned_num_byte = num_byte & 0xFF;

        return String.format("%02X", unsigned_num_byte).toLowerCase();
    }

    public static String toByteHex(int num) {
        byte num_byte = (byte) num;
        int unsigned_num_byte = num_byte & 0xFF;

        return String.format("%02X", unsigned_num_byte).toLowerCase();
    }

}
