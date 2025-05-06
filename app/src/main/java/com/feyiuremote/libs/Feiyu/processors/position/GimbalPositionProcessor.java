package com.feyiuremote.libs.Feiyu.processors.position;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.feyiuremote.libs.Cameras.Panasonic.PanasonicCamera;
import com.feyiuremote.libs.Feiyu.Axes;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.calibration.CalibrationDB;
import com.feyiuremote.libs.Feiyu.queue.FeyiuCommandQueue;
import com.feyiuremote.libs.Feiyu.queue.FeyiuCommands;
import com.feyiuremote.libs.Feiyu.queue.debug.PositioningDebugger;
import com.feyiuremote.libs.Utils.Debugger;
import com.feyiuremote.ui.camera.waypoints.Waypoint;

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

        this.mDb = CalibrationDB.get();
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
        target = new GimbalPositionTarget(waypoint.getPanAngle(), waypoint.getTiltAngle(), waypoint.getPanSpeed(), waypoint.getTiltSpeed(), waypoint.getDwellTimeMs(), waypoint.getFocusPoint());
    }

    public void clearTarget() {
        target = null;
    }

    public void start() {
        Log.d(TAG, "Starting...");

        FeyiuCommandQueue.cancelQueuedCommands();
        PositioningDebugger.init();

        if (target != null) {
            if (startListener != null) {
                startListener.onStarted();
            }
            FeyiuCommands.setTiltSensitivity(target.getSensitivity(Axes.Axis.TILT));
            FeyiuCommands.setPanSensitivity(target.getSensitivity(Axes.Axis.PAN));

            PanasonicCamera cam = camera != null ? camera.getValue() : null;

            if (cam != null) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    if (target.getFocus() != null) {
                        cam.focus.focusTo(target.getFocus());
                    } else {
                        Log.e(TAG, "Could not focus!");
                    }
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

        FeyiuCommands.clearAll();
        this.target = null;

        Log.i(TAG, "Target cleared.");
    }

    public void stop() {
        setActive(false);

        FeyiuCommands.clearAll();
        FeyiuCommands.setPanJoy(0);
        FeyiuCommands.setTiltJoy(0);
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
        Debugger.start();
        withTargetAvailable(() -> {
            processAxis(Axes.Axis.PAN);
            processAxis(Axes.Axis.TILT);

            if (listener != null) {
                if (target.isReached()) {
                    if (FeyiuState.getInstance().isStationary()) {
                        Log.i(TAG, "Target is reached");
                        PositioningDebugger.onTargetReached(target);
                        listener.onTargetReached(target);
                    }

                    // Note that target dissapears on blend when is nearby
                    // can cause null exception if used after this
                    // maybe its best not to clear the targets, idk
//                    if (target.isNearby()) {
//                        Log.i(TAG, "Target is nearby");
//                        listener.onTargetNearlyReached();
//                    }
                }
            }
        });
    }

    private void processAxis(Axes.Axis axis) {
        String axisName = Axes.toString(axis);

        if (target.isReachedOn(axis)) {
            return; // Target has been reached, nothing to do
        }

        if (target.isStoppingOn(axis)) { // Target was stopping
            if (FeyiuState.getAngle(axis).isStationary()) { // Target no longer moving
                PositioningDebugger.onAxisStopped(axis, target);
                target.setHasReachedOn(axis);
            }
            return;
        }

        // Target is not stopping and not reached, but should stop:
        if (target.shouldStartStopping(axis)) {
            int axisMovementTime = (int) target.getMovementTime(axis);
            PositioningDebugger.onAxisStartStopping(axis, target);

            if (target.getMovementTime(axis) > 0) { // Negative on overshoot
                Log.w(TAG, axisName + " is anticipating stop in: " + axisMovementTime);
                FeyiuCommands.setStrictJoyAfter(axis, 0, axisMovementTime, FeyiuCommands.SHORT);
            } else {
                Log.e(TAG, axisName + " movement time became negative, ignoring.: " + axisMovementTime);
            }

            target.setIsStopping(axis);
            return;
        }

        // Not stopping, not reached and shouldnt stop:
        Log.d(TAG, axisName + " is moving regularly to target....");
        FeyiuCommands.setLooseJoyFor(axis, target.getJoyValue(axis), FeyiuCommands.REGULAR);
    }

    public void onGimbalUpdate() {
        if (this.isActive) {
            Log.d(TAG, "---------WP UPDATE----------");
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
        double pan_diff = Math.abs(FeyiuState.getAngle(Axes.Axis.PAN).value() - angle_pan);
        double tilt_diff = Math.abs(FeyiuState.getAngle(Axes.Axis.TILT).value() - angle_tilt);

        return pan_diff < 1 && tilt_diff < 1;
    }

    public void log() {
        withTargetAvailable(() -> {
            String d = "Gimbal Position Processor (Target Available):";
            d += "\n Is Stopping: P: " + target.isStoppingOn(Axes.Axis.PAN) + " T:" + target.isStoppingOn(Axes.Axis.TILT);
            d += "\n Should Stop: P: " + target.shouldStartStopping(Axes.Axis.PAN) + " T:" + target.shouldStartStopping(Axes.Axis.TILT);
            d += "\n Target Axis Reached: P: " + target.isReachedOn(Axes.Axis.PAN) + " T:" + target.isReachedOn(Axes.Axis.TILT);
            d += "\n Target Is Reached: " + target.isReached();

            Log.d(TAG, d);
        });
    }

    @NonNull
    public String toString() {
//        DecimalFormat decimalFormat = new DecimalFormat(" #.#; -#.#");
        String d = "";

        if (target != null) {
            d += "\nTime: P:" + Math.round(target.getMovementTime(Axes.Axis.PAN)) + " T:" + Math.round(target.getMovementTime(Axes.Axis.TILT));
            d += "\n Is Stopping: P: " + target.isStoppingOn(Axes.Axis.PAN) + " T:" + target.isStoppingOn(Axes.Axis.TILT);
//            d += "\n Angle Df: P:" + decimalFormat.format(target.angleDiffInDeg(mDb.AXIS_PAN)) + " T:" + decimalFormat.format(target.angleDiffInDeg(mDb.AXIS_TILT));
        }

//        d += FeyiuCommandQueue.asString();

        return d;
    }

}
