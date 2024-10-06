package com.feyiuremote.libs.AI.detectors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.feyiuremote.libs.AI.ObjectUtils;
import com.feyiuremote.libs.AI.trackers.POI;

import org.opencv.core.Rect;

import java.util.concurrent.ExecutorService;

abstract public class ObjectDetector {
    public final static String TAG = ObjectDetector.class.getSimpleName();
    private final ExecutorService executor;
    DetectedObject[] detectedObjects;
    private boolean detecting;

    public ObjectDetector(ExecutorService executor) {
        this.executor = executor;
    }

    public abstract void init(Context context);

    public void onNewFrame(Bitmap bitmap) {
        if (!detecting) {
            try {
                detecting = true;
                detectedObjects = detect(bitmap);
                detecting = false;
            } catch (NullPointerException e) {
                Log.e(TAG, "Could not rescale received pose, probably null");
            }
        } else {
            Log.i(TAG, "Detection skipped, its still detecting...");
        }
    }

    abstract DetectedObject[] detect(Bitmap bitmap);

    public DetectedObject[] getDetectedObjects() {
        return detectedObjects;
    }

    public Bitmap draw(Bitmap bitmap) {
        if (detectedObjects != null && detectedObjects.length > 0) {
            Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

            // Draw rectangles on the bitmap
            Canvas canvas = new Canvas(mutableBitmap);
            Paint paint = new Paint();
            paint.setColor(Color.LTGRAY);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.0f);

            try {
                for (DetectedObject obj : detectedObjects) {
                    if (obj.label == 0) {
                        canvas.drawRect(ObjectUtils.cvRectToAndroidRect(obj.rect), paint);
                    }
                }
            } catch (NullPointerException e) {
                Log.e(TAG, "Detected object no longer exists");
                return bitmap;
            }
//            Log.d("YoloV8", "Objects detected and rectangles drawn");

            return mutableBitmap;
        }

        return bitmap;
    }

    public DetectedObject getClosestDetectedObjectToPoi(int label_index, POI mPOI) {
        if (detectedObjects != null && detectedObjects.length > 0) {
            DetectedObject closestObject = null;
            double minDistance = Double.MAX_VALUE;

            for (DetectedObject obj : detectedObjects) {
                if (obj.label == label_index) {
                    if (mPOI != null) {
//                        double distance = mPOI.calculateDistance(obj.rect);
//                        if (distance < minDistance) {
//                            minDistance = distance;
//                            closestObject = obj;
//                        }
                    } else {
                        return obj;
                    }
                }
            }

            if (closestObject != null) {
                Log.d(TAG, "Closest object found: Label " + closestObject.label + ", Distance: " + minDistance);
                // Do something with the closest object, for example, update the POI or perform other actions
                return closestObject;
            } else {
                Log.d(TAG, "No objects found with label " + label_index);
            }
        }

        return null;
    }

//    public boolean poiOverlapsWithObject(DetectedObject obj, POI poi) {
//        double overlap = calculateOverlap(obj.rect, poi.toOpenCVRect());
//        double overlapReversed = calculateOverlap(poi.toOpenCVRect(), obj.rect);
//        double minOverlap = 0.8; // 90% overlap threshold
//
//        if ((overlap >= minOverlap && overlapReversed >= minOverlap)) {
//            Log.d(TAG, "POI overlaps with object: Label " + obj.label + ", Overlap: " + overlap + " Reversed:" + overlapReversed);
//            // Do something with the overlapping object, for example, update the POI or perform other actions
//            return true;
//        }
//
//        return false;
//    }

    private double calculateOverlap(Rect rect1, Rect rect2) {
        double intersectionWidth = Math.min(rect1.x + rect1.width, rect2.x + rect2.width) - Math.max(rect1.x, rect2.x);
        double intersectionHeight = Math.min(rect1.y + rect1.height, rect2.y + rect2.height) - Math.max(rect1.y, rect2.y);

        if (intersectionWidth <= 0 || intersectionHeight <= 0) {
            // No overlap
            return 0.0;
        }

        double intersectionArea = intersectionWidth * intersectionHeight;
        double rect2Area = rect2.width * rect2.height;

        return intersectionArea / rect2Area;
    }
}
