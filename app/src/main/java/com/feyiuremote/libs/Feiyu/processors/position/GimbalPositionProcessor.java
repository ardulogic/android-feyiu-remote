package com.feyiuremote.libs.Feiyu.processors.position;

import android.content.Context;
import android.util.Log;

import com.feyiuremote.libs.Cameras.Panasonic.PanasonicCamera;
import com.feyiuremote.libs.Feiyu.FeyiuControls;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.calibration.CalibrationDbHelper;
import com.feyiuremote.ui.camera.waypoints.Waypoint;

import java.util.ArrayList;

import androidx.lifecycle.MutableLiveData;

public class GimbalPositionProcessor {

    private final CalibrationDbHelper mDb;
    private final String TAG = GimbalPositionProcessor.class.getSimpleName();
    private final Context context;
    private MutableLiveData<PanasonicCamera> camera;

    public boolean isActive = false;

    private GimbalPositionTarget target;

    private IGimbalPositionProcessorListener listener;

    private ArrayList<String> debugLog = new ArrayList<String>();
    private Long pan_start_time = null;
    private Float pan_start_angle = null;

    private Long tilt_start_time = null;
    private Float tilt_start_angle = null;

    private Long pan_end_time = null;
    private Float pan_end_angle = null;
    private Long tilt_end_time = null;
    private Float tilt_end_angle = null;

    private IGimbalPositionProcessorStartedListener startListener;

    public GimbalPositionProcessor(Context context) {
        this.context = context;
        this.mDb = new CalibrationDbHelper(context);
        this.isActive = false;
    }

    public void setCamera(MutableLiveData<PanasonicCamera> camera) {
        this.camera = camera;
    }

    public void setListener(IGimbalPositionProcessorListener listener) {
        this.listener = listener;
    }

    public void setOnStartListener(IGimbalPositionProcessorStartedListener listener) {
        this.startListener = listener;
    }

    public void setTarget(Waypoint waypoint) {
        FeyiuControls.cancelQueuedCommands();
        target = new GimbalPositionTarget(context, waypoint.getPanAngle(), waypoint.getTiltAngle(), waypoint.getPanSpeed(), waypoint.getTiltSpeed(), waypoint.getDwellTimeMs(), waypoint.getFocusPoint());
    }

    public void start() {
        debugLog.clear();
        FeyiuControls.cancelQueuedCommands();

        if (target != null) {
            if (startListener != null) {
                startListener.onStarted();
            }

            FeyiuControls.setPanSensitivity(target.getPanSensitivity());
            FeyiuControls.setTiltSensitivity(target.getTiltSensitivity());

            PanasonicCamera cam = camera != null ? camera.getValue() : null;
            if (cam != null && cam.focusIsAvailable() && target.getFocus() != null) {
                cam.focus.focusTo(target.getFocus());
            }

            Log.i(TAG, "Gimbal position target is active.");
            this.isActive = true;
        } else {
            Log.e(TAG, "Waypoint target not specified.");
        }
    }

    public void cancel() {
        this.isActive = false;
        FeyiuControls.cancelQueuedCommands();
        this.target = null;

        Log.i(TAG, "Target cleared.");
    }

    public void stop() {
        this.isActive = false;
        FeyiuControls.cancelQueuedCommands();
        FeyiuControls.setPanJoy(0);
        FeyiuControls.setTiltJoy(0);
    }

    public GimbalPositionTarget getTarget() {
        return target;
    }

    private void withTargetAvailable(Runnable r) {
        if (target != null) {
            r.run();
        }
    }

    public void queueGimbalCommands() {
        withTargetAvailable(() -> {
            // Wait for other commands to finish
            if (target.panShouldStop() && !target.panIsStopping()) {
                if (!target.panIsOvershooting()) {
                    debugLog.add("Pan Stop in " + (int) target.getPanMovementTime() + "ms (shouldStop=true, isStopping=false)");
                    FeyiuControls.setPanJoyAfter(0, (int) target.getPanMovementTime());
                } else {
                    debugLog.add("Pan (Emergency) Stop ASAP since its overshooting");
                    FeyiuControls.setPanJoy(0);
                }

                target.setPanIsStopping();
                debugLog.add("Pan state is set to stopping");
            } else {
                if (target.panShouldMove()) {
                    FeyiuControls.setPanJoy(target.getPanJoyValue());
                    debugLog.add("Pan should move (" + target.angleDiffInDeg(mDb.AXIS_PAN) + " diff)");
                }
            }

            if (target.tiltShouldStop() && !target.tiltIsStopping()) {
                if (!target.tiltIsOvershooting()) {
                    FeyiuControls.setTiltJoyAfter(0, (int) target.getTiltMovementTime());
                    debugLog.add("Tilt Stop in " + (int) target.getTiltMovementTime() + "ms (shouldStop=true, isStopping=false)");
                } else {
                    FeyiuControls.setTiltJoy(0);
                    debugLog.add("Tilt (Emergency) Stop ASAP since its overshooting");
                }
                target.setTiltIsStopping();
                debugLog.add("Tilt state is set to stopping");
            } else {
                if (target.tiltShouldMove()) {
                    FeyiuControls.setTiltJoy(target.getTiltJoyValue());
                    debugLog.add("Tilt should move (" + target.angleDiffInDeg(mDb.AXIS_TILT) + " diff)");

                }
            }

            if (listener != null) {
                if (target != null && target.tiltIsStopping() && target.panIsStopping()) {
                    if (FeyiuControls.timeSinceLastRequest() > target.dwell_time_ms && FeyiuControls.shouldBeStopped()) {
                        Log.i(TAG, "Target is reached");
                        listener.onTargetReached();
                    }
                }

                // Note that target dissapears on blend when is nearby
                // can cause null exception if used after this
                // maybe its best not to clear the targets, idk
                if (target != null && target.isNearby()) {
                    Log.i(TAG, "Target is nearby");
                    listener.onTargetNearlyReached();
                }
            }
        });
    }

    public void debugCalAccuracy() {
        pan_end_time = System.currentTimeMillis();
        pan_end_angle = FeyiuState.getInstance().angle_pan.value();

        tilt_end_time = System.currentTimeMillis();
        tilt_end_angle = FeyiuState.getInstance().angle_tilt.value();

        long pan_time = Math.abs(pan_end_time - pan_start_time);
        float pan_angle = Math.abs(pan_end_angle - pan_start_angle);
        float pan_speed = (float) pan_angle / pan_time * 1000;
        debugLog.add("Pan Calibration: " + target.getPanSpeedDegPerSec() + " Actual:" + pan_speed);

        long tilt_time = Math.abs(tilt_end_time - tilt_start_time);
        float tilt_angle = Math.abs(tilt_end_angle - tilt_start_angle);
        float tilt_speed = (float) (tilt_angle / tilt_time) * 1000;
        debugLog.add("tilt Calibration: " + target.getTiltSpeedDegPerSec() + " Actual:" + tilt_speed);
    }

    public void onGimbalUpdate() {
        if (this.isActive) {
            queueGimbalCommands();
            Log.d(TAG, "---------WP ACTIVE----------");
        }
    }

    public boolean isAt(double angle_pan, double angle_tilt) {
        GimbalPositionTarget t = new GimbalPositionTarget(context, angle_pan, angle_tilt, 1, 1, 0, null);
        return t.isPositionReached();
    }

}
