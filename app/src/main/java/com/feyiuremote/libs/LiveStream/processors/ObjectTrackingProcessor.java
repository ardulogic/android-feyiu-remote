package com.feyiuremote.libs.LiveStream.processors;

import android.graphics.Bitmap;

import com.feyiuremote.libs.AI.ObjectUtils;
import com.feyiuremote.libs.AI.trackers.BCVObjectTracker;
import com.feyiuremote.libs.AI.trackers.POI;
import com.feyiuremote.libs.AI.views.IRectangleDrawViewListener;
import com.feyiuremote.libs.AI.views.RectangleDrawView;
import com.feyiuremote.libs.LiveStream.interfaces.ILiveFeedProcessor;
import com.feyiuremote.libs.LiveStream.interfaces.IPoiUpdateListener;

import java.util.concurrent.ExecutorService;

public class ObjectTrackingProcessor implements ILiveFeedProcessor {

    private RectangleDrawView mRectDrawView;
    private BCVObjectTracker mObjectTracker;
    private final ExecutorService executor;

    private IPoiUpdateListener mPoiUpdateListener;

    public ObjectTrackingProcessor(RectangleDrawView rectangleDrawView, ExecutorService executor) {
        this.executor = executor;

        attachTo(rectangleDrawView);
    }

    public void attachTo(RectangleDrawView rectangleDrawView) {
        this.mRectDrawView = rectangleDrawView;

        this.mRectDrawView.setOnDrawListener(new IRectangleDrawViewListener() {
            @Override
            public void onNewRectangle(int x1, int y1, int x2, int y2) {
                mObjectTracker.lock(x1, y1, x2, y2);
                if (mPoiUpdateListener != null) {
                    mPoiUpdateListener.onPoiLock(ObjectUtils.pointsToRect(x1, y1, x2, y2));
                }
            }

            @Override
            public void onClear() {
                mPoiUpdateListener.onPoiCancel();
                mObjectTracker.clear();
            }
        });

        this.mRectDrawView.setOnLongPressListener((x, y) -> {
            if (mPoiUpdateListener != null) {
                mPoiUpdateListener.onPoiTargetPositionUpdate((double) x / mRectDrawView.getMeasuredWidth(), (double) y / mRectDrawView.getMeasuredHeight());
            }
        });

        this.mObjectTracker = new BCVObjectTracker(mRectDrawView, executor);
    }


    public void setOnPoiUpdateListener(IPoiUpdateListener listener) {
        this.mPoiUpdateListener = listener;
        this.mObjectTracker.setOnPoiUpdateListener(listener);
    }

    @Override
    public POI getPOI() {
        return mObjectTracker.getPOI();
    }

    @Override
    public void cancel() {
        this.mObjectTracker.clear();
    }

    @Override
    public Bitmap process(Bitmap bitmap) {
        return this.mObjectTracker.onNewFrame(bitmap);
    }

}
