package com.feyiuremote.libs.AI.detectors.abstracts;

import java.util.LinkedList;

public interface IObjectDetectorListener {

    public void onObjectsDetected(LinkedList<DetectedObject> objects);

}
