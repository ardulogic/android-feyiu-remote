package com.feyiuremote.libs.LiveStream.processors;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import com.feyiuremote.MainActivity;
import com.feyiuremote.databinding.FragmentCameraBinding;
import com.feyiuremote.libs.AI.trackers.FastObjectTracker;
import com.feyiuremote.libs.AI.trackers.POI;
import com.feyiuremote.libs.Feiyu.processors.GimbalFollowProcessor;
import com.feyiuremote.libs.Feiyu.processors.position.GimbalWaypointsProcessor;
import com.feyiuremote.libs.LiveStream.LiveView.Listeners.ILiveViewTouchListener;
import com.feyiuremote.libs.LiveStream.interfaces.ILiveFeedProcessor;
import com.feyiuremote.libs.Utils.Rectangle;
import com.feyiuremote.ui.camera.CameraViewModel;

import java.util.concurrent.ExecutorService;

public class UnifiedTrackingProcessor implements ILiveFeedProcessor {

    public final static String TAG = UnifiedTrackingProcessor.class.getSimpleName();
    private final FastObjectTracker tracker;
    private final GimbalFollowProcessor gimbalFollowProcessor;
    private final ExecutorService executor;
    private final FragmentCameraBinding binding;

    public final GimbalWaypointsProcessor mWaypointsProcessor;

    private POI mPOI;

    private Rectangle drawingRectangle;


    public UnifiedTrackingProcessor(MainActivity mainActivity, FragmentCameraBinding binding, CameraViewModel cameraViewModel) {
        this.executor = mainActivity.executor;
        this.binding = binding;

        gimbalFollowProcessor = new GimbalFollowProcessor(mainActivity.mBluetoothLeService);

        tracker = new FastObjectTracker(mainActivity.executor, mainActivity);
        tracker.setListener(rectangle -> { // onUpdate
            if (mPOI != null) {
                mPOI.update(rectangle);
            }
        });

        // Prevent two trackers from fighting each other
        mWaypointsProcessor = new GimbalWaypointsProcessor(mainActivity.getBaseContext(), cameraViewModel.waypointList);
        mWaypointsProcessor.setOnStartListener(tracker::stop);

        attachDrawableSurface();
    }

    /**
     * If you experience random memory leaks or unexpected crashes
     * test methods using "synchronized". It's a multi-threaded mess
     */
    public void attachDrawableSurface() {
        binding.liveView.setOnTouchListener(new ILiveViewTouchListener() {
            @Override
            public void onLongPress() {

            }

            @Override
            public void onDoubleTap() {
                mPOI = null;
                tracker.stop();
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
    public Bitmap onNewFrame(Bitmap bitmap) {
        Bitmap mutableBitmap = bitmap.isMutable() ? bitmap : bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);

        tracker.onNewFrame(bitmap);

        if (mPOI != null) {
            mPOI.rect.drawOnCanvas(canvas, Color.GREEN);
            executor.execute(() -> gimbalFollowProcessor.onPoiUpdate(mPOI));
        }

        if (drawingRectangle != null) {
            drawingRectangle.drawOnCanvas(canvas, Color.LTGRAY);
        }

        return mutableBitmap;
    }

    @Override
    public POI getPOI() {
        return mPOI;
    }

    @Override
    public void cancel() {
        this.tracker.stop();
        this.mWaypointsProcessor.cancel();
    }

}
