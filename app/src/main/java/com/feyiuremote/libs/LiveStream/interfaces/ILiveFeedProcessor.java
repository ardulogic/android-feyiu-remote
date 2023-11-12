package com.feyiuremote.libs.LiveStream.interfaces;

import android.graphics.Bitmap;

import com.feyiuremote.libs.AI.trackers.POI;

public interface ILiveFeedProcessor {

    Bitmap process(Bitmap toBitmap);

    void setOnPoiUpdateListener(IPoiUpdateListener listener);

    POI getPOI();

    void cancel();
}
