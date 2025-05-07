package com.feyiuremote.libs.LiveStream.processors.detectors;

import com.feyiuremote.libs.AI.detectors.FaceDetector;
import com.feyiuremote.libs.Cameras.abstracts.CameraFrame;
import com.feyiuremote.libs.LiveStream.LiveView.OverlayView;
import com.feyiuremote.libs.LiveStream.abstracts.FrameProcessor;

public class FaceDetectionProcessor extends FrameProcessor {

    public FaceDetectionProcessor(OverlayView v) {
        super(v);
    }

    @Override
    public void processFrame(CameraFrame frame) {
        FaceDetector.detect(frame.bitmap());
    }

    @Override
    public boolean providesPOI() {
        return false;
    }


    @Override
    public void terminate() {

    }

    @Override
    public void stop() {

    }


}
