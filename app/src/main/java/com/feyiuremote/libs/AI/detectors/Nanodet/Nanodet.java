package com.feyiuremote.libs.AI.detectors.Nanodet;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.Size;

import com.feyiuremote.libs.AI.detectors.IObjectDetector;
import com.feyiuremote.libs.AI.detectors.ObjectDetector;
import com.feyiuremote.libs.AI.detectors.YoloV8.DetectedObject;
import com.feyiuremote.libs.AI.detectors.YoloV8.YoloV8;
import com.feyiuremote.libs.AI.detectors.YoloV8.Yolov8Ncnn;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * This is a pose detector presumably faster than google or yolo
 */
public class Nanodet {

    private NanodetNcnn nanodetNcnn = new NanodetNcnn();

    private Context context;

    private int USE_CPU = 0;
    private int USE_GPU = 1;

    private Size modelImageSize;

    public Nanodet(Context context) {
        this.context = context;
    }

    public void loadModel(String model, int model_img_width, int model_img_height) {
        modelImageSize = new Size(model_img_width, model_img_height);
        boolean ret_init = nanodetNcnn.loadModel(context.getAssets(), model, model_img_width, USE_GPU);

        if (!ret_init) {
            Log.e("MainActivity", "Nanodet loadmodel failed");
        }
    }

    /**
     * This method automatically resizes input bitmap
     * and returned poses
     *
     * @param bitmap Any bitmap
     * @return DetectedPose[] Rescaled poses
     */
    public DetectedPose[] detect(Bitmap bitmap) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, modelImageSize.getWidth(), modelImageSize.getHeight(), true);

        Mat mat = new Mat();
        Utils.bitmapToMat(resizedBitmap, mat);

        // Convert the Mat to a 3-channel (RGB) Mat
        Mat convertedMat = new Mat();
        Imgproc.cvtColor(mat, convertedMat, Imgproc.COLOR_RGBA2RGB);


        // Detect objects
        DetectedPose[] poses = nanodetNcnn.detect(convertedMat.getNativeObjAddr());

        for (DetectedPose pose : poses) {
            pose.rescale(modelImageSize.getWidth(), modelImageSize.getHeight(), bitmap.getWidth(), bitmap.getHeight());
        }

        return poses;
    }

}
