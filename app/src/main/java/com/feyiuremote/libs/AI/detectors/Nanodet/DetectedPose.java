package com.feyiuremote.libs.AI.detectors.Nanodet;

import org.opencv.core.Point;
import org.opencv.core.Rect;

public class DetectedPose {
    public float right_eye_y;
    public float right_eye_x;
    public float left_eye_y;
    public float left_eye_x;
    public float nose_x;
    public float nose_y;

    public DetectedPose(float n_x, float n_y, float le_x, float le_y, float re_x, float re_y) {
        this.nose_x = n_x;
        this.nose_y = n_y;

        this.left_eye_x = le_x;
        this.left_eye_y = le_y;

        this.right_eye_x = le_x;
        this.right_eye_y = le_y;
    }

    public Rect face() {
        // Calculate the center of the eyes and nose
        float centerX = (left_eye_x + right_eye_x + nose_x) / 3;
        float centerY = (left_eye_y + right_eye_y + nose_y) / 3;

        // Calculate the top-left and bottom-right corners of the face rectangle
        int left = (int) (centerX - 40);
        int top = (int) (centerY - 40);
        int right = (int) (centerX + 40);
        int bottom = (int) (centerY + 40);

        // Create and return the face rectangle
        return new Rect(new Point(left, top), new Point(right, bottom));
    }

    public void rescale(int model_w, int model_h, int target_w, int target_h) {
        // Calculate scaling factors for x and y
        float scaleX = (float) target_w / model_w;
        float scaleY = (float) target_h / model_h;

        // Rescale facial feature coordinates
        left_eye_x *= scaleX;
        left_eye_y *= scaleY;
        right_eye_x *= scaleX;
        right_eye_y *= scaleY;
        nose_x *= scaleX;
        nose_y *= scaleY;
    }

}