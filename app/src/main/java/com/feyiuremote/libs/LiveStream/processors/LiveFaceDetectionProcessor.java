package com.feyiuremote.libs.LiveStream.processors;

import android.graphics.Bitmap;

import com.feyiuremote.libs.AI.detectors.FaceDetector;
import com.feyiuremote.libs.AI.trackers.POI;
import com.feyiuremote.libs.LiveStream.interfaces.ILiveFeedProcessor;

public class LiveFaceDetectionProcessor implements ILiveFeedProcessor {

    @Override
    public Bitmap onNewFrame(Bitmap toBitmap) {
        return FaceDetector.detect(toBitmap);
    }

    @Override
    public POI getPOI() {
        return null;
    }

    @Override
    public void cancel() {

    }

}
