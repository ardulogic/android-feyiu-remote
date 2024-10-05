package com.feyiuremote.libs.AI.trackers;

import android.graphics.Bitmap;

public interface IObjectTracker {
    void onNewFrame(Bitmap bitmap);

    void lock(POI poi);

    POI getPOI();

    void cancel();

}
