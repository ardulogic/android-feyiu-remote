package com.feyiuremote.libs.Feiyu.processors;

import android.graphics.Bitmap;
import android.graphics.Rect;

public interface IGimbalProcessor {

    void onPoiUpdate(Bitmap bitmap, Rect poi);

}
