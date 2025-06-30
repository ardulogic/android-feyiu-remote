package com.feyiuremote.libs.Feiyu.processors.position;

import android.content.Context;
import android.util.Log;

import com.feyiuremote.ui.camera.waypoints.Waypoint;
import com.feyiuremote.ui.camera.waypoints.WaypointsList;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GimbalWaypointsProcessor extends GimbalPositionProcessor {
    private final String TAG = GimbalWaypointsProcessor.class.getSimpleName();
    // TODO: Implement proper dwelling on/off
    public static final String MODE_GO_TO = "single"; // Mode when we just need it to go to single waypoint

    public static final String MODE_PLAY_ONCE = "all"; // Mode when we just need to go through all waypoints once

    public static final String MODE_PLAY_LOOP = "endless"; // Mode when we want to loop waypoints
    private final WaypointsList waypoints;

    public String mode = MODE_GO_TO;

    private int current_waypoint = 0;

    private Future<?> currentScheduledTask;

    private IGimbalWaypointsProcessorStateListener stateListener;

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public GimbalWaypointsProcessor(Context context, WaypointsList waypoints) {
        super(context);

        this.waypoints = waypoints;

        this.setListener(new positionProcessorListener());
    }

    public boolean atLeastTwoWaypointsExist() {
        return this.waypoints.size() >= 2;
    }

    public void setStateListener(IGimbalWaypointsProcessorStateListener listener) {
        this.stateListener = listener;
    }

    public void start(String mode) {
        this.mode = mode;
        setActive(true);

        if (Objects.equals(mode, MODE_GO_TO)) {
            super.start();
        } else {
            if (atLeastTwoWaypointsExist()) {
                if (isAtStart()) {
                    moveToNextWaypoint();
                } else {
                    startFromFirstWaypoint();
                }
            } else {
                setActive(false);
            }
        }
    }

    protected void setActive(boolean state) {
        this.isActive = state;

        if (!isActive) {
            setAllWaypointsInactive();
        }

        if (stateListener != null) {
            stateListener.onStateChange(mode, isActive);
        }
    }

    public void setAllWaypointsInactive() {
        waypoints.setAllAsInactive();
    }


    public boolean isAtStart() {
        return this.isAt(getWaypoint(0).anglePan, getWaypoint(0).angleTilt);
    }

    public void setActiveWaypoint(int index) {
        if (waypointExists(index)) {
            getWaypoint(current_waypoint).setActive(false);
            waypoints.onWaypointIsActiveChanged(getWaypoint(current_waypoint));

            getWaypoint(index).setActive(true);
            current_waypoint = index;
            waypoints.onWaypointIsActiveChanged(getWaypoint(current_waypoint));
        } else {
            setActive(false);
        }
    }

    public void setWaypointAsTarget(int index) {
        setActiveWaypoint(index);
        setCurrentWaypointAsTarget();
    }

    public void setCurrentWaypointAsTarget() {
        setTarget(getWaypoint(current_waypoint));
    }

    public void startFromFirstWaypoint() {
        Log.d(TAG, "Moving to starting waypoint");

        setActiveWaypoint(0);
        setCurrentWaypointAsTarget();
        super.start();
    }

    private boolean isAtLastWaypoint() {
        return waypoints.isEmpty() || current_waypoint == waypoints.size() - 1;
    }

    private void moveToNextWaypoint() {
        if (isActive) {
            int next_index = current_waypoint + 1;
            if (!waypointExists(next_index)) {
                next_index = 0;
            }

            if (waypointExists(next_index)) {
                Log.d(TAG, "Moving to next waypoint: " + next_index);

                setActiveWaypoint(next_index);
                setCurrentWaypointAsTarget();
                super.start();
            } else {
                Log.w(TAG, "Waypoint no longer exists: " + next_index);

                super.cancel();
            }
        }
    }

    public void goToWaypoint(int waypoint_index) {
        try {
            if (waypointExists(waypoint_index)) {
                setWaypointAsTarget(waypoint_index);
                start(GimbalWaypointsProcessor.MODE_GO_TO);
            } else {
                Log.e(TAG, "Unable to go to waypoint (" + waypoint_index + "), it doesnt exist.");
            }
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "Hard crash trying to go to waypoint " + waypoint_index + ", it doesnt exist!");
        }
    }

    private boolean isLastWaypoint() {
        return !waypointExists(current_waypoint + 1);
    }

    public void cancelIfActive(int position) {
        if (current_waypoint == position) {
            cancel();
        }
    }

    class positionProcessorListener implements IGimbalPositionProcessorListener {

        @Override
        public void onTargetReached(GimbalPositionTarget target) {
            Log.d(TAG, "Target has been reached.");
            clearTarget();

            // Cancel the previous task if it exists and hasn't finished
            if (currentScheduledTask != null) {
                currentScheduledTask.cancel(false); // Cancel without interrupting the task
            }

            if (mode.equals(MODE_GO_TO)) {
                setActive(false);
                return;
            }

            if (mode.equals(MODE_PLAY_ONCE)) {
                if (isAtLastWaypoint()) {
                    setActive(false);
                    return;
                }
            }

            scheduleGoToNextTarget(target);
        }

        @Override
        public void onTargetNearlyReached() {
            // TODO: Properly implement blending
        }
    }

    private void scheduleGoToNextTarget(GimbalPositionTarget target) {
        // Schedule next waypoint otherwise
        Log.d(TAG, "Scheduling next waypoint in :" + target.dwell_time_ms);

        // Schedule a new task to move to the next waypoint after a delay
        currentScheduledTask = scheduler.schedule(() -> {
            Log.d(TAG, "Executing scheduled waypoint:" + target.dwell_time_ms);
            try {
                // After dwell time, move to the next waypoint
                moveToNextWaypoint();
            } catch (Exception e) {
                System.err.println("Error during waypoint transition:");
                e.printStackTrace(System.err);
            }
        }, target.dwell_time_ms, TimeUnit.MILLISECONDS);
    }

    private Waypoint getWaypoint(int index) {
        return waypoints.get(index);
    }

    private boolean waypointExists(int index) {
        return (index >= 0 && index < waypoints.size());
    }


}
