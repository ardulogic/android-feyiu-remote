package com.feyiuremote.libs.LiveStream.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import com.feyiuremote.libs.Utils.Debugger;

public class RawImage {
    private byte[] rawBuffer;
    private Integer dataSize;

    private Integer dataStartIndex;

    /**
     * Creates a raw image from buffer
     *
     * @param buffer      raw buffer of a fixed size. For better performance it shouldnt realcate.
     * @param start_index Buffer can contain the "end" previous image and "start" from the next one
     *                    This defines the index when the image starts
     * @param length      This defines the count of bytes from start to end of a single image
     */
    public RawImage(byte[] buffer, int start_index, int length) {
        rawBuffer = buffer;
        dataStartIndex = start_index;
        dataSize = length;
    }

    public RawImage(byte[] data) {
        rawBuffer = data;
        dataStartIndex = 0;
        dataSize = data.length;
    }

    public Bitmap toBitmap() {
        return BitmapFactory.decodeByteArray(rawBuffer, dataStartIndex, dataSize - dataStartIndex);
    }

    public void update(byte[] buffer, int start_index, int length) {
        rawBuffer = buffer;
        dataStartIndex = start_index;
        dataSize = length;
    }
}
