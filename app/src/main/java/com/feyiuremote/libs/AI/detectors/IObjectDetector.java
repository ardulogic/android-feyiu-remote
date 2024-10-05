package com.feyiuremote.libs.AI.detectors;

import android.content.Context;
import android.graphics.Bitmap;

public interface IObjectDetector {
    public void init(Context context);

    public DetectedObject[] detect(Bitmap bitmap);

}
