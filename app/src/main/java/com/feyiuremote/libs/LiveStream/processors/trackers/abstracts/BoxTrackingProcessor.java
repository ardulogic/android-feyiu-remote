package com.feyiuremote.libs.LiveStream.processors.trackers.abstracts;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.feyiuremote.MainActivity;
import com.feyiuremote.libs.AI.trackers.IObjectTracker;
import com.feyiuremote.libs.AI.trackers.POI;
import com.feyiuremote.libs.Cameras.abstracts.CameraFrame;
import com.feyiuremote.libs.Feiyu.processors.GimbalFollowProcessor;
import com.feyiuremote.libs.LiveStream.LiveView.Listeners.ILiveViewTouchListener;
import com.feyiuremote.libs.LiveStream.LiveView.OverlayView;
import com.feyiuremote.libs.LiveStream.abstracts.FrameProcessor;
import com.feyiuremote.libs.Utils.Rectangle;
import com.feyiuremote.ui.camera.models.CameraViewModel;

import java.util.ArrayList;
import java.util.List;

public abstract class BoxTrackingProcessor extends FrameProcessor {

    public final static String TAG = BoxTrackingProcessor.class.getSimpleName();
    protected final IObjectTracker tracker;
    private final GimbalFollowProcessor gimbalFollowProcessor;
    protected final CameraViewModel cameraViewModel;
    private Rectangle drawingRectangle;

    protected Activity mainActivity;

    protected abstract IObjectTracker createTracker();

    public BoxTrackingProcessor(OverlayView v, MainActivity mainActivity, CameraViewModel cameraViewModel) {
        super(v);

        this.mainActivity = mainActivity;
        this.cameraViewModel = cameraViewModel;
        gimbalFollowProcessor = new GimbalFollowProcessor(cameraViewModel);

        tracker = createTracker();
        if (tracker == null) {
            throw new RuntimeException("Tracker was not created, its null!");
        }

        tracker.setListener(poi -> { // onUpdate
            if (mPOI != null) {
                mPOI.updateFrom(poi);

                gimbalFollowProcessor.onPoiUpdate(poi);
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
            overlays.add(mPOI.asTextDrawable(Color.WHITE, 24));
        }

        if (drawingRectangle != null) {
            overlays.add(drawingRectangle.asDrawable(Color.GREEN, 5f));
        }

        // -- Tell the OverlayView to show them --
        overlayView.updateOverlay(overlays);
    }

    @Override
    public void onOverlayViewAttached() {
        overlayView.post(() -> {
            overlayView.attachOnTouchListeners();
            overlayView.setTouchProcessorListener(new ILiveViewTouchListener() {
                @Override
                public void onLongPress() {

                }

                @Override
                public void onDoubleTap(int x, int y, int drawAreaW, int drawAreaH) {
                    Log.d(TAG, "onDoubleTap");
                    drawingRectangle = null;
                    mPOI = new POI(createTapRectangle(x, y, drawAreaW, drawAreaH));
                    tracker.lock(mPOI.rect);
                }

                @Override
                public void onSingleTap(int x, int y, int drawAreaW, int drawAreaH) {
                    Log.d(TAG, "onSingleTap");
                    drawingRectangle = null;
                    stop();
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
        });
    }

    @Override
    public boolean providesPOI() {
        return true;
    }

    protected int getTapRectSize() {
        return 100;
    }

    protected Rectangle createTapRectangle(int tapX, int tapY, int drawAreaW, int drawAreaH) {
        int half = getTapRectSize() / 2;
        int x1 = tapX - half;
        int y1 = tapY - half;
        int x2 = tapX + half;
        int y2 = tapY + half;

        return new Rectangle(x1, y1, x2, y2, drawAreaW, drawAreaH);
    }

    @Override
    public void stop() {
        this.mPOI = null;
        this.tracker.stop();
        this.overlayView.clear();
        this.gimbalFollowProcessor.stop();
    }

    @Override
    public void terminate() {
        stop();

        tracker.shutdown();
    }

}
