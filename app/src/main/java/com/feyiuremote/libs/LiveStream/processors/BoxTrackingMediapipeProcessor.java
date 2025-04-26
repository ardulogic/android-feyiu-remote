package com.feyiuremote.libs.LiveStream.processors;

import android.graphics.Color;
import android.graphics.drawable.Drawable;

import com.feyiuremote.MainActivity;
import com.feyiuremote.libs.AI.trackers.IObjectTracker;
import com.feyiuremote.libs.AI.trackers.MediaPipeObjectTrackerCPU;
import com.feyiuremote.libs.AI.trackers.POI;
import com.feyiuremote.libs.Cameras.abstracts.CameraFrame;
import com.feyiuremote.libs.Feiyu.processors.GimbalFollowProcessor;
import com.feyiuremote.libs.LiveStream.LiveView.Listeners.ILiveViewTouchListener;
import com.feyiuremote.libs.LiveStream.LiveView.OverlayView;
import com.feyiuremote.libs.LiveStream.abstracts.FrameProcessor;
import com.feyiuremote.libs.Utils.Rectangle;
import com.feyiuremote.ui.camera.CameraViewModel;

import java.util.ArrayList;
import java.util.List;

public class BoxTrackingMediapipeProcessor extends FrameProcessor {

    public final static String TAG = BoxTrackingMediapipeProcessor.class.getSimpleName();
    //    private final FastObjectTracker tracker;
    private final IObjectTracker tracker;
    private final GimbalFollowProcessor gimbalFollowProcessor;

    private Rectangle drawingRectangle;


    public BoxTrackingMediapipeProcessor(OverlayView v, MainActivity mainActivity, CameraViewModel cameraViewModel) {
        super(v);

        gimbalFollowProcessor = new GimbalFollowProcessor(mainActivity.mBluetoothLeService);

        tracker = new MediaPipeObjectTrackerCPU(mainActivity);
        tracker.setListener(poi -> { // onUpdate
            if (mPOI != null) {
                mPOI.updateFrom(poi);
            }
        });

        // Prevent two trackers from fighting each other
//        mWaypointsProcessor = new GimbalWaypointsProcessor(mainActivity.getBaseContext(), cameraViewModel.waypointList);
//        mWaypointsProcessor.setOnStartListener(tracker::stop);

        attachDrawableSurface();
    }

    /**
     * If you experience random memory leaks or unexpected crashes
     * test methods using "synchronized". It's a multi-threaded mess
     */
    public void attachDrawableSurface() {
        overlayView.setOnTouchListener(new ILiveViewTouchListener() {
            @Override
            public void onLongPress() {

            }

            @Override
            public void onDoubleTap() {
                mPOI = null;
                tracker.stop();
                gimbalFollowProcessor.stop();
            }

            @Override
            public void onDrawingRectangleFail() {
                drawingRectangle = null;
            }

            @Override
            public void onDrawingRectangle(int x1, int y1, int x2, int y2, int drawAreaW, int drawAreaH) {
                drawingRectangle = new Rectangle(x1, y1, x2, y2, drawAreaW, drawAreaH);
            }

            @Override
            public void onNewRectangle(int rectX1, int rectY1, int rectX2, int rectY2, int drawAreaW, int drawAreaH) {
                drawingRectangle = null;
                mPOI = new POI(new Rectangle(rectX1, rectY1, rectX2, rectY2, drawAreaW, drawAreaH));
                tracker.lock(mPOI.rect);
            }
        });
    }

    @Override
    public void processFrame(CameraFrame frame) {
        //TODO: Frame might be lost due to concurrency
        tracker.onNewFrame(frame);

        // -- Build a list of all overlay shapes we want right now --
        List<Drawable> overlays = new ArrayList<>();

        if (mPOI != null) {
            overlays.add(mPOI.rect.asDrawable(Color.GREEN, 5f));
        }

        if (drawingRectangle != null) {
            overlays.add(drawingRectangle.asDrawable(Color.GREEN, 5f));
        }

        // -- Tell the OverlayView to show them --
        overlayView.updateOverlay(overlays);
    }

    @Override
    public void terminate() {
        stop();
    }

    @Override
    public void stop() {
        this.tracker.stop();
        this.gimbalFollowProcessor.stop();
    }

    @Override
    public boolean providesPOI() {
        return true;
    }

}
