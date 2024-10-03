package com.feyiuremote.libs.AI.trackers;

import android.graphics.Bitmap;
import android.util.Log;


import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import georegression.struct.shapes.Quadrilateral_F64;

public class POI {
    public int x1, x2, y1, y2;
    public int frame_width, frame_height;

    private Mat imageMat = new Mat();

    public double health = 100;

    public POI(int x1, int y1, int x2, int y2, int frame_width, int frame_height) {
        this.x1 = x1;
        this.x2 = x2;
        this.y1 = y1;
        this.y2 = y2;
        this.frame_width = frame_width;
        this.frame_height = frame_height;
    }

    public POI(Rect rect, int frame_width, int frame_height) {
        this(
                rect.x,
                rect.y,
                rect.x + rect.width,
                rect.y + rect.height,
                frame_width,
                frame_height
        );
    }

    public POI(int x1, int y1, int x2, int y2, Integer drawAreaW, Integer drawAreaH, Integer bitmapW, Integer bitmapH) {
        this.update(x1, y1, x2, y2, drawAreaW, drawAreaH, bitmapW, bitmapH);
    }

    public int width() {
        return x2 - x1;
    }

    public int height() {
        return y2 - y1;
    }


    public void adjWidth(int increment) {
        x1 -= increment;
        x2 += increment;
    }

    public void adjHeight(int increment) {
        y1 -= increment;
        y2 += increment;
    }

    public double[] topLeftRelativePerc() {
        return new double[]{(double) x1 / frame_width, (double) y1 / frame_height};
    }

    public double[] bottomRightRelativePerc() {
        return new double[]{(double) x2 / frame_width, (double) y2 / frame_height};
    }

    public double centerX() {
        return (double) (x1 + x2) / 2;
    }

    public double centerY() {
        return (double) (y1 + y2) / 2;
    }

    public double centerXPerc() {
        return (double) centerX() / frame_width;
    }

    public double centerYPerc() {
        return centerY() / frame_height;
    }


    /**
     * How much % POI is off target point
     */
    public double calcDeviationPercentage(double point, double axis_width, double target_point, boolean invert_sign) {
        int dev = (int) Math.abs(point - target_point);
        double perc_dev = (double) dev / axis_width;

        if (point < target_point) {
            return invert_sign ? -perc_dev : perc_dev;
        } else {
            return invert_sign ? perc_dev : -perc_dev;
        }
    }

    public double xCenterDevPercFromPoint(int poiDestinationX, boolean invert_sign) {
        return calcDeviationPercentage(centerX(), frame_width, poiDestinationX, invert_sign);
    }

    public double yCenterDevPercFromPoint(int poiDestinationY, boolean invert_sign) {
        return calcDeviationPercentage(centerY(), frame_height, poiDestinationY, invert_sign);
    }

    public Rect toOpenCVRect() {
        return new Rect(x1, y1, width(), height());
    }

    public Bitmap drawOnBitmap(Bitmap bitmap) {
        // Convert Bitmap to Mat
        imageMat.release();
        Utils.bitmapToMat(bitmap, imageMat);

        // Map health to color
        Scalar color = mapHealthToColor((int) health);

        Rect r = toOpenCVRect();
        Imgproc.rectangle(imageMat, r, color, 3);

        // Convert Mat back to Bitmap
        Bitmap modifiedBitmap = Bitmap.createBitmap(imageMat.cols(), imageMat.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(imageMat, modifiedBitmap);

        return modifiedBitmap;
    }

    private Scalar mapHealthToColor(int health) {
        // Map health to color gradient (red to green)
        int red = (int) (255 * (100 - health) / 100.0);
        int green = (int) (255 * health / 100.0);
        int blue = 50; // You can adjust the blue component if needed

        // Ensure color values are in the valid range (0-255)
        red = Math.max(0, Math.min(255, red));
        green = Math.max(0, Math.min(255, green));
        blue = Math.max(0, Math.min(255, blue));

        return new Scalar(blue, green, red); // OpenCV uses BGR color order
    }

    public Quadrilateral_F64 toPolygon() {
        return new Quadrilateral_F64(
                x1, y1,
                x2, y1,
                x2, y2,
                x1, y2
        );
    }

    public void update(int x1, int y1, int x2, int y2, Integer drawAreaW, Integer drawAreaH, Integer bitmapW, Integer bitmapH) {
        this.frame_width = bitmapW;
        this.frame_height = bitmapH;

        this.update(
                (int) ((double) x1 / drawAreaW * bitmapW),
                (int) ((double) y1 / drawAreaH * bitmapH),
                (int) ((double) x2 / drawAreaW * bitmapW),
                (int) ((double) y2 / drawAreaH * bitmapH)
        );
    }

    public void update(Quadrilateral_F64 poly) {
        update((int) poly.a.x, (int) poly.a.y, (int) poly.c.x, (int) poly.c.y);
    }

    public void update(Rect rect) {
        update(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height);
    }

    public void update(int x1, int y1, int x2, int y2) {
        calculateHealth(x1, y1, x2, y2);

        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    private void calculateHealth(int nx1, int ny1, int nx2, int ny2) {
        double oldCenterX = centerX();
        double oldCenterY = centerY();

        double newCenterX = (nx1 + nx2) / 2.0;
        double newCenterY = (ny1 + ny2) / 2.0;

        double distance = Math.sqrt(Math.pow(newCenterX - oldCenterX, 2) + Math.pow(newCenterY - oldCenterY, 2));

        // Adjust health based on the distance or any other criteria
        if (distance / frame_width * 100 > 5) {
            health -= 20;
        }

        // Ensure health is non-negative
        health = Math.max(0, health);
    }

    public void resetHealth() {
        health = 100;
    }


    public double calculateDistance(Rect rect) {
        double centerX1 = centerX();
        double centerY1 = centerY();

        double centerX2 = rect.x + rect.width / 2.0;
        double centerY2 = rect.y + rect.height / 2.0;

        return Math.sqrt(Math.pow(centerX1 - centerX2, 2) + Math.pow(centerY1 - centerY2, 2));
    }

    public void resizeTowards(POI mTargetPOI) {
        if (mTargetPOI.x1 < x1) x1 -= 1;
        if (mTargetPOI.x2 < x2) x2 -= 1;
        if (mTargetPOI.x1 > x1) x1 += 1;
        if (mTargetPOI.x2 > x2) x2 += 1;

        if (mTargetPOI.y1 < y1) y1 -= 1;
        if (mTargetPOI.y2 < y2) y2 -= 1;
        if (mTargetPOI.y1 > y1) y1 += 1;
        if (mTargetPOI.y2 > y2) y2 += 1;


    }
}

