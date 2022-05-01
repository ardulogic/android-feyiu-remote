package com.feyiuremote.libs.LiveStream.processors;

import android.graphics.Bitmap;

import com.feyiuremote.libs.AI.FaceDetector;
import com.feyiuremote.libs.LiveStream.interfaces.ILiveFeedProcessor;

public class LiveFaceDetectionProcessor implements ILiveFeedProcessor {
    @Override
    public Bitmap process(Bitmap toBitmap) {
        return FaceDetector.detect(toBitmap);
    }
}
