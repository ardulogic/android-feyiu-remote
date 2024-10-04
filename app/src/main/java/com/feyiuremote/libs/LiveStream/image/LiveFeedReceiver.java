package com.feyiuremote.libs.LiveStream.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.feyiuremote.libs.LiveStream.interfaces.ILiveFeedProcessor;
import com.feyiuremote.libs.LiveStream.interfaces.ILiveFeedReceiver;
import com.feyiuremote.libs.LiveStream.interfaces.ILiveFeedUpdateListener;
import com.feyiuremote.libs.Utils.NamedThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class LiveFeedReceiver implements ILiveFeedReceiver {
    private final static String TAG = LiveFeedReceiver.class.getSimpleName();

    private ILiveFeedUpdateListener mStatusListener;

    protected static final ThreadFactory threadFactory = new NamedThreadFactory("LiveFeedReceiver");

    protected static final ExecutorService executor = Executors.newFixedThreadPool(2, threadFactory);

    private final Context context;

    private Bitmap frameBitmap;

    private Long frames = 0L;

    private Long lastImageTimestamp = null;
    private ILiveFeedProcessor mImageProcessor;

    public LiveFeedReceiver(Context context) {
        this.context = context;
    }

    public void setImageProcessor(ILiveFeedProcessor processor) {
        this.mImageProcessor = processor;
    }

    public void setUpdateListener(ILiveFeedUpdateListener statusListener) {
        this.mStatusListener = statusListener;
    }

    @Override
    public void onNewFrame(Bitmap bitmap) {
        frames++;
        this.lastImageTimestamp = System.currentTimeMillis();
        this.frameBitmap = bitmap;

        executor.execute(() -> {
            if (mImageProcessor != null) {
                mStatusListener.onNewFrame(
                        mImageProcessor.onNewFrame(frameBitmap)
                );
            } else {
                mStatusListener.onNewFrame(frameBitmap);
            }
        });

        executor.execute(() -> {
            mStatusListener.onMessage("New frame received: " + frames);
        });
    }

    @Override
    public Bitmap getImage() {
        if (frameBitmap != null) {
            if (this.mImageProcessor != null) {
                // This is used to draw on the image (reactangles and shit)
                return mImageProcessor.onNewFrame(frameBitmap);
            } else {
                // Don't apply any effects if mImageProcessor is not available
                return frameBitmap;
            }
        } else {
            Log.e(TAG, "Image is null, not received yet!");
            return null;
        }
    }

    @Override
    public Long getImageTimestamp() {
        return lastImageTimestamp;
    }


    @Override
    public void onError(String message) {
        Log.e(TAG, message);
        this.mStatusListener.onMessage(message);
    }

    @Override
    public void onWarning(String message) {
        Log.w(TAG, message);
        this.mStatusListener.onMessage(message);
    }

    @Override
    public void onInfo(String message) {
        Log.d(TAG, message);
        this.mStatusListener.onMessage(message);
    }

}
