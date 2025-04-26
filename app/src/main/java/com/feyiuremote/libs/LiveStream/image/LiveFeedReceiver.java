package com.feyiuremote.libs.LiveStream.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.feyiuremote.libs.Cameras.abstracts.CameraFrame;
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

    private CameraFrame frame;

    private Long frames = 0L;

    private ILiveFeedProcessor mFrameProcessor;

    public LiveFeedReceiver(Context context) {
        this.context = context;
    }

    public void setFrameProcessor(ILiveFeedProcessor processor) {
        this.mFrameProcessor = processor;
    }

    public void setUpdateListener(ILiveFeedUpdateListener statusListener) {
        this.mStatusListener = statusListener;
    }

    @Override
    public void onNewFrame(CameraFrame frame) {
        frames++;

        this.frame = frame;

        if (mFrameProcessor != null) {
            mStatusListener.onNewFrame(
                    mFrameProcessor.processFrame(frame.toBitmap())
            );
        } else {
            mStatusListener.onNewFrame(frame.toBitmap());
        }

        executor.execute(() -> {
            this.onInfo("New frame received: " + frames);
        });
    }

    @Override
    public Bitmap getFrameBitmap() {
        if (frame != null) {
            if (this.mFrameProcessor != null) {
                // This is used to draw on the image (reactangles and shit)
                return mFrameProcessor.processFrame(frame.toBitmap());
            } else {
                // Don't apply any effects if mImageProcessor is not available
                return frame.toBitmap();
            }
        } else {
            Log.e(TAG, "Image is null, not received yet!");
            return null;
        }
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
