package com.feyiuremote.libs.LiveStream.interfaces;

import android.graphics.Bitmap;

public interface ILiveFeedReceiver {
    void onNewFrame(Bitmap bitmap);

    public void setUpdateListener(ILiveFeedUpdateListener statusListener);

    Bitmap getImage();

    Long getImageTimestamp();

    void onError(String message);

    void onWarning(String message);

    void onInfo(String message);
}
