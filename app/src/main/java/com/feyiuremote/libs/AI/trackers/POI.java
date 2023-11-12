package com.feyiuremote.libs.AI.trackers;

public class POI {
    public int x1, x2, y1, y2;
    public int frame_width, frame_height;

    public POI(int x, int y, int width, int height, int frame_width, int frame_height) {
        this.x1 = x;
        this.x2 = x + width;
        this.y1 = y;
        this.y2 = y + height;
        this.frame_width = frame_width;
        this.frame_height = frame_height;
    }

    public double[] topLeftPerc() {
        return new double[]{(double) x1 / frame_width, (double) y1 / frame_height};
    }

    public double[] bottomRightPerc() {
        return new double[]{(double) x2 / frame_width, (double) y2 / frame_height};
    }

    public double centerXPerc() {
        return (double) (x1 + x2) / (2 * frame_width);
    }

    public double centerYPerc() {
        return (double) (y1 + y2) / (2 * frame_height);
    }
}

