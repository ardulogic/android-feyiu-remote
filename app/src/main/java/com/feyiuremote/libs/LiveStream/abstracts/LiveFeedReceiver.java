package com.feyiuremote.libs.LiveStream.abstracts;

import androidx.viewbinding.ViewBinding;

import com.feyiuremote.libs.Cameras.abstracts.CameraFrame;

public abstract class LiveFeedReceiver {

    CameraFrame frame = null;

    long framesReceived = 0L;

    public void onNewFrame(CameraFrame frame) {
        this.frame = frame;
        if (frame.bitmap() != null) framesReceived++;
    }

    public long getFramesReceivedCount() {
        return framesReceived;
    }

    public CameraFrame getFrame() {
        return frame;
    }


    public abstract void onError(String message);

    public abstract void onWarning(String message);

    public abstract void onInfo(String message);

    public abstract void onStop(String message);

    public abstract void setBinding(ViewBinding binding);
}
