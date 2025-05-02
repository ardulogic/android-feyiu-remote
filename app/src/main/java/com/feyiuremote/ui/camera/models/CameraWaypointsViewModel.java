package com.feyiuremote.ui.camera.models;

import android.content.Context;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.feyiuremote.libs.Cameras.Panasonic.PanasonicCamera;
import com.feyiuremote.libs.Feiyu.processors.position.GimbalWaypointsProcessor;
import com.feyiuremote.ui.camera.waypoints.Waypoint;
import com.feyiuremote.ui.camera.waypoints.WaypointListAdapter;

import java.util.ArrayList;

public class CameraWaypointsViewModel extends ViewModel {

    private final String TAG = CameraWaypointsViewModel.class.getSimpleName();

    public final MutableLiveData<GimbalWaypointsProcessor> processor = new MutableLiveData<>();
    public MutableLiveData<ArrayList<Waypoint>> waypointList = new MutableLiveData<>();
    public MutableLiveData<Boolean> waypointsLoaded = new MutableLiveData<>();

    public MutableLiveData<Boolean> isActive = new MutableLiveData<>();
    public MutableLiveData<Boolean> isPlayLoopActive = new MutableLiveData<>();
    public MutableLiveData<Boolean> isPlayOnceActive = new MutableLiveData<>();


    public MutableLiveData<String> debugMessage = new MutableLiveData<>();

    public CameraWaypointsViewModel() {
        // This prevents from saving waypoints twice
        // since loading triggers observer
        processor.setValue(null);
        waypointsLoaded.setValue(false);
        waypointList.setValue(new ArrayList<Waypoint>());
    }

    public void addWaypoint(Waypoint wp, boolean main_thread) {
        ArrayList<Waypoint> list = waypointList.getValue();
        list.add(wp);

        if (main_thread) {
            waypointList.setValue(list);
        } else {
            waypointList.postValue(list);
        }
    }


}