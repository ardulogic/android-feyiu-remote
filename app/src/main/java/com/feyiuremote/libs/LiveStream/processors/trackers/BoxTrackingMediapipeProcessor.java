package com.feyiuremote.libs.LiveStream.processors.trackers;

import com.feyiuremote.MainActivity;
import com.feyiuremote.libs.AI.trackers.IObjectTracker;
import com.feyiuremote.libs.AI.trackers.MediaPipeObjectTrackerCPU;
import com.feyiuremote.libs.LiveStream.LiveView.OverlayView;
import com.feyiuremote.libs.LiveStream.processors.trackers.abstracts.BoxTrackingProcessor;
import com.feyiuremote.ui.camera.models.CameraViewModel;

public class BoxTrackingMediapipeProcessor extends BoxTrackingProcessor {

    public final static String TAG = BoxTrackingMediapipeProcessor.class.getSimpleName();

    public BoxTrackingMediapipeProcessor(OverlayView v, MainActivity mainActivity, CameraViewModel cameraViewModel) {
        super(v, mainActivity, cameraViewModel);
    }

    @Override
    protected IObjectTracker createTracker() {
        return new MediaPipeObjectTrackerCPU(this.mainActivity);
    }
}
