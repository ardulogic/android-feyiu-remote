package com.feyiuremote.libs.LiveStream.interfaces;

import android.graphics.Bitmap;

import org.opencv.core.Rect;

public interface IPoiUpdateListener {

    void onPoiUpdate(Bitmap bitmap, Rect poi);

}
