package com.feyiuremote.libs.LiveStream.interfaces;

import android.graphics.Bitmap;

import com.feyiuremote.libs.LiveStream.image.RawImage;

public interface ILiveFeedReceiver {
    void onNewRawFrame(RawImage rawImage);

    public void setUpdateListener(ILiveFeedUpdateListener statusListener);

    Bitmap getImage();

    Long getImageTimestamp();

    void onError(String message);

    void onWarning(String message);

    void onInfo(String message);
}
