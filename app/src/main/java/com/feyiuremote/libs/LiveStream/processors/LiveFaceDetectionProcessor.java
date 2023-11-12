package com.feyiuremote.libs.LiveStream.processors;

import android.graphics.Bitmap;

import com.feyiuremote.libs.AI.detectors.FaceDetector;
import com.feyiuremote.libs.AI.trackers.POI;
import com.feyiuremote.libs.LiveStream.interfaces.ILiveFeedProcessor;
import com.feyiuremote.libs.LiveStream.interfaces.IPoiUpdateListener;

public class LiveFaceDetectionProcessor implements ILiveFeedProcessor {

    @Override
    public Bitmap process(Bitmap toBitmap) {
        return FaceDetector.detect(toBitmap);
    }

    @Override
    public void setOnPoiUpdateListener(IPoiUpdateListener listener) {

    }

    @Override
    public POI getPOI() {
        return null;
    }

    @Override
    public void cancel() {

    }

}
