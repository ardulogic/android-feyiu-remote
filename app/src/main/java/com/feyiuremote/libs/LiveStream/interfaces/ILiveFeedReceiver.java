package com.feyiuremote.libs.LiveStream.interfaces;

import android.graphics.Bitmap;

import com.feyiuremote.libs.Cameras.abstracts.CameraFrame;

public interface ILiveFeedReceiver {
    void onNewFrame(CameraFrame frame);

    Bitmap getFrameBitmap();

    void setUpdateListener(ILiveFeedUpdateListener statusListener);

    void onError(String message);

    void onWarning(String message);

    void onInfo(String message);
}
