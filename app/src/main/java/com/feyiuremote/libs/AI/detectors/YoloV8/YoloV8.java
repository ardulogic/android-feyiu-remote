package com.feyiuremote.libs.AI.detectors.YoloV8;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class YoloV8 {
    private Yolov8Ncnn yolov8ncnn = new Yolov8Ncnn();

    private Context context;

    private int USE_CPU = 0;
    private int USE_GPU = 1;

    public YoloV8(Context context) {
        this.context = context;
    }

    public void loadModel(String model) {
        boolean ret_init = yolov8ncnn.loadModel(context.getAssets(), model, USE_GPU);

        if (!ret_init) {
            Log.e("MainActivity", "yolov8ncnn loadModel failed");
        }
    }

    public DetectedObject[] detect(Bitmap bitmap) {
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);

        // Convert the Mat to a 3-channel (RGB) Mat
        Mat convertedMat = new Mat();
        Imgproc.cvtColor(mat, convertedMat, Imgproc.COLOR_RGBA2RGB);


        // Detect objects
        return yolov8ncnn.detect(convertedMat.getNativeObjAddr());
    }


    public static Bitmap resizeBitmap(Bitmap originalBitmap, int newWidth, int newHeight) {
        // Calculate the new height based on the scale factor

        // Create a new bitmap with the desired width and height
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);

        return resizedBitmap;
    }

}
