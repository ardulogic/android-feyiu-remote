package com.feyiuremote.libs.Feiyu.processors;

import com.feyiuremote.libs.AI.trackers.POI;

public interface IGimbalProcessor {


    void onPoiUpdate(POI poi);


    void stop();

}
