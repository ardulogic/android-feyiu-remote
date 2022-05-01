package com.feyiuremote.libs.LiveStream.interfaces;

import com.feyiuremote.libs.LiveStream.image.RawImage;

public interface IImageStreamObserver {

    void onNewRawFrame(RawImage rawImage);

    void onError(String message);

    void onWarning(String message);

    void onInfo(String message);

}
