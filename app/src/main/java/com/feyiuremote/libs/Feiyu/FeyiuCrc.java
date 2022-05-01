package com.feyiuremote.libs.Feiyu;

public class FeyiuCrc {

    static {
        System.loadLibrary("crclib-c");
    }

    public static native String calc(String hex_string);

}
