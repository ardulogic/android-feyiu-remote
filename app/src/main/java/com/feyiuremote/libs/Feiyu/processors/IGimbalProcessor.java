package com.feyiuremote.libs.Feiyu.processors;

import com.feyiuremote.libs.AI.trackers.POI;

public interface IGimbalProcessor {

    void onPoiLock();

    void onPoiUpdate(POI poi);

    void updatePoiDestination(double x_perc, double y_perc);

    void cancel();

}
