package com.feyiuremote.libs.LiveStream.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.feyiuremote.R;
import com.feyiuremote.libs.LiveStream.interfaces.ILiveFeedProcessor;
import com.feyiuremote.libs.LiveStream.interfaces.ILiveFeedReceiver;
import com.feyiuremote.libs.LiveStream.interfaces.ILiveFeedUpdateListener;

import java.util.ArrayList;

public class LiveFeedReceiver implements ILiveFeedReceiver {
    private final static String TAG = LiveFeedReceiver.class.getSimpleName();

    private ILiveFeedUpdateListener mStatusListener;
    private final Context context;

    private final ArrayList<RawImage> rawImageCache = new ArrayList<>();
    private final Integer IMAGE_CACHE_MAX = 1;

    private Long frames = 0L;
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
    public void onNewRawFrame(RawImage rawImage) {
        frames++;
        rawImageCache.add(rawImage);

        if (rawImageCache.size() > IMAGE_CACHE_MAX) {
            rawImageCache.remove(0);
        }

        mStatusListener.onUpdate("New frame received:" + frames);
    }

    @Override
    public Bitmap getImage(int index) {
        if (rawImageCache.size() == 0) {
            return BitmapFactory.decodeResource(context.getResources(), R.drawable.video_unavailable);
        }

        index = index >= rawImageCache.size() ? rawImageCache.size() - 1 : index;
        Bitmap cachedImage = rawImageCache.get(index).toBitmap();

        if (this.mImageProcessor != null && cachedImage != null) {
            // This is used to draw on the image (reactangles and shit)
            return mImageProcessor.process(cachedImage);
        } else {
            // Don't apply any effects if mImageProcessor is not available
            return cachedImage;
        }
    }


    @Override
    public void onError(String message) {
        Log.e(TAG, message);
        this.mStatusListener.onUpdate(message);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onWarning(String message) {
        Log.w(TAG, message);
        this.mStatusListener.onUpdate(message);
    }

    @Override
    public void onInfo(String message) {
        Log.d(TAG, message);
        this.mStatusListener.onUpdate(message);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
