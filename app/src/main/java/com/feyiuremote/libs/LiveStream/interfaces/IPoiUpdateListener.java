package com.feyiuremote.libs.LiveStream.interfaces;

import com.feyiuremote.libs.AI.trackers.POI;


public interface IPoiUpdateListener {

    void onPoiLock(POI poi);

    void onPoiUpdate(POI poi);

    void onPoiCancel();

    void onPoiTargetPositionUpdate(double x_perc, double y_perc);
}
