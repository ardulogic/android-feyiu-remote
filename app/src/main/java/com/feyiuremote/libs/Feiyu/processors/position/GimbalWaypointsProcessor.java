package com.feyiuremote.libs.Feiyu.processors.position;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.feyiuremote.ui.camera.waypoints.Waypoint;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class GimbalWaypointsProcessor extends GimbalPositionProcessor {
    private final String TAG = GimbalWaypointsProcessor.class.getSimpleName();
    // TODO: Implement proper dwelling on/off
    public static final String MODE_SINGLE = "single"; // Mode when we just need it to go to single waypoint

    public static final String MODE_ALL = "all"; // Mode when we just need to go through all waypoints once

    public static final String MODE_ENDLESS = "endless"; // Mode when we want to loop waypoints
    private final MutableLiveData<ArrayList<Waypoint>> waypoints;

    private String mode = MODE_SINGLE;

    private int current_waypoint = 0;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public GimbalWaypointsProcessor(Context context, MutableLiveData<ArrayList<Waypoint>> waypoints) {
        super(context);

        this.waypoints = waypoints;

        this.setListener(new positionProcessorListener());
    }

    public boolean atLeastTwoWaypointsExist() {
        return this.waypoints.getValue().size() >= 2;
    }

    public void start(String mode) {
        this.mode = mode;

        if (Objects.equals(mode, MODE_SINGLE)) {
            super.start();
        } else {
            if (atLeastTwoWaypointsExist()) {
                if (isAtStart()) {
                    setActiveWaypoint(0);
                    moveToNextWaypoint();
                } else {
                    startFromFirstWaypoint();
                }
            }
        }
    }

    public boolean isAtStart() {
        return this.isAt(getWaypoint(0).anglePan, getWaypoint(0).angleTilt);
    }

    public void setActiveWaypoint(int index) {
        if (waypointExists(index)) {
            getWaypoint(current_waypoint).setActive(false);
            getWaypoint(index).setActive(true);
            current_waypoint = index;

            waypoints.postValue(waypoints.getValue());
        }

        // Else waypoint no longer exists, maybe choose a previous one
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

    private void moveToNextWaypoint() {
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

    public void goToWaypoint(int waypoint_index) {
        try {
            if (waypointExists(waypoint_index)) {
                setWaypointAsTarget(waypoint_index);
                start(GimbalWaypointsProcessor.MODE_SINGLE);
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
            // Blending on last waypoint makes it miss it by big margin
            // since stopping doesnt really happen onTargetReached()
            if (mode.equals(MODE_ENDLESS) || mode.equals(MODE_ALL)) {
                executor.submit(() -> {
                    try {
                        TimeUnit.MILLISECONDS.sleep(target.dwell_time_ms);

                        // After dwell time, move to the next waypoint
                        if (mode.equals(MODE_ENDLESS) || !isLastWaypoint()) {
                            moveToNextWaypoint();
                        }
                    } catch (InterruptedException e) {
                        // Handle interruption
                        Thread.currentThread().interrupt();
                        System.err.println("Dwell time interrupted: " + e.getMessage());
                    }
                });
            }
        }

        @Override
        public void onTargetNearlyReached() {
            // TODO: Properly implement blending
        }
    }

    private Waypoint getWaypoint(int index) {
        return waypoints.getValue().get(index);
    }

    private boolean waypointExists(int index) {
        return (index >= 0 && index < waypoints.getValue().size());
    }

}
