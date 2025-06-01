package com.feyiuremote.libs.AI.detectors;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.feyiuremote.R;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class FaceDetector {

    private static CascadeClassifier mCascadeClassifier;
    private static final String TAG = "FaceDetector";

    /**
     * The following code loads face detection model
     *
     * @param activity
     */
    public static void init(Activity activity) {
        if (mCascadeClassifier != null) {
            // Model already loaded
            return;
        }
        InputStream is = activity.getResources().openRawResource(R.raw.lbpcascade_frontalface_improved);
        File cascadeDir = activity.getDir("cascade", Context.MODE_PRIVATE);
        File cascadeFile = new File(cascadeDir, "lbpcascade_frontalface_improved.xml");
        try {
            FileOutputStream os = new FileOutputStream(cascadeFile);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0,  bytesRead);
            }

            is.close();
            os.close();

            mCascadeClassifier = new CascadeClassifier(cascadeFile.getAbsolutePath());
            if (mCascadeClassifier.empty()) {
                Log.e(TAG, "Failed to load cascade classifier from " + cascadeFile.getAbsolutePath());
                mCascadeClassifier = null;
            } else {
                Log.i(TAG, "Cascade classifier loaded successfully from " + cascadeFile.getAbsolutePath());
            }

        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException while loading cascade classifier: " + cascadeFile.getAbsolutePath(), e);
        } catch (IOException e) {
            Log.e(TAG, "IOException while loading cascade classifier: " + e.getMessage(), e);
        }
    }

    public static Bitmap detect(Bitmap bitmap) {
        if (mCascadeClassifier == null) {
            System.err.println("FaceDetector: CascadeClassifier not initialized. Call init() first.");
            return bitmap; // Return original bitmap if model not loaded
        }
        Mat imageMatrix = new Mat (bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap, imageMatrix);

        MatOfRect faces = new MatOfRect();
        mCascadeClassifier.detectMultiScale(imageMatrix, faces);

        for (Rect face : faces.toArray()) {
            Imgproc.rectangle(imageMatrix,
                    new Point(face.x, face.y),
                    new Point(face.x + face.width, face.y + face.height),
                    new Scalar(0,0,255), 3
            );
        }

        Bitmap bitmapOut = Bitmap.createBitmap(imageMatrix.cols(), imageMatrix.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imageMatrix, bitmapOut);

        return bitmapOut;
    }

}
