package com.feyiuremote.libs.Utils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

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

    public int getXmin() {
        return Math.min(x1, x2);
    }

    /**
     * Pixel-based minimum Y (top-most)
     */
    public int getYmin() {
        return Math.min(y1, y2);
    }

    /**
     * Pixel-based maximum X (right-most)
     */
    public int getXmax() {
        return Math.max(x1, x2);
    }

    /**
     * Pixel-based maximum Y (bottom-most)
     */
    public int getYmax() {
        return Math.max(y1, y2);
    }

    /**
     * Normalized Xmin in [0..1]
     */
    public float getRelativeXmin() {
        return getXmin() / (float) max_w;
    }

    public float getRelativeLeft() {
        return getRelativeXmin();
    }

    /**
     * Normalized Ymin in [0..1]
     */
    public float getRelativeYmin() {
        return getYmin() / (float) max_h;
    }

    public float getRelativeTop() {
        return getRelativeYmin();
    }

    /**
     * Normalized right (max(x1,x2)/max_w)
     */
    public float getRelativeRight() {
        return getXmax() / (float) max_w;
    }

    /**
     * Normalized bottom (max(y1,y2)/max_h)
     */
    public float getRelativeBottom() {
        return getYmax() / (float) max_h;
    }

    /**
     * Normalized width (pixel width / max_w)
     */
    public float getRelativeWidth() {
        return width() / (float) max_w;
    }

    /**
     * Normalized height (pixel height / max_h)
     */
    public float getRelativeHeight() {
        return height() / (float) max_h;
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

    /**
     * Wrap this Rectangle in a Drawable, so it can be handed
     * to OverlayView.updateOverlay().
     *
     * @param color       the stroke color
     * @param strokeWidth the stroke width in pixels
     * @return a Drawable that will draw this rectangle (rescaled to the canvas size)
     */
    public Drawable asDrawable(final int color, final float strokeWidth) {
        // capture a copy of this Rectangle in case it mutates
        final Rectangle rectCopy = new Rectangle(x1, y1, x2, y2, max_w, max_h);

        return new Drawable() {
            private final Paint paint = new Paint();

            {
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(color);
                paint.setStrokeWidth(strokeWidth);
            }

            @Override
            public void draw(Canvas canvas) {
                // rescale to the canvas dimensions
                Rectangle r = rectCopy.getRescaled(canvas.getWidth(), canvas.getHeight());
                // draw using an Android RectF
                RectF rf = new RectF(
                        r.getXmin(), r.getYmin(),
                        r.getXmax(), r.getYmax()
                );
                canvas.drawRect(rf, paint);
            }

            @Override
            public void setAlpha(int alpha) {
                paint.setAlpha(alpha);
            }

            @Override
            public void setColorFilter(@Nullable ColorFilter colorFilter) {
                paint.setColorFilter(colorFilter);
            }

            @Override
            public int getOpacity() {
                return PixelFormat.TRANSLUCENT;
            }
        };
    }
}
