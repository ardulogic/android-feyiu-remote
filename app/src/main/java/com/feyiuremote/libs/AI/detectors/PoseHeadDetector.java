package com.feyiuremote.libs.AI.detectors;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Size;

import com.feyiuremote.libs.AI.detectors.Nanodet.DetectedPose;
import com.feyiuremote.libs.AI.detectors.Nanodet.Nanodet;
import com.feyiuremote.libs.AI.detectors.YoloV8.DetectedObject;

import org.opencv.core.Point;
import org.opencv.core.Rect;

import java.util.concurrent.ExecutorService;

public class PoseHeadDetector extends ObjectDetector implements IObjectDetector {

    private Nanodet nanodet;
    private Size modelImageSize;

    //192 //192x256
    //256 //256x320
    //480 //480x640
    public PoseHeadDetector(ExecutorService executor) {
        super(executor);
    }

    @Override
    public void init(Context context) {
        nanodet = new Nanodet(context);
        nanodet.loadModel("multipose_192", 192, 256);
//        nanodet.loadModel("multipose_256", 256, 320);
    }

    @Override
    public DetectedObject[] detect(Bitmap bitmap) {
        // Assuming 'bitmap' is your original Bitmap
        DetectedPose[] poses = nanodet.detect(bitmap);
        DetectedObject[] objects = new DetectedObject[poses.length];

        int index = 0;
        for (DetectedPose pose : poses) {
            objects[index++] = new DetectedObject(0, pose.face(), null);
        }

        return objects;
    }


}
