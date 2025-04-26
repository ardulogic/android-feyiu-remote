package com.feyiuremote.libs.LiveStream.abstracts;

import com.feyiuremote.libs.AI.trackers.POI;
import com.feyiuremote.libs.Cameras.abstracts.CameraFrame;
import com.feyiuremote.libs.LiveStream.LiveView.OverlayView;

public abstract class FrameProcessor {

    public POI mPOI = null;
    public OverlayView overlayView;

    public FrameProcessor(OverlayView overlayView) {
        this.overlayView = overlayView;
    }

    public void setOverlayView(OverlayView overlayView) {
        this.overlayView = overlayView;
    }

    public abstract void processFrame(CameraFrame frame);

    public abstract void terminate();

    public abstract void stop();

    abstract public boolean providesPOI();

    public POI getPOI() {
        if (providesPOI()) {
            return mPOI;
        }

        return null;
    }

}
