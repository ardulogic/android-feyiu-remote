package com.feyiuremote.libs.Utils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import org.opencv.core.Rect;

import georegression.struct.shapes.Quadrilateral_F64;


public class Rectangle {
    public int x1, x2, y1, y2;
    public int max_w, max_h;

    public Rectangle(int x1, int y1, int x2, int y2, int maxWidth, int maxHeight) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.max_w = maxWidth;
        this.max_h = maxHeight;
    }

    public Rectangle getRescaled(int new_max_w, int new_max_h) {
        if (new_max_w != this.max_w || new_max_h != this.max_h) {
            float ratioW = (float) new_max_w / this.max_w;
            float ratioH = (float) new_max_h / this.max_h;

            return new Rectangle(
                    (int) (x1 * ratioW), (int) (y1 * ratioH),
                    (int) (x2 * ratioW), (int) (y2 * ratioH),
                    new_max_w, new_max_h
            );
        }

        return this;
    }

    public void drawOnCanvas(Canvas c) {
        drawOnCanvas(c, Color.GREEN);
    }

    public void drawOnCanvas(Canvas c, int color) {
        // Draw the rectangle onto the canvas
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);  // Optional: you can change the style to FILL if needed
        paint.setColor(color);           // Set the color of the rectangle (example: red)
        paint.setStrokeWidth(5);             // Set the stroke width (for STROKE style)

        // Draw the rectangle on the canvas
        c.drawRect(this.getRescaled(c.getWidth(), c.getHeight()).toRectF(), paint);
    }

    public Quadrilateral_F64 toPolygon() {
        return new Quadrilateral_F64(
                Math.min(x1, x2), Math.min(y1, y2),  // Top-left corner
                Math.max(x1, x2), Math.min(y1, y2),  // Top-right corner
                Math.max(x1, x2), Math.max(y1, y2),  // Bottom-right corner
                Math.min(x1, x2), Math.max(y1, y2)   // Bottom-left corner
        );
    }

    private RectF toRectF() {
        return new RectF(x1, y1, x1 + (x2 - x1), y1 + (y2 - y1)); // Android RectF uses float values
    }

    public Rect toOpenCVRect() {
        return new Rect(x1, y1, width(), height());
    }


    public void adjWidth(int increment) {
        x1 -= increment;
        x2 += increment;
    }

    public void adjHeight(int increment) {
        y1 -= increment;
        y2 += increment;
    }

    public int width() {
        return Math.abs(x2 - x1);
    }

    public int height() {
        return Math.abs(y2 - y1);
    }

    public double centerX() {
        return (double) (x1 + x2) / 2;
    }

    public double centerW() {
        return (double) max_w / 2;
    }

    public double centerY() {
        return (double) (y1 + y2) / 2;
    }

    public double centerH() {
        return (double) max_h / 2;
    }

    public double xDevPercFromFrameCenter() {
        return (centerX() - centerW()) / max_w;
    }

    public double yDevPercFromFrameCenter() {
        return (centerH() - centerY()) / max_h;
    }

    public double[] topLeftRelativePerc() {
        return new double[]{(double) x1 / max_w, (double) y1 / max_h};
    }

    public double[] bottomRightRelativePerc() {
        return new double[]{(double) x2 / max_w, (double) y2 / max_h};
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
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public void update(int x1, int y1, int x2, int y2, int max_w, int max_h) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.max_w = max_w;
        this.max_h = max_h;
    }

    public void update(Quadrilateral_F64 polygon, int polygon_max_w, int polygon_max_h) {
        float ratioW = (float) this.max_w / polygon_max_w;
        float ratioH = (float) this.max_h / polygon_max_h;

        this.x1 = (int) (Math.min(polygon.a.x, Math.min(polygon.b.x, Math.min(polygon.c.x, polygon.d.x))) * ratioW);
        this.x2 = (int) (Math.max(polygon.a.x, Math.max(polygon.b.x, Math.max(polygon.c.x, polygon.d.x))) * ratioW);
        this.y1 = (int) (Math.min(polygon.a.y, Math.min(polygon.b.y, Math.min(polygon.c.y, polygon.d.y))) * ratioH);
        this.y2 = (int) (Math.max(polygon.a.y, Math.max(polygon.b.y, Math.max(polygon.c.y, polygon.d.y))) * ratioH);
    }


    public double centerXPercInFrame() {
        return centerX() / max_w;
    }

    public double centerYPercInFrame() {
        return centerX() / max_h;
    }
}
