package com.feyiuremote.libs.AI.detectors.Nanodet;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;

import com.feyiuremote.libs.AI.detectors.IObjectDetector;
import com.feyiuremote.libs.AI.detectors.ObjectDetector;
import com.feyiuremote.libs.AI.detectors.YoloV8.DetectedObject;
import com.feyiuremote.libs.AI.detectors.YoloV8.YoloV8;

import java.util.concurrent.ExecutorService;

public class NanodetNcnn {

    public native boolean loadModel(AssetManager mgr, String model_path, int target_size, int cpugpu);

    public native DetectedPose[] detect(long matAddr);

    static {
        System.loadLibrary("ncnnbodypose");
    }


}
