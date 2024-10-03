package com.feyiuremote.libs.LiveStream.interfaces;

import android.graphics.Bitmap;

import com.feyiuremote.libs.LiveStream.image.RawImage;

public interface ILiveFeedUpdateListener {

    void onMessage(String message);

    void onNewFrame(Bitmap frame);

}
