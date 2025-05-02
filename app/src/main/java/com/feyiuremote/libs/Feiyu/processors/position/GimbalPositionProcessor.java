package com.feyiuremote.libs.Feiyu.processors.position;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.feyiuremote.libs.Cameras.Panasonic.PanasonicCamera;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.calibration.CalibrationDB;
import com.feyiuremote.libs.Feiyu.queue.FeyiuCommands;
import com.feyiuremote.libs.Feiyu.queue.FeyiuCommandQueue;
import com.feyiuremote.libs.Feiyu.queue.debug.QueueStopDebugger;
import com.feyiuremote.ui.camera.waypoints.Waypoint;

import java.text.DecimalFormat;
import java.util.concurrent.Executors;

public class GimbalPositionProcessor {

    private final CalibrationDB mDb;
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
        this.mDb = new CalibrationDB(context);
    }

    protected void setActive(boolean value) {
        this.isActive = value;
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
        FeyiuCommandQueue.cancelQueuedCommands();
        target = new GimbalPositionTarget(context, waypoint.getPanAngle(), waypoint.getTiltAngle(), waypoint.getPanSpeed(), waypoint.getTiltSpeed(), waypoint.getDwellTimeMs(), waypoint.getFocusPoint());
    }

    public void clearTarget() {
        target = null;
    }

    public void start() {
        Log.d(TAG, "Starting...");

        FeyiuCommandQueue.cancelQueuedCommands();

        if (target != null) {
            if (startListener != null) {
                startListener.onStarted();
            }
            FeyiuCommands.setTiltSensitivity(target.getTiltSensitivity());
            FeyiuCommands.setPanSensitivity(target.getPanSensitivity());
//            FeyiuControls.setTiltSensitivity(target.getTiltSensitivity());

            PanasonicCamera cam = camera != null ? camera.getValue() : null;

            if (cam != null) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    if (target.getFocus() != null) {
                        cam.focus.focusTo(target.getFocus());
                    } else {
                        Log.e(TAG, "Could not focus!");
                    }
                    ;
                });
            }

            Log.d(TAG, "Gimbal position target is active.");
            setActive(true);
        } else {
            Log.e(TAG, "Waypoint target not specified.");
        }
    }

    public void cancel() {
        setActive(false);

//        FeyiuControls.cancelQueuedCommands();
        FeyiuCommands.clearAll();
        this.target = null;

        Log.i(TAG, "Target cleared.");
    }

    public void stop() {
        setActive(false);

        FeyiuCommands.clearAll();
        FeyiuCommands.setPanJoy(0);
        FeyiuCommands.setTiltJoy(0);
//        FeyiuControls.cancelQueuedCommands();
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
            if (!target.panIsStopping()) {
                if (target.panShouldStop()) {
                    Log.d(TAG, "Pan should stop in " + target.getPanMovementTime());
                    QueueStopDebugger.onPanStartStopping(target);

                    if (target.getPanMovementTime() < 0) {
                        Log.e(TAG, "Pan movement time became negative: " + target.getPanMovementTime());
                        FeyiuCommands.setPanJoyAfter(0, 100, FeyiuCommands.SHORT);
                    } else {
                        FeyiuCommands.setPanJoyAfter(0, (int) target.getPanMovementTime(), FeyiuCommands.SHORT);
                    }

                    target.setPanIsStopping();
                } else {
                    Log.d(TAG, "Pan is moving regularly to target....");
                    FeyiuCommands.setPanJoyFor(target.getPanJoyValue(), FeyiuCommands.REGULAR);
                }
            } else if (!target.isPanReached()) {
                if (FeyiuState.joy_val_pan == 0) {
                    target.setPanHasReached();
                }
            }

            if (!target.tiltIsStopping()) {
                if (target.tiltShouldStop()) {
                    Log.d(TAG, "Tilt should stop in " + target.getTiltMovementTime());
                    QueueStopDebugger.onTiltStartStopping(target);

                    if (target.getTiltMovementTime() < 0) {
                        Log.e(TAG, "Tilt movement time became negative:" + target.getTiltMovementTime());
                        FeyiuCommands.setTiltJoyAfter(0, 100, FeyiuCommands.SHORT);
                    } else {
                        FeyiuCommands.setTiltJoyAfter(0, (int) target.getTiltMovementTime(), FeyiuCommands.SHORT);
                    }

                    target.setTiltIsStopping();
                } else {
                    Log.d(TAG, "Tilt is moving regularly to target....");
                    FeyiuCommands.setTiltJoyFor(target.getTiltJoyValue(), FeyiuCommands.REGULAR);
                }
            } else if (!target.isTiltReached()) {
                if (FeyiuState.joy_val_tilt == 0) {
                    target.setTiltHasReached();
                }
            }


            if (listener != null) {
                if (target.isReached()) {

                    Log.i(TAG, "Target is reached");
                    QueueStopDebugger.onTargetReached(target);
                    listener.onTargetReached(target);

                    // Note that target dissapears on blend when is nearby
                    // can cause null exception if used after this
                    // maybe its best not to clear the targets, idk
//                    if (target.isNearby()) {
//                        Log.i(TAG, "Target is nearby");
//                        listener.onTargetNearlyReached();
//                    }
                }
            }

//            log();
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

    /**
     * Used by waypoint algo to determine where the system is at
     *
     * @param angle_pan
     * @param angle_tilt
     * @return
     */
    public boolean isAt(double angle_pan, double angle_tilt) {
        GimbalPositionTarget t = new GimbalPositionTarget(context, angle_pan, angle_tilt, 1, 1, 0, null);
        return t.isPositionReached();
    }

    public void log() {
        withTargetAvailable(() -> {
            String d = "Gimbal Position Processor (Target Available):";
            d += "\n Is Stopping: P: " + target.panIsStopping() + " T:" + target.tiltIsStopping();
            d += "\n Should Stop: P: " + target.panShouldStop() + " T:" + target.tiltShouldStop();
            d += "\n Target Axis Reached: P: " + target.isPanReached() + " T:" + target.isTiltReached();
            d += "\n Target Is Reached: " + target.isReached();

            Log.d(TAG, d);
        });
    }

    @NonNull
    public String toString() {
        DecimalFormat decimalFormat = new DecimalFormat(" #.#; -#.#");
//        String d = String.format("%s, %s, %s",
//                FeyiuState.getInstance().angle_pan.speedToString(),
//                FeyiuState.getInstance().angle_tilt.speedToString(),
//                FeyiuState.getInstance().angle_yaw.speedToString());

        String d = "";

        if (target != null) {
            d += "\nTime: P:" + Math.round(target.getPanMovementTime()) + " T:" + Math.round(target.getTiltMovementTime());
            d += "\n Angle Df: P:" + decimalFormat.format(target.angleDiffInDeg(mDb.AXIS_PAN)) + " T:" + decimalFormat.format(target.angleDiffInDeg(mDb.AXIS_TILT));
        }

        d += FeyiuCommandQueue.asString();

        return d;
    }

}
