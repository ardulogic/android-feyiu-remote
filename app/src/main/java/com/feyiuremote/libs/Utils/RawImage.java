package com.feyiuremote.libs.Utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class RawImage {
    private byte[] rawBuffer;
    private Integer dataSize;

    private Integer dataStartIndex;


    public RawImage(byte[] data) {
        rawBuffer = data;
        dataStartIndex = 0;
        dataSize = data.length;
    }

    public Bitmap toBitmap() {
        return BitmapFactory.decodeByteArray(rawBuffer, dataStartIndex, dataSize - dataStartIndex);
    }


}
