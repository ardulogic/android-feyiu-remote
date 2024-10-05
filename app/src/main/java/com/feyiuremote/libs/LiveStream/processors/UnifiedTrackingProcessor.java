package com.feyiuremote.libs.LiveStream.processors;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

import com.feyiuremote.MainActivity;
import com.feyiuremote.databinding.FragmentCameraBinding;
import com.feyiuremote.libs.AI.trackers.FastObjectTracker;
import com.feyiuremote.libs.AI.trackers.POI;
import com.feyiuremote.libs.Feiyu.processors.GimbalFollowProcessor;
import com.feyiuremote.libs.LiveStream.LiveView.Listeners.IRectangleDrawTouchListener;
import com.feyiuremote.libs.LiveStream.LiveView.Listeners.OnTouchRectangleDraw;
import com.feyiuremote.libs.LiveStream.LiveView.LiveImageView;
import com.feyiuremote.libs.LiveStream.interfaces.ILiveFeedProcessor;
import com.feyiuremote.libs.LiveStream.interfaces.IPoiUpdateListener;

import org.opencv.core.Rect;

import java.util.concurrent.ExecutorService;

public class UnifiedTrackingProcessor implements ILiveFeedProcessor {

    private final FastObjectTracker tracker;
//    private final ObjectDetector detector;

    public final static String TAG = UnifiedTrackingProcessor.class.getSimpleName();
    private final GimbalFollowProcessor gimbalLiveProcessor;
    private final ExecutorService executor;
    private final FragmentCameraBinding binding;
    private final MainActivity mainActivity;
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

    private OnTouchRectangleDraw mOnTouchRectangleDraw;


    public UnifiedTrackingProcessor(MainActivity mainActivity, FragmentCameraBinding binding) {
        this.mainActivity = mainActivity;
        this.executor = mainActivity.executor;
        this.binding = binding;
//        this.mLiveView = binding.liveView;

        this.drawAreaW = binding.liveView.getWidth();
        this.drawAreaH = binding.liveView.getHeight();

        gimbalLiveProcessor = new GimbalFollowProcessor(mainActivity.mBluetoothLeService);

        tracker = new FastObjectTracker(mainActivity.executor);

        tracker.setOnPoiUpdateListener(new IPoiUpdateListener() {
            @Override
            public void onPoiLock(POI poi) {

            }

            @Override
            public void onPoiUpdate(POI poi) {
                mPOI = poi;
            }

            @Override
            public void onPoiCancel() {
                mPOI = null;
            }

            @Override
            public void onPoiTargetPositionUpdate(double x_perc, double y_perc) {

            }
        });

//        detector = new PoseHeadDetector(mainActivity.executor);
//        detector = new YoloDetector(mainActivity.executor);
//        detector.init(mainActivity);


//        cameraViewModel.liveFeedReceiveFFr.getValue().setImageProcessor(tracker);
        // Prevent two trackers from fighting each other
//        mWaypointsProcessor.setOnStartListener(tracker::cancel);
//        cameraViewModel.objectTrackingProcessor.postValue(tracker);
        attachDrawableSurface();
    }

    public void attachDrawableSurface() {
        mainActivity.runOnUiThread(() -> {
            mOnTouchRectangleDraw = new OnTouchRectangleDraw(mainActivity, binding.liveView);
            mOnTouchRectangleDraw.setListener(new IRectangleDrawTouchListener() {
                @Override
                public void onNewRectangle(int x1, int y1, int x2, int y2, int max_w, int max_h) {
                    mPOI = new POI(x1, y1, x2, y2, max_w, max_h);
                    tracker.lock(x1, y1, x2, y2, max_w, max_h);
                }

                @Override
                public void onClear() {
                    Log.d(TAG, "SHOULD DRAW THE FUCKING THING");
                }

                @Override
                public void onLongPress() {
                    Log.d(TAG, "SHOULD DRAW THE FUCKING THING");
                }

                @Override
                public void onDoubleTap() {
                    Log.d(TAG, "SHOULD DRAW THE FUCKING THING");
                }

                @Override
                public void onTooSmallRectangle() {
                    Log.d(TAG, "SHOULD DRAW THE FUCKING THING");
                }

                @Override
                public void onDrawingRectangle(int x1, int y1, int x2, int y2) {
//                    Log.d(TAG, "SHOULD DRAW THE FUCKING THING");
//                    Bitmap b = binding.liveView.getFrame();
//                    if (b == null) {
//                        b = Bitmap.createBitmap(drawAreaW, drawAreaH, Bitmap.Config.ARGB_8888);
//                    }
//
//                    b = mOnTouchRectangleDraw.drawCurrentRectangleOnBitmap(b);
//
//                    binding.liveView.setFrameBitmap(b);
                }
            });

            binding.liveView.setOnTouchListener(mOnTouchRectangleDraw);
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

        this.executor.execute(() -> tracker.onNewFrame(bitmap));

        Bitmap mutableBitmap = bitmap.isMutable() ? bitmap : bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        drawDrawnRectangle(canvas);


        if (mPOI != null) {
            tracker.onNewFrame(mutableBitmap);
            mPOI.drawOnCanvas(canvas);
        }

        if (!manualTrack) {
            this.executor.execute(() -> {
                if ((mPOI == null || mPOI.health < 90) ||
                        timeSinceLastDetection() > 200 ||
                        timeSinceTrackingStarted() > 3000) {
//                    autoTrack(finalBitmap);
                }
            });
        }
////

//
//        bitmap = detector.draw(bitmap);
//        bitmap = gimbalLiveProcessor.drawPoiDestination(bitmap);
//        if (detector != null) {
//            bitmap = detector.draw(bitmap);
//        }

//}
//        adjustPOI();
//        Long time = System.currentTimeMillis() - timeFrameReceived;
//        Log.d(TAG, "Processing delay: " + time + "ms");

        return mutableBitmap;
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
//        detector.onNewFrame(bitmap);
//        bitmap = detector.draw(bitmap);

        /*
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

         */
    }

    public void drawDrawnRectangle(Canvas c) {
        if (mOnTouchRectangleDraw != null) {
            RectF r = mOnTouchRectangleDraw.getRect(bitmapW, bitmapH);

            // Draw the rectangle onto the canvas
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);  // Optional: you can change the style to FILL if needed
            paint.setColor(Color.LTGRAY);           // Set the color of the rectangle (example: red)
            paint.setStrokeWidth(5);             // Set the stroke width (for STROKE style)

            // Draw the rectangle on the canvas
            c.drawRect(r, paint);
        }
    }


    @Override
    public POI getPOI() {
        return mPOI;
    }

    @Override
    public void cancel() {
//        this.tracker.cancel();
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
