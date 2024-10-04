package com.feyiuremote.libs.LiveStream.processors;

import android.content.Context;
import android.graphics.Bitmap;

import com.feyiuremote.libs.AI.trackers.GooglePoseTracker;
import com.feyiuremote.libs.AI.trackers.POI;
import com.feyiuremote.libs.LiveStream.interfaces.ILiveFeedProcessor;
import com.feyiuremote.libs.LiveStream.interfaces.IPoiUpdateListener;

import java.util.concurrent.ExecutorService;

public class PoseTrackingProcessor implements ILiveFeedProcessor {

    private final GooglePoseTracker mPoseTracker;

    public PoseTrackingProcessor(Context context, ExecutorService executor) {
        this.mPoseTracker = new GooglePoseTracker(context, executor);
    }

    public void setOnPoiUpdateListener(IPoiUpdateListener listener) {
        this.mPoseTracker.setOnPoiUpdateListener(listener);
    }

    @Override
    public Bitmap onNewFrame(Bitmap toBitmap) {
        return this.mPoseTracker.onNewFrame(toBitmap);
    }

    @Override
    public POI getPOI() {
        return null;
    }

    @Override
    public void cancel() {

    }


}
