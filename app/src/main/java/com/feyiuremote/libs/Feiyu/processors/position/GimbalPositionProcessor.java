package com.feyiuremote.libs.Feiyu.processors.position;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.feyiuremote.libs.Cameras.Panasonic.PanasonicCamera;
import com.feyiuremote.libs.Feiyu.FeyiuControls;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.calibration.CalibrationDbHelper;
import com.feyiuremote.ui.camera.waypoints.Waypoint;

import java.text.DecimalFormat;

public class GimbalPositionProcessor {

    private final CalibrationDbHelper mDb;
    private final String TAG = GimbalPositionProcessor.class.getSimpleName();
    private final Context context;
    private MutableLiveData<PanasonicCamera> camera;

    public boolean isActive = false;

    private GimbalPositionTarget target;

    private IGimbalPositionProcessorListener listener;

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
        Log.d(TAG, "Starting...");

        FeyiuControls.cancelQueuedCommands();

        if (target != null) {
            if (startListener != null) {
                startListener.onStarted();
            }

            FeyiuControls.setPanSensitivity(target.getPanSensitivity());
            FeyiuControls.setTiltSensitivity(target.getTiltSensitivity());

            PanasonicCamera cam = camera != null ? camera.getValue() : null;
            if (cam != null) {
                // TODO: Fix auto focus so it no longer crashes the system
//                if (target.getFocus() != null) {
//                    cam.focus.focusTo(target.getFocus());
//                } else {
//                    Log.e(TAG, "Could not focus!");
//                }
            }

            Log.d(TAG, "Gimbal position target is active.");
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
        FeyiuControls.setPanJoy(0, "Stopping Gimbal Position Processor (Pan)");
        FeyiuControls.setTiltJoy(0, "Stopping Gimbal Position Processor (Tilt)");
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
            Log.d(TAG, "Queuing gimbal commands...");

            // Wait for other commands to finish
            if (!target.panIsOvershooting()) {
                if (target.panShouldStop()) {
                    Log.d(TAG, "Moving/Stopping in :" + target.getPanMovementTime());
                    FeyiuControls.setPanJoyAfter(0, (int) target.getPanMovementTime(), "Stopping pan in: " + target.getPanMovementTime() + "ms since its close.");
                } else {
                    FeyiuControls.setPanJoy(target.getPanJoyValue(), "Rotating pan to target");
                }
            } else {
                Log.d(TAG, "Pan is overshooting!");
                FeyiuControls.setPanJoy(0, "Pan is overshooting");
            }

            if (!target.tiltIsOvershooting()) {
                if (target.tiltShouldStop()) {
                    FeyiuControls.setTiltJoyAfter(0, (int) target.getTiltMovementTime(), "Stopping tilt in: " + target.getTiltMovementTime() + "ms since its close.");
                } else {
                    FeyiuControls.setTiltJoy(target.getTiltJoyValue(), "Rotating tilt to target");
                }
            } else {
                Log.d(TAG, "Tilt is overshooting!");
                FeyiuControls.setTiltJoy(0, "Tilt is overshooting");
            }

            withTargetAvailable(() -> {
                if (listener != null) {
//                    if (target.isReached()) {
//                        Log.i(TAG, "Target is reached");
//                        listener.onTargetReached(target);
//                        this.isActive = false;
//                    }

                    // Note that target dissapears on blend when is nearby
                    // can cause null exception if used after this
                    // maybe its best not to clear the targets, idk
//                    if (target.isNearby()) {
//                        Log.i(TAG, "Target is nearby");
//                        listener.onTargetNearlyReached();
//                    }
                }
            });
        });

//        log();
    }

    public void debugCalAccuracy() {
        pan_end_time = System.currentTimeMillis();
        pan_end_angle = FeyiuState.getInstance().angle_pan.value();

        tilt_end_time = System.currentTimeMillis();
        tilt_end_angle = FeyiuState.getInstance().angle_tilt.value();

        long pan_time = Math.abs(pan_end_time - pan_start_time);
        float pan_angle = Math.abs(pan_end_angle - pan_start_angle);
        float pan_speed = (float) pan_angle / pan_time * 1000;
        Log.d(TAG, "Pan Calibration: " + target.getPanSpeedDegPerSec() + " Actual:" + pan_speed);

        long tilt_time = Math.abs(tilt_end_time - tilt_start_time);
        float tilt_angle = Math.abs(tilt_end_angle - tilt_start_angle);
        float tilt_speed = (float) (tilt_angle / tilt_time) * 1000;
        Log.d(TAG, "tilt Calibration: " + target.getTiltSpeedDegPerSec() + " Actual:" + tilt_speed);
    }

    public void onGimbalUpdate() {
        if (this.isActive) {
            Log.d(TAG, "---------WP ACTIVE----------");
            queueGimbalCommands();
        }
    }

    public boolean isAt(double angle_pan, double angle_tilt) {
        GimbalPositionTarget t = new GimbalPositionTarget(context, angle_pan, angle_tilt, 1, 1, 0, null);
        return t.isPositionReached();
    }

    public void log() {
        withTargetAvailable(() -> {
            String d = "Gimbal Position Processor (Target Available):";
            d += "\n Is Stopping: P: " + target.panIsStopping() + " T:" + target.tiltIsStopping();
            d += "\n Should Stop: P: " + target.panShouldStop() + " T:" + target.tiltShouldStop();
            d += "\n Oversh-ting: P: " + target.panIsOvershooting() + " T:" + target.tiltIsOvershooting();
            d += "\n Should Move: P: " + target.panShouldMove() + " T:" + target.tiltShouldMove();
            d += "\n Target Axis Reached: P: " + target.isPanReached() + " T:" + target.isTiltReached();
            d += "\n Target Is Reached: " + target.isReached();
            d += "\n Target Is Nearby: " + target.isNearby();

            Log.d(TAG, d);
        });
    }

    @NonNull
    public String toString() {
        DecimalFormat decimalFormat = new DecimalFormat(" #.#; -#.#");
        String d = String.format("%s, %s, %s",
                FeyiuState.getInstance().angle_pan.speedToString(),
                FeyiuState.getInstance().angle_tilt.speedToString(),
                FeyiuState.getInstance().angle_yaw.speedToString());

        if (target != null) {
            d += "\nTime: P:" + Math.round(target.getPanMovementTime()) + " T:" + Math.round(target.getTiltMovementTime());
            d += "\n Overshooting: P:" + target.panIsOvershooting() + " T:" + target.tiltIsOvershooting();
            d += "\n Angle Df: P:" + decimalFormat.format(target.angleDiffInDeg(mDb.AXIS_PAN)) + " T:" + decimalFormat.format(target.angleDiffInDeg(mDb.AXIS_TILT));
        }

        d += FeyiuControls.commandsToString();

        return d;
    }

}
