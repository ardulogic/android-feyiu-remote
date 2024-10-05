package com.feyiuremote.libs.AI.detectors;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

public class DetectedObject {
    public int label;

    public Rect rect;
    public Mat mask;

    public DetectedObject(int label, Rect rect, Mat mask) {
        this.label = label;
        this.rect = rect;
        this.mask = mask;
    }

    public boolean isNotTooBigFor(int w, int h) {
        int maxAllowedWidth = (int) (0.8 * w);
        int maxAllowedHeight = (int) (0.8 * h);

        int objectWidth = rect.width;
        int objectHeight = rect.height;

        boolean isWidthOk = objectWidth <= maxAllowedWidth;
        boolean isHeightOk = objectHeight <= maxAllowedHeight;

        return isWidthOk && isHeightOk;
    }
}