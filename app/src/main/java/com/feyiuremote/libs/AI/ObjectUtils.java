package com.feyiuremote.libs.AI;

import com.feyiuremote.R;

import org.opencv.core.Rect;

import georegression.struct.shapes.Quadrilateral_F64;

public class ObjectUtils {

    /**
     * Converts points to rectangle
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     */
    public static Rect pointsToRect(int x1, int y1, int x2, int y2) {
        int minX = Math.min(x1, x2);
        int minY = Math.min(y1, y2);
        int maxX = Math.max(x1, x2);
        int maxY = Math.max(y1, y2);

        return new Rect(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * Converts points to rectangle with transformation ratio
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param transformRatioW
     * @param transformRatioH
     * @return
     */
    public static Rect pointsToRect(int x1, int y1, int x2, int y2, float transformRatioW, float transformRatioH) {
        Rect r = pointsToRect(x1, y1, x2, y2);

        return transformRect(r, transformRatioW, transformRatioH);
    }

    /**
     * Transforms rectangle
     *
     * @param rect
     * @param widthRatio
     * @param heightRatio
     * @return
     */
    public static Rect transformRect(Rect rect, float widthRatio, float heightRatio) {
        Rect r = new Rect();

        r.x = (int) ((float) rect.x * widthRatio);
        r.y = (int) ((float) rect.y * heightRatio);

        r.width = (int) ((float) rect.width * widthRatio);
        r.height = (int) ((float) rect.height * heightRatio);

        return r;
    }

    /**
     * Mediator between BCV and OCV shapes
     *
     * @param rect
     * @return
     */
    public static Quadrilateral_F64 rectToPolygon(Rect rect) {
        return new Quadrilateral_F64(
                rect.x, rect.y,
                rect.x + rect.width, rect.y,
                rect.x + rect.width, rect.y + rect.height,
                rect.x, rect.y + rect.height
        );
    }

    /**
     * Mediator between BCV and OCV shapes
     *
     * @param poly
     * @return
     */
    public static Rect polygonToRect(Quadrilateral_F64 poly) {
        return pointsToRect((int) poly.a.x, (int) poly.a.y, (int) poly.c.x, (int) poly.c.y);
    }

    public static android.graphics.Rect cvRectToAndroidRect(org.opencv.core.Rect cvRect) {
        return new android.graphics.Rect(cvRect.x, cvRect.y, cvRect.x + cvRect.width, cvRect.y + cvRect.height);
    }

}
