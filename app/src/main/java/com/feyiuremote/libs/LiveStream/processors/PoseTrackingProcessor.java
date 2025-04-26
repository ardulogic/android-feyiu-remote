package com.feyiuremote.libs.LiveStream.processors;

import android.content.Context;

import com.feyiuremote.libs.AI.detectors.GooglePoseDetector;
import com.feyiuremote.libs.Cameras.abstracts.CameraFrame;
import com.feyiuremote.libs.LiveStream.LiveView.OverlayView;
import com.feyiuremote.libs.LiveStream.abstracts.FrameProcessor;

import java.util.concurrent.ExecutorService;

public class PoseTrackingProcessor extends FrameProcessor {

    private final GooglePoseDetector mPoseTracker;

    public PoseTrackingProcessor(OverlayView v, Context context, ExecutorService executor) {
        super(v);

        this.mPoseTracker = new GooglePoseDetector(context, executor);
    }

//    public void setOnPoiUpdateListener(IPoiUpdateListener listener) {
//        this.mPoseTracker.setOnPoiUpdateListener(listener);
//    }

    @Override
    public void processFrame(CameraFrame frame) {
        this.mPoseTracker.onNewFrame(frame.bitmap());
    }

    @Override
    public void terminate() {

    }

    @Override
    public void stop() {

    }

    @Override
    public boolean providesPOI() {
        return false;
    }


}
