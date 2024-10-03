package com.feyiuremote.libs.Feiyu.processors.position;

import android.content.Context;
import android.util.Log;

import com.feyiuremote.ui.camera.waypoints.Waypoint;

import java.util.ArrayList;

import androidx.lifecycle.MutableLiveData;

public class GimbalWaypointsProcessor extends GimbalPositionProcessor {
    private final String TAG = GimbalWaypointsProcessor.class.getSimpleName();
    public static final int MODE_DWELL = 0; // Mode when gimbal stops after each waypoint
    public static final int MODE_BLEND = 1; // Mode when gimbal "blends" waypoints

    public static final int MODE_SINGLE = 2; // Mode when we just need it to go to single waypoint

    public static final int MODE_ENDLESS = 3; // Mode when we just need it to go to single waypoint
    private final MutableLiveData<ArrayList<Waypoint>> waypoints;

    private int mode = MODE_DWELL;

    private int current_waypoint = 0;

    private boolean is_active = false;

    public GimbalWaypointsProcessor(Context context, MutableLiveData<ArrayList<Waypoint>> waypoints) {
        super(context);

        this.waypoints = waypoints;

        this.setListener(new positionProcessorListener());
    }

    public boolean enoughWaypointsAvailable() {
        return this.waypoints.getValue().size() >= 2;
    }

    public void start(int mode) {
        this.mode = mode;

        if (mode == MODE_SINGLE) {
            super.start();
        } else {
            if (enoughWaypointsAvailable()) {
                if (isAtStart()) {
                    setActiveWaypoint(0);
                    moveToNextWaypoint();
                } else {
                    moveToStart();
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

    public void moveToStart() {
        setActiveWaypoint(0);
        setCurrentWaypointAsTarget();
        super.start();
        Log.d(TAG, "Moving to starting waypoint");
    }

    private void moveToNextWaypoint() {
        int next_index = current_waypoint + 1;
        Log.d(TAG, "Moving to next waypoint: " + next_index);

        if (waypointExists(next_index)) {
            setActiveWaypoint(next_index);
            setCurrentWaypointAsTarget();
            super.start();
        } else {
            if (mode != MODE_ENDLESS) {
                Log.d(TAG, "Clearing target, last waypoint reached");
                this.cancel();
            } else {
                Log.d(TAG, "All over again");
                setActiveWaypoint(0);
                setCurrentWaypointAsTarget();
            }
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
        public void onTargetReached() {
            // Blending on last waypoint makes it miss it by big margin
            // since stopping doesnt really happen onTargetReached()
            if (mode == MODE_DWELL || isLastWaypoint() || mode == MODE_ENDLESS) {
                moveToNextWaypoint();
            }
        }

        @Override
        public void onTargetNearlyReached() {
            if (mode == MODE_BLEND && !isLastWaypoint()) {
                moveToNextWaypoint();
            }
        }
    }

    private Waypoint getWaypoint(int index) {
        return waypoints.getValue().get(index);
    }

    private boolean waypointExists(int index) {
        return (index >= 0 && index < waypoints.getValue().size());
    }

}
