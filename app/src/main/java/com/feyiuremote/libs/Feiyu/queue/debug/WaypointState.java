package com.feyiuremote.libs.Feiyu.queue.debug;

import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.calibration.CalibrationDB;
import com.feyiuremote.libs.Feiyu.processors.position.GimbalPositionTarget;

public class WaypointState {

    public double targetTiltAngle;
    public double targetPanAngle;
    public Long timeSinceGimbalUpdate;
    public Long tiltTime;
    public Long panTime;
    public double tiltCalSpeed;
    public double panCalSpeed;
    public double panAngleDiff;
    public double tiltAngleDiff;
    public Long timestamp;

    public WaypointState(GimbalPositionTarget target) {
        tiltCalSpeed = target.getTiltSpeedDegPerSec();
        panCalSpeed = target.getTiltSpeedDegPerSec();
        targetPanAngle = target.getPanTargetAngle();
        targetTiltAngle = target.getTiltTargetAngle();
        panAngleDiff = target.angleDiffInDeg(CalibrationDB.AXIS_PAN);
        tiltAngleDiff = target.angleDiffInDeg(CalibrationDB.AXIS_TILT);
        timeSinceGimbalUpdate = FeyiuState.getInstance().getTimeSinceLastUpdateMs();
        tiltTime = (long) target.getTiltMovementTime();
        panTime = (long) target.getPanMovementTime();

        timestamp = System.currentTimeMillis();
    }
}
