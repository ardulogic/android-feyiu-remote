package com.feyiuremote.libs.AI.trackers;

import com.feyiuremote.libs.Utils.Rectangle;

public class POI {

    public Rectangle rect;

//    private final Mat imageMat = new Mat();

    public double health = 100;


    public POI(Rectangle r) {
        this.rect = r;
    }


    /**
     * How much % POI is off target point
     */
    public double calcDeviationPercentage(double point, double axis_width, double target_point, boolean invert_sign) {
        int dev = (int) Math.abs(point - target_point);
        double perc_dev = (double) dev / axis_width;

        if (point < target_point) {
            return invert_sign ? -perc_dev : perc_dev;
        } else {
            return invert_sign ? perc_dev : -perc_dev;
        }
    }

    public double xCenterDevPercFromPoint(int poiDestinationX, boolean invert_sign) {
        return calcDeviationPercentage(rect.centerX(), rect.max_w, poiDestinationX, invert_sign);
    }

    public double yCenterDevPercFromPoint(int poiDestinationY, boolean invert_sign) {
        return calcDeviationPercentage(rect.centerY(), rect.max_h, poiDestinationY, invert_sign);
    }


//    private Scalar mapHealthToColor(int health) {
//        // Map health to color gradient (red to green)
//        int red = (int) (255 * (100 - health) / 100.0);
//        int green = (int) (255 * health / 100.0);
//        int blue = 50; // You can adjust the blue component if needed
//
//        // Ensure color values are in the valid range (0-255)
//        red = Math.max(0, Math.min(255, red));
//        green = Math.max(0, Math.min(255, green));
//        blue = Math.max(0, Math.min(255, blue));
//        // TODO: wrong scaling when POI is drawn on!
//        return new Scalar(blue, green, red); // OpenCV uses BGR color order
//    }
//
//    private void calculateHealth(int nx1, int ny1, int nx2, int ny2) {
//        double oldCenterX = centerX();
//        double oldCenterY = centerY();
//
//        double newCenterX = (nx1 + nx2) / 2.0;
//        double newCenterY = (ny1 + ny2) / 2.0;
//
//        double distance = Math.sqrt(Math.pow(newCenterX - oldCenterX, 2) + Math.pow(newCenterY - oldCenterY, 2));
//
//        // Adjust health based on the distance or any other criteria
//        if (distance / max_w * 100 > 5) {
//            health -= 20;
//        }
//
//        // Ensure health is non-negative
//        health = Math.max(0, health);
//    }

    public void resetHealth() {
        health = 100;
    }


//    public double calculateDistance(Rect rect) {
//        double centerX1 = centerX();
//        double centerY1 = centerY();
//
//        double centerX2 = rect.x + rect.width / 2.0;
//        double centerY2 = rect.y + rect.height / 2.0;
//
//        return Math.sqrt(Math.pow(centerX1 - centerX2, 2) + Math.pow(centerY1 - centerY2, 2));
//    }

//    public void resizeTowards(POI mTargetPOI) {
//        if (mTargetPOI.x1 < x1) x1 -= 1;
//        if (mTargetPOI.x2 < x2) x2 -= 1;
//        if (mTargetPOI.x1 > x1) x1 += 1;
//        if (mTargetPOI.x2 > x2) x2 += 1;
//
//        if (mTargetPOI.y1 < y1) y1 -= 1;
//        if (mTargetPOI.y2 < y2) y2 -= 1;
//        if (mTargetPOI.y1 > y1) y1 += 1;
//        if (mTargetPOI.y2 > y2) y2 += 1;
//    }

    public void update(Rectangle rectangle) {
        this.rect = rectangle;
    }
}

