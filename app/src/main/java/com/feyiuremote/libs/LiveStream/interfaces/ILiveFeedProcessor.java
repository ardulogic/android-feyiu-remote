package com.feyiuremote.libs.LiveStream.interfaces;

import android.graphics.Bitmap;

import com.feyiuremote.libs.AI.trackers.POI;

public interface ILiveFeedProcessor {

    Bitmap onNewFrame(Bitmap toBitmap);

    POI getPOI();

    void cancel();
}
