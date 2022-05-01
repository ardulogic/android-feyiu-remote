package com.feyiuremote.libs.LiveStream.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.feyiuremote.R;
import com.feyiuremote.libs.Feiyu.processors.IGimbalProcessor;
import com.feyiuremote.libs.LiveStream.interfaces.ILiveFeedProcessor;
import com.feyiuremote.libs.LiveStream.interfaces.ILiveFeedReceiver;
import com.feyiuremote.libs.LiveStream.interfaces.ILiveFeedStatusListener;

import java.util.ArrayList;

public class LiveFeedReceiver implements ILiveFeedReceiver {
    private final static String TAG = LiveFeedReceiver.class.getSimpleName();

    private final ILiveFeedStatusListener statusListener;
    private final Context context;

    private final ArrayList<RawImage> rawImageCache = new ArrayList<>();
    private final Integer IMAGE_CACHE_MAX = 1;

    private Long frames = 0L;
    private ILiveFeedProcessor mImageProcessor;
    private IGimbalProcessor mGimbalProcessor;

    public LiveFeedReceiver(Context context, ILiveFeedStatusListener statusListener) {
        this.context = context;
        this.statusListener = statusListener;
    }

    public void setImageProcessor(ILiveFeedProcessor processor) {
        this.mImageProcessor = processor;
    }

    public void setGimbalProcessor(IGimbalProcessor processor) {
        this.mGimbalProcessor = processor;
    }

    @Override
    public void onNewRawFrame(RawImage rawImage) {
        frames++;
        rawImageCache.add(rawImage);

        if (rawImageCache.size() > IMAGE_CACHE_MAX) {
            rawImageCache.remove(0);
        }

        statusListener.onProgress("New frame received:" + frames);
    }

    @Override
    public Bitmap getImage(int index) {
        Bitmap bitmap;

        if (rawImageCache.size() == 0) {
            return BitmapFactory.decodeResource(context.getResources(), R.drawable.video_unavailable);
        }

        index = index >= rawImageCache.size() ? rawImageCache.size() - 1 : index;

//        return rawImageCache.get(index).toBitmap();

        if (this.mImageProcessor != null) {
            bitmap = mImageProcessor.process(rawImageCache.get(index).toBitmap());
        } else {
            bitmap = rawImageCache.get(index).toBitmap();
        }

        return bitmap;
    }

    @Override
    public void onError(String message) {
        Log.e(TAG, message);
        this.statusListener.onProgress(message);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onWarning(String message) {
        Log.w(TAG, message);
        this.statusListener.onProgress(message);
    }

    @Override
    public void onInfo(String message) {
        Log.d(TAG, message);
        this.statusListener.onProgress(message);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
