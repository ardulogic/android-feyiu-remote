package com.feyiuremote.libs.Cameras.abstracts;


import android.graphics.Bitmap;

import java.util.concurrent.TimeUnit;

/**
 * Simple wrapper around a Bitmap that can clone itself for tracking.
 */
public abstract class CameraFrame {
    private final Bitmap bitmap;
    private final long timestampNs;

    public CameraFrame(Bitmap b) {
        this.bitmap = b;
        this.timestampNs = System.nanoTime();
    }

    public long getTimestampNs() {
        return this.timestampNs;
    }

    /**
     * @return the timestamp (when this frame was created) in milliseconds
     */
    public long getTimestampMs() {
        // Convert nanoseconds to milliseconds
        return TimeUnit.NANOSECONDS.toMillis(timestampNs);
        // Alternatively: return timestampNs / 1_000_000L;
    }

    public Bitmap bitmap() {
        return bitmap;
    }
}
