package com.feyiuremote.ui.camera.waypoints;

import android.graphics.Bitmap;

import java.util.Objects;
import java.util.UUID;

public class Waypoint {

    // Currently just used in serialization
    public String id;

    public double panSpeed;
    public double tiltSpeed;

    // We cant serialize bitmap as it is, "transient" excludes field
    private transient Bitmap waypointImage;
    public double anglePan;
    public double angleTilt;
    public int angleSpeed;
    public Double focusPoint;

    public int dwellTimeMs;

    private transient boolean active = false;
    private boolean isSaved;

    public Waypoint(Bitmap bitmap, float anglePan, float angleTilt, int angleSpeed, int dwell_time_ms, Double focusPoint) {
        this.waypointImage = bitmap;
        this.anglePan = anglePan;
        this.angleTilt = angleTilt;
        this.angleSpeed = angleSpeed;
        this.dwellTimeMs = dwell_time_ms;
        this.focusPoint = focusPoint;
        this.setId();
    }

    /**
     * This is needed to break references when tracking changes
     * within waypoint
     *
     * @param w
     */
    public Waypoint(Waypoint w) {
        this.waypointImage = w.getWaypointImage();
        this.anglePan = w.anglePan;
        this.angleTilt = w.angleTilt;
        this.angleSpeed = w.angleSpeed;
        this.dwellTimeMs = w.dwellTimeMs;
        this.focusPoint = w.focusPoint;
        this.active = w.isActive();
        this.id = w.id;
    }

    public Waypoint() {
        // Used for deserializing
        this.setId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Waypoint)) return false;

        Waypoint w = (Waypoint) o;

        // Compare only the fields that define “content” for UI-diffing.
        // Exclude transient Bitmap + `active` flag because they change often
        // and Bitmap equality is heavy / undefined.
        return angleSpeed == w.angleSpeed &&
                dwellTimeMs == w.dwellTimeMs &&
                Double.compare(w.anglePan, anglePan) == 0 &&
                Double.compare(w.angleTilt, angleTilt) == 0 &&
                Objects.equals(id, w.id) &&
                Objects.equals(focusPoint, w.focusPoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, anglePan, angleTilt, angleSpeed, dwellTimeMs, focusPoint);
    }

    public void setId() {
        this.id = UUID.randomUUID().toString();
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public Bitmap getWaypointImage() {
        return this.waypointImage;
    }

    public double getPanAngle() {
        return anglePan;
    }

    public double getTiltAngle() {
        return angleTilt;
    }

    public int getAngleSpeed() {
        return angleSpeed;
    }

    public void setAngleSpeed(int speed) {
        this.angleSpeed = speed;
    }

    public float getPanSpeed() {
        return this.getAngleSpeed();
    }

    public float getTiltSpeed() {
        return this.getAngleSpeed();
    }

    public boolean hasFocusPoint() {
        return this.focusPoint != null;
    }

    public Double getFocusPoint() {
        return this.focusPoint;
    }

    public void setFocus(double position) {
        this.focusPoint = position;
    }

    public void setImage(Bitmap b) {
        this.waypointImage = b;
    }

    public int getDwellTimeMs() {
        return this.dwellTimeMs;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setDwellTime(int dwellTimeMs) {
        this.dwellTimeMs = dwellTimeMs;
    }

    public void setPanAngle(double panAngle) {
        this.anglePan = panAngle;
    }

    public void setTiltAngle(double tiltAngle) {
        this.angleTilt = tiltAngle;
    }

}
