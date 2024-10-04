package com.feyiuremote.libs.LiveStream.interfaces;

import android.graphics.Bitmap;

public interface IImageStreamObserver {

    void onNewFrame(Bitmap image);

    void onError(String message);

    void onWarning(String message);

    void onInfo(String message);

}
