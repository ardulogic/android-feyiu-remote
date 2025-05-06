package com.feyiuremote.libs.Feiyu.calibration;

import com.feyiuremote.libs.Feiyu.Axes;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.calibration.commands.GimbalCommand;

public class CalibrationState {

    public double pan_angle;
    public double tilt_angle;
    public double pan_speed = 0;
    public double tilt_speed = 0;

    public long timestamp;

    public CalibrationState(Long timestamp) {
        pan_angle = FeyiuState.getInstance().angle_pan.value();
        tilt_angle = FeyiuState.getInstance().angle_tilt.value();

        this.timestamp = timestamp;
    }

    public CalibrationState(GimbalCommand command) {
        pan_angle = command.panAngleAtExecution;
        tilt_angle = command.tiltAngleAtExecution;

        timestamp = command.timeExecuted;
    }

    public void calculateSpeed(CalibrationState previousState) {
        long timeDiff = this.timestamp - previousState.timestamp;

        if (timeDiff == 0) {
            // Avoid division by zero
            this.pan_speed = 0;
            this.tilt_speed = 0;
            return;
        }

        double panAngleDiff = this.pan_angle - previousState.pan_angle;
        double tiltAngleDiff = this.tilt_angle - previousState.tilt_angle; // You had pan_angle again — fixed here.

        // speed = angle difference / time difference (converted to seconds)
        this.pan_speed = panAngleDiff / (timeDiff / 1000.0);  // degrees per second
        this.tilt_speed = tiltAngleDiff / (timeDiff / 1000.0);
    }

    public double getSpeed(Axes.Axis axis) {
        if (axis.equals(Axes.Axis.PAN)) {
            return Math.abs(this.pan_speed);
        } else {
            return Math.abs(this.tilt_speed);
        }
    }

}
