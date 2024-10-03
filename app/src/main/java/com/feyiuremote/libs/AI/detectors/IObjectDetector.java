package com.feyiuremote.libs.AI.detectors;

import android.content.Context;
import android.graphics.Bitmap;

import com.feyiuremote.libs.AI.detectors.YoloV8.DetectedObject;

public interface IObjectDetector {
    public void init(Context context);

    public DetectedObject[] detect(Bitmap bitmap);

}
