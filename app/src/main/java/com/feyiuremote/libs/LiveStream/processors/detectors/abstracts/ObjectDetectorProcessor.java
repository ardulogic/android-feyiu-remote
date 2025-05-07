package com.feyiuremote.libs.LiveStream.processors.detectors.abstracts;

import android.graphics.Color;
import android.graphics.drawable.Drawable;

import com.feyiuremote.MainActivity;
import com.feyiuremote.libs.AI.detectors.abstracts.DetectedObject;
import com.feyiuremote.libs.AI.detectors.abstracts.IObjectDetector;
import com.feyiuremote.libs.Cameras.abstracts.CameraFrame;
import com.feyiuremote.libs.Feiyu.processors.GimbalFollowProcessor;
import com.feyiuremote.libs.LiveStream.LiveView.OverlayView;
import com.feyiuremote.libs.LiveStream.abstracts.FrameProcessor;
import com.feyiuremote.ui.camera.models.CameraViewModel;

import java.util.LinkedList;

public abstract class ObjectDetectorProcessor extends FrameProcessor {
    private final IObjectDetector detector;
    private final MainActivity mainActivity;
    private final CameraViewModel cameraViewModel;
    protected final GimbalFollowProcessor gimbalFollowProcessor;

    public ObjectDetectorProcessor(OverlayView v, MainActivity mainActivity, CameraViewModel cameraViewModel) {
        super(v);

        this.mainActivity = mainActivity;
        this.cameraViewModel = cameraViewModel;
        gimbalFollowProcessor = new GimbalFollowProcessor();

        this.detector = createDetector();
        this.detector.init(mainActivity.getApplicationContext());

        if (detector == null) {
            throw new RuntimeException("Detector was not created, its null!");
        }

        setDetectorListener(detector);
    }

    protected abstract IObjectDetector createDetector();

    protected void setDetectorListener(IObjectDetector detector) {
        detector.setListener(objects -> drawDetections(objects, overlayView));
    }

    @Override
    public void processFrame(CameraFrame frame) {
        detector.onNewFrame(frame);
    }

    protected void drawDetections(LinkedList<DetectedObject> objects, OverlayView view) {
        LinkedList<Drawable> drawables = new LinkedList<>();

        // Create drawables from objects
        for (DetectedObject obj : objects) {
            // You can customize the color or stroke width here as needed
            int color = Color.GREEN;
            float strokeWidth = 5.0f;

            Drawable drawable = obj.rect.asDrawable(color, strokeWidth);
            drawables.add(drawable);
        }

        // Update the overlay with the new drawables
        view.updateOverlay(drawables);
    }

    @Override
    public void terminate() {
        stop();
        detector.shutdown();
    }

    @Override
    public void stop() {
        detector.stop();
        overlayView.clear();
    }

    @Override
    public boolean providesPOI() {
        return true;
    }
}
