package com.feyiuremote.libs.LiveStream.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class JpegPacket {
    private final static String TAG = JpegPacket.class.getSimpleName();

    private static final byte[] JPEG_START_MARKER = {(byte) 0xFF, (byte) 0xD8}; // SOI (Start of Image)
    private static final byte[] JPEG_END_MARKER = {(byte) 0xFF, (byte) 0xD9};   // EOI (End of Image)

    private Integer startIndex;
    private Integer endIndex;

    private final byte[] tempBuffer = new byte[45000];

    /**
     * Checks if starting/ending marker of frame in stream exists at certain index
     *
     * @param marker      sequence of bytes for starting/ending marker
     * @param markerIndex position in buffer
     * @return boolean
     */
    private boolean markerExistsAt(byte[] marker, byte[] buffer, int bufferLength, int markerIndex) {
        // Ensure there is enough room in buffer to check the entire marker
        if (markerIndex + marker.length > buffer.length) {
            return false;
        }

        // Compare marker elements with buffer starting from bufferIndex
        for (int j = 0; j < marker.length; j++) {
            if (buffer[markerIndex + j] != marker[j]) {
                return false;
            }
        }

        return true;
    }


    private Integer findStartOfImage(byte[] buffer, int bufferLength) {
        if (startIndex != null && markerExistsAt(JPEG_START_MARKER, buffer, bufferLength, startIndex)) {
            return startIndex;
        }

        for (int i = 0; i < bufferLength; i++) {
            if (markerExistsAt(JPEG_START_MARKER, buffer, bufferLength, i)) {
                return i;
            }
        }

        return null;
    }

    private Integer findEndOfImage(byte[] buffer, int bufferLength) {
        if (endIndex != null && markerExistsAt(JPEG_END_MARKER, buffer, bufferLength, endIndex)) {
            return endIndex;
        }

        // Start loop from the end of the buffer and move backwards
        for (int i = bufferLength - 1; i >= 0; i--) {
            if (markerExistsAt(JPEG_END_MARKER, buffer, bufferLength, i)) {
                return i;
            }
        }

        return null;
    }

    public Bitmap getBitmapIfAvailable(byte[] packet, int length) {

        startIndex = findStartOfImage(packet, length);
        endIndex = findEndOfImage(packet, length);

        if (startIndex != null && endIndex != null) {
            System.arraycopy(packet, startIndex, tempBuffer, 0, endIndex - startIndex);

            return BitmapFactory.decodeByteArray(tempBuffer, 0, endIndex - startIndex);
        } else {
            Log.e(TAG, "Video packet failed!");
        }

        return null;
    }


}
