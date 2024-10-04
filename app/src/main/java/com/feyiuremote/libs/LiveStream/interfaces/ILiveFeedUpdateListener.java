package com.feyiuremote.libs.LiveStream.interfaces;

import android.graphics.Bitmap;

public interface ILiveFeedUpdateListener {

    void onMessage(String message);

    void onNewFrame(Bitmap frame);

}
