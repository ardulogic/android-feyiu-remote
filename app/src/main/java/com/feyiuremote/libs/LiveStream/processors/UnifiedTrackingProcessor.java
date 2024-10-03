package com.feyiuremote.libs.LiveStream.processors;

import android.graphics.Bitmap;
import android.util.Log;

import com.feyiuremote.MainActivity;
import com.feyiuremote.databinding.FragmentCameraBinding;
import com.feyiuremote.libs.AI.detectors.ObjectDetector;
import com.feyiuremote.libs.AI.detectors.PoseHeadDetector;
import com.feyiuremote.libs.AI.detectors.YoloDetector;
import com.feyiuremote.libs.AI.detectors.YoloV8.DetectedObject;
import com.feyiuremote.libs.AI.trackers.FastObjectTracker;
import com.feyiuremote.libs.AI.trackers.POI;
import com.feyiuremote.libs.AI.views.IRectangleDrawViewListener;
import com.feyiuremote.libs.AI.views.RectangleDrawView;
import com.feyiuremote.libs.Feiyu.processors.GimbalFollowProcessor;
import com.feyiuremote.libs.LiveStream.LiveImageView;
import com.feyiuremote.libs.LiveStream.interfaces.ILiveFeedProcessor;

import org.opencv.core.Rect;

import java.util.concurrent.ExecutorService;

public class UnifiedTrackingProcessor implements ILiveFeedProcessor {

    private final FastObjectTracker tracker;
    private final ObjectDetector detector;

    public final static String TAG = UnifiedTrackingProcessor.class.getSimpleName();
    private final GimbalFollowProcessor gimbalLiveProcessor;
    private final ExecutorService executor;
    private final FragmentCameraBinding binding;
    private LiveImageView mLiveView;

    private Integer bitmapW;
    private Integer bitmapH;

    private Integer drawAreaW;
    private Integer drawAreaH;

    private boolean manualTrack = false;

    private POI mPOI;


    private Long timeObjectDetected = 0L;
    private Long timeTrackingStarted = 0L;
    private Long timeFrameReceived = 0L;

    private Long detectInterval = 5000L;

    private POI mTargetPOI;


    public UnifiedTrackingProcessor(MainActivity mainActivity, FragmentCameraBinding binding) {
        this.executor = mainActivity.executor;
        this.binding = binding;
//        this.mLiveView = binding.liveView;

        this.drawAreaW = binding.liveView.getWidth();
        this.drawAreaH = binding.liveView.getHeight();

        gimbalLiveProcessor = new GimbalFollowProcessor(mainActivity.mBluetoothLeService);

        tracker = new FastObjectTracker(mainActivity.executor);
        tracker.setUpdateListener(poi -> {
            mPOI = poi;
        });

//        detector = new PoseHeadDetector(mainActivity.executor);
        detector = new YoloDetector(mainActivity.executor);
        detector.init(mainActivity);


//        cameraViewModel.liveFeedReceiveFFr.getValue().setImageProcessor(tracker);
        // Prevent two trackers from fighting each other
//        mWaypointsProcessor.setOnStartListener(tracker::cancel);
//        cameraViewModel.objectTrackingProcessor.postValue(tracker);
        attachDrawableSurface();
    }

    public void attachDrawableSurface() {
        binding.liveView.setOnDrawListener(new IRectangleDrawViewListener() {
            @Override
            public void onNewRectangle(int x1, int y1, int x2, int y2) {
                setPOI(x1, y1, x2, y2);
                mPOI.resetHealth();
                tracker.lock(mPOI);
                gimbalLiveProcessor.onPoiLock();
                manualTrack = true;
            }

            @Override
            public void onClear() {
                manualTrack = false;
                tracker.cancel();
                mPOI = null;
            }
        });

        binding.liveView.setOnLongPressListener((x, y) -> {
            gimbalLiveProcessor.updatePoiDestination((double) x / drawAreaW, (double) y / drawAreaH);
        });
    }

    public Bitmap drawPoiRectangle(Bitmap bitmap) {
        return mPOI.drawOnBitmap(bitmap);
    }


    private Long timeSinceLastDetection() {
        return System.currentTimeMillis() - timeObjectDetected;
    }

    private Long timeSinceTrackingStarted() {
        return System.currentTimeMillis() - timeTrackingStarted;
    }

    @Override
    public Bitmap onNewFrame(Bitmap bitmap) {
        timeFrameReceived = System.currentTimeMillis();

        if (bitmapW == null || bitmapH == null) {
            setBitmapDimensions(bitmap);
        }
//
        Bitmap finalBitmap = bitmap;
//        this.executor.execute(() -> tracker.onNewFrame(finalBitmap));
//        Bitmap finalBitmap = bitmap;
//        this.executor.execute(() -> detector.onNewFrame(finalBitmap));
        if (mPOI != null) {
            tracker.onNewFrame(bitmap);
            bitmap = drawPoiRectangle(bitmap);
        }

        if (!manualTrack) {
            this.executor.execute(() -> {
                if ((mPOI == null || mPOI.health < 90) ||
                        timeSinceLastDetection() > 200 ||
                        timeSinceTrackingStarted() > 3000) {
                    autoTrack(finalBitmap);
                }
            });
        }
////

//
//        bitmap = detector.draw(bitmap);
//        bitmap = gimbalLiveProcessor.drawPoiDestination(bitmap);
        bitmap = detector.draw(bitmap);
//
//        adjustPOI();
//        Long time = System.currentTimeMillis() - timeFrameReceived;
//        Log.d(TAG, "Processing delay: " + time + "ms");

        return bitmap;
    }

    private void adjustPOI() {
        if (mTargetPOI != null) {
            mPOI.resizeTowards(mTargetPOI);
//            tracker.lock(mPOI);
        }
    }

    private void setBitmapDimensions(Bitmap b) {
        this.bitmapW = b.getWidth();
        this.bitmapH = b.getHeight();
    }

    private void autoTrack(Bitmap bitmap) {
        detector.onNewFrame(bitmap);
//        bitmap = detector.draw(bitmap);

        DetectedObject object = detector.getClosestDetectedObjectToPoi(0, mPOI);
        if (object != null && object.isNotTooBigFor(bitmapW, bitmapH)) {
            Log.d(TAG, "Object detected");
            timeObjectDetected = System.currentTimeMillis();
            timeTrackingStarted = System.currentTimeMillis();

            Boolean overlaps = false;
            if (mPOI != null) {
                overlaps = detector.poiOverlapsWithObject(object, mPOI);
            } else {
                setPOI(object.rect);
                return;
            }

            if (!overlaps) {
                mTargetPOI = new POI(object.rect, bitmapW, bitmapH);
                setPOI(object.rect);
                mPOI.resetHealth();
                tracker.lock(mPOI);
            }
        }
    }

    @Override
    public POI getPOI() {
        return mPOI;
    }

    @Override
    public void cancel() {
        this.tracker.cancel();
    }

    public void setPOI(int x1, int y1, int x2, int y2) {
        if (mPOI == null) {
            mPOI = new POI(x1, y1, x2, y2, drawAreaW, drawAreaH, bitmapW, bitmapH);
        } else {
            mPOI.update(x1, y1, x2, y2, drawAreaW, drawAreaH, bitmapW, bitmapH);
        }
    }

    public void setPOI(Rect r) {
        if (mPOI == null) {
            mPOI = new POI(r, bitmapW, bitmapH);
        } else {
            mPOI.update(r);
        }
    }
}
