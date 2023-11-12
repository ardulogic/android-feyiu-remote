package com.feyiuremote.libs.LiveStream.interfaces;

import android.graphics.Bitmap;

import org.opencv.core.Rect;

public interface IPoiUpdateListener {

    void onPoiLock(Rect poi);

    void onPoiUpdate(Bitmap bitmap, Rect poi);

    void onPoiCancel();

    void onPoiTargetPositionUpdate(double x_perc, double y_perc);
}
