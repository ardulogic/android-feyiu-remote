package com.feyiuremote.libs.LiveStream.processors.abstracts;

import com.feyiuremote.libs.AI.trackers.POI;
import com.feyiuremote.libs.Cameras.abstracts.CameraFrame;
import com.feyiuremote.libs.LiveStream.LiveView.OverlayView;

public interface IFrameProcessor {
    /**
     * Called on your processor‐thread; return once you’ve queued any async work.
     */
    void processFrame(CameraFrame frame, OverlayView overlayView);

    /**
     * Called when this processor is deactivated—use to clear state.
     */

    boolean providesPOI();

    POI getPOI();

    void onDeactivate();
}