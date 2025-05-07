package com.feyiuremote.libs.AI.detectors.abstracts;

import com.feyiuremote.libs.Utils.Rectangle;

import org.opencv.core.Mat;

public class DetectedObject {
    public int id;

    public String label;

    public Rectangle rect;
    public Mat mask;

    public DetectedObject(int id, String label, Rectangle rect) {
        this.id = id;
        this.label = label;
        this.rect = rect;
    }

    public DetectedObject(int id, String label, Rectangle rect, Mat mask) {
        this.id = id;
        this.label = label;
        this.rect = rect;
        this.mask = mask;
    }

}