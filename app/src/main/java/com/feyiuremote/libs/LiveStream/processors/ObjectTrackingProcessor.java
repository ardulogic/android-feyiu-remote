package com.feyiuremote.libs.LiveStream.processors;

import android.graphics.Bitmap;

import com.feyiuremote.libs.AI.ObjectTracker;
import com.feyiuremote.libs.AI.views.IRectangleDrawViewListener;
import com.feyiuremote.libs.AI.views.RectangleDrawView;
import com.feyiuremote.libs.LiveStream.interfaces.ILiveFeedProcessor;

import java.util.concurrent.ExecutorService;

public class ObjectTrackingProcessor implements ILiveFeedProcessor {

    private final RectangleDrawView mRectDrawView;
    private final ObjectTracker mObjectTracker;

    public ObjectTrackingProcessor(RectangleDrawView rectangleDrawView, ExecutorService executor) {
        this.mRectDrawView = rectangleDrawView;

        this.mRectDrawView.setOnDrawListener(new IRectangleDrawViewListener() {
            @Override
            public void onNewRectangle(int x1, int y1, int x2, int y2) {
                mObjectTracker.lock(x1, y1, x2, y2);
            }

            @Override
            public void onClear() {
                mObjectTracker.clear();
            }
        });

        this.mObjectTracker = new ObjectTracker(mRectDrawView, executor);
    }

    @Override
    public Bitmap process(Bitmap bitmap) {
        return this.mObjectTracker.onNewFrame(bitmap);
    }
}
