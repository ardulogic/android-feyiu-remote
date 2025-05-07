package com.feyiuremote.libs.LiveStream.processors.detectors;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;

import com.feyiuremote.MainActivity;
import com.feyiuremote.libs.AI.detectors.abstracts.DetectedObject;
import com.feyiuremote.libs.AI.detectors.abstracts.IObjectDetector;
import com.feyiuremote.libs.AI.detectors.pose.GoogleAccuratePoseDetector;
import com.feyiuremote.libs.AI.detectors.pose.IPoseDetectorListener;
import com.feyiuremote.libs.AI.trackers.POI;
import com.feyiuremote.libs.LiveStream.LiveView.OverlayView;
import com.feyiuremote.libs.LiveStream.processors.detectors.abstracts.ObjectDetectorProcessor;
import com.feyiuremote.libs.Utils.Rectangle;
import com.feyiuremote.ui.camera.models.CameraViewModel;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class GooglePoseDetectorProcessor extends ObjectDetectorProcessor {

    public GooglePoseDetectorProcessor(OverlayView v, MainActivity mainActivity, CameraViewModel cameraViewModel) {
        super(v, mainActivity, cameraViewModel);
    }

    @Override
    protected IObjectDetector createDetector() {
        return new GoogleAccuratePoseDetector();
    }

    @Override
    protected void setDetectorListener(IObjectDetector detector) {
        detector.setListener(new IPoseDetectorListener() {
            @Override
            public void onPoseUpdate(Pose pose, Bitmap detectedBitmap) {
                drawPoseOnOverlay(pose, detectedBitmap);
            }

            @Override
            public void onObjectsDetected(LinkedList<DetectedObject> objects) {

            }
        });
    }

    protected void drawPoseOnOverlay(Pose pose, Bitmap detectedBitmap) {
        ArrayList<Drawable> drawables = new ArrayList<>();

        List<PoseLandmark> landmarks = pose.getAllPoseLandmarks();

        if (landmarks == null || landmarks.isEmpty()) {
            overlayView.clear();
            cancelPOI();
            return;
        }

        landmarks = Arrays.asList(
                pose.getPoseLandmark(PoseLandmark.NOSE),
                pose.getPoseLandmark(PoseLandmark.LEFT_EYE),
                pose.getPoseLandmark(PoseLandmark.RIGHT_EYE)
        );

        float scaleX = (float) (overlayView.getWidth()) / detectedBitmap.getWidth();
        float scaleY = (float) (overlayView.getHeight()) / detectedBitmap.getHeight();

        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;
        float confidence = 0;

        for (PoseLandmark landmark : landmarks) {
            float x = landmark.getPosition().x * scaleX;
            float y = landmark.getPosition().y * scaleY;
            confidence += landmark.getInFrameLikelihood();

            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);

            ShapeDrawable circle = new ShapeDrawable(new OvalShape());
            circle.getPaint().setColor(mapConfidenceToColor(confidence));
            circle.setBounds((int) x - 10, (int) y - 10, (int) x + 10, (int) y + 10);
            drawables.add(circle);
        }

        confidence /= landmarks.size();

        // Need to create a rectangle around all landmarks
        Rectangle rect = new Rectangle(
                (int) minX, (int) minY,
                (int) maxX, (int) maxY,
                overlayView.getWidth(), overlayView.getHeight()
        );

        followPOI(new POI(rect));

        drawables.add(rect.asDrawable(mapConfidenceToColor(confidence), 2));
        overlayView.updateOverlay(drawables);
    }

    public static int mapConfidenceToColor(double confidence) {
        confidence = Math.max(0.0, Math.min(1.0, confidence)); // Clamp between 0 and 1

        int red, green, blue;

        if (confidence < 0.5) {
            // Red to Yellow
            float ratio = (float) (confidence / 0.5);
            red = 255;
            green = (int) (255 * ratio);
            blue = 0;
        } else {
            // Yellow to Light Green (#FFFF00 to #90EE90)
            float ratio = (float) ((confidence - 0.5) / 0.5);
            red = (int) (255 - ratio * (255 - 144));    // 255 → 144
            green = (int) (255 - ratio * (255 - 238));  // 255 → 238
            blue = (int) (0 + ratio * 144);             // 0 → 144
        }

        return Color.rgb(red, green, blue);
    }

    protected void followPOI(POI mPoi) {
        gimbalFollowProcessor.onPoiUpdate(mPoi);
    }

    protected void cancelPOI() {
        gimbalFollowProcessor.stop();
    }

}
