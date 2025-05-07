package com.feyiuremote.libs.AI.detectors.pose;

import android.graphics.Bitmap;

import com.feyiuremote.libs.AI.detectors.abstracts.IObjectDetectorListener;
import com.google.mlkit.vision.pose.Pose;

public interface IPoseDetectorListener extends IObjectDetectorListener {
    void onPoseUpdate(Pose pose, Bitmap detectedBitmap);

}
