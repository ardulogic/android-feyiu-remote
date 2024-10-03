package com.feyiuremote.libs.AI.detectors;

import android.content.Context;
import android.graphics.Bitmap;

import com.feyiuremote.libs.AI.detectors.YoloV8.DetectedObject;
import com.feyiuremote.libs.AI.detectors.YoloV8.YoloV8;

import java.util.concurrent.ExecutorService;

public class YoloDetector extends ObjectDetector implements IObjectDetector {

    private YoloV8 yolo;


    public YoloDetector(ExecutorService executor) {
        super(executor);
    }

    @Override
    public void init(Context context) {
        yolo = new YoloV8(context);
        yolo.loadModel("yolov8n-coco");
    }

    @Override
    public DetectedObject[] detect(Bitmap bitmap) {
        return yolo.detect(bitmap);
    }


}
