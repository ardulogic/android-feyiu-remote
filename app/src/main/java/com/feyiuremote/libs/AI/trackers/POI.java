package com.feyiuremote.libs.AI.trackers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import georegression.struct.shapes.Quadrilateral_F64;

public class POI {
    public int x1, x2, y1, y2;
    public int max_w, max_h;

    private Mat imageMat = new Mat();

    public double health = 100;

    public POI(int x1, int y1, int x2, int y2, int maxWidth, int maxHeight) {
        this.x1 = x1;
        this.x2 = x2;
        this.y1 = y1;
        this.y2 = y2;
        this.max_w = maxWidth;
        this.max_h = maxHeight;
    }

    public POI(Rect rect, int max_w, int max_h) {
        this(
                rect.x,
                rect.y,
                rect.x + rect.width,
                rect.y + rect.height,
                max_w,
                max_h
        );
    }


    public void update(Quadrilateral_F64 polygon, int polygon_max_w, int polygon_max_h) {
        float ratioW = (float) this.max_w / polygon_max_w;
        float ratioH = (float) this.max_h / polygon_max_h;

        this.x1 = (int) (Math.min(polygon.a.x, Math.min(polygon.b.x, Math.min(polygon.c.x, polygon.d.x))) * ratioW);
        this.x2 = (int) (Math.max(polygon.a.x, Math.max(polygon.b.x, Math.max(polygon.c.x, polygon.d.x))) * ratioW);
        this.y1 = (int) (Math.min(polygon.a.y, Math.min(polygon.b.y, Math.min(polygon.c.y, polygon.d.y))) * ratioH);
        this.y2 = (int) (Math.max(polygon.a.y, Math.max(polygon.b.y, Math.max(polygon.c.y, polygon.d.y))) * ratioH);
    }

    public POI getRescaled(int new_max_w, int new_max_h) {
        if (new_max_w != this.max_w || new_max_h != this.max_h) {
            float ratioW = (float) new_max_w / this.max_w;
            float ratioH = (float) new_max_h / this.max_h;

            return new POI(
                    (int) (x1 * ratioW), (int) (y1 * ratioH),
                    (int) (x2 * ratioW), (int) (y2 * ratioH),
                    new_max_w, new_max_h
            );
        }

        return this;
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
        return new double[]{(double) x1 / max_w, (double) y1 / max_h};
    }

    public double[] bottomRightRelativePerc() {
        return new double[]{(double) x2 / max_w, (double) y2 / max_h};
    }

    public double centerX() {
        return (double) (x1 + x2) / 2;
    }

    public double centerY() {
        return (double) (y1 + y2) / 2;
    }

    public double centerXPerc() {
        return (double) centerX() / max_w;
    }

    public double centerYPerc() {
        return centerY() / max_h;
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
        return calcDeviationPercentage(centerX(), max_w, poiDestinationX, invert_sign);
    }

    public double yCenterDevPercFromPoint(int poiDestinationY, boolean invert_sign) {
        return calcDeviationPercentage(centerY(), max_h, poiDestinationY, invert_sign);
    }

    public Rect toOpenCVRect() {
        return new Rect(x1, y1, width(), height());
    }

    private RectF toRect() {
        return new RectF(x1, y1, x1 + width(), y1 + height()); // Android RectF uses float values
    }

    public void drawOnCanvas(Canvas c) {
        // Draw the rectangle onto the canvas
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);  // Optional: you can change the style to FILL if needed
        paint.setColor(Color.LTGRAY);           // Set the color of the rectangle (example: red)
        paint.setStrokeWidth(5);             // Set the stroke width (for STROKE style)

        // Draw the rectangle on the canvas
        c.drawRect(this.getRescaled(c.getWidth(), c.getHeight()).toRect(), paint);
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
        // TODO: wrong scaling when POI is drawn on!
        return new Scalar(blue, green, red); // OpenCV uses BGR color order
    }

    public Quadrilateral_F64 toPolygon() {
        return new Quadrilateral_F64(
                Math.min(x1, x2), Math.min(y1, y2),  // Top-left corner
                Math.max(x1, x2), Math.min(y1, y2),  // Top-right corner
                Math.max(x1, x2), Math.max(y1, y2),  // Bottom-right corner
                Math.min(x1, x2), Math.max(y1, y2)   // Bottom-left corner
        );
    }

    public void update(int x1, int y1, int x2, int y2, Integer drawAreaW, Integer drawAreaH, Integer bitmapW, Integer bitmapH) {
        this.max_w = bitmapW;
        this.max_h = bitmapH;

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
        if (distance / max_w * 100 > 5) {
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

