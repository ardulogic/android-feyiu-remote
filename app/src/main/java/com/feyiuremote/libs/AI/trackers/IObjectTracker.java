package com.feyiuremote.libs.AI.trackers;

import com.feyiuremote.libs.Cameras.abstracts.CameraFrame;
import com.feyiuremote.libs.Utils.Rectangle;

public interface IObjectTracker {
    /**
     * This should track the drawn box
     *
     * @param frame
     * @return
     */
    void onNewFrame(CameraFrame frame);

    /**
     * This is called when user draws the box
     * rectangle has properties x1, x2, y1, y2
     *
     * @param rectangle
     */
    void lock(Rectangle rectangle);

    /**
     * Should call listener.onUpdate when box coordinates are updated
     *
     * @param listener
     */
    void setListener(IObjectTrackerListener listener);

    /**
     * Cancel box trtacking
     */
    void stop();

}
