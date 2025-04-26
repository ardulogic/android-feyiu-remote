package com.feyiuremote.libs.LiveStream.processors;

import android.content.Context;
import android.graphics.Bitmap;

import com.feyiuremote.libs.AI.detectors.GooglePoseDetector;
import com.feyiuremote.libs.AI.trackers.POI;
import com.feyiuremote.libs.LiveStream.interfaces.ILiveFeedProcessor;

import java.util.concurrent.ExecutorService;

public class PoseTrackingProcessor implements ILiveFeedProcessor {

    private final GooglePoseDetector mPoseTracker;

    public PoseTrackingProcessor(Context context, ExecutorService executor) {
        this.mPoseTracker = new GooglePoseDetector(context, executor);
    }

//    public void setOnPoiUpdateListener(IPoiUpdateListener listener) {
//        this.mPoseTracker.setOnPoiUpdateListener(listener);
//    }

    @Override
    public POI getPOI() {
        return null;
    }

    @Override
    public void stop() {

    }

    @Override
    public Bitmap processFrame(Bitmap bitmap) {
        return this.mPoseTracker.onNewFrame(bitmap);
    }

}
