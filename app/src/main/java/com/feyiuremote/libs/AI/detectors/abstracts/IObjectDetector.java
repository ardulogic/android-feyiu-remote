package com.feyiuremote.libs.AI.detectors.abstracts;

import android.content.Context;

import com.feyiuremote.libs.Cameras.abstracts.CameraFrame;

public interface IObjectDetector {
    public void init(Context context);

    void onNewFrame(CameraFrame frame);

    void shutdown();

    void stop();

    /**
     * Should call listener.onUpdate when box coordinates are updated
     *
     * @param listener
     */
    void setListener(IObjectDetectorListener listener);
}
