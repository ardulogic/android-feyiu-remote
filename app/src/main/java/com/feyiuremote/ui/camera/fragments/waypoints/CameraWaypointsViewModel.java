package com.feyiuremote.ui.camera.fragments.waypoints;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.feyiuremote.libs.Feiyu.processors.position.GimbalWaypointsProcessor;
import com.feyiuremote.ui.camera.waypoints.WaypointsList;

public class CameraWaypointsViewModel extends ViewModel {

    private final String TAG = CameraWaypointsViewModel.class.getSimpleName();

    public final MutableLiveData<GimbalWaypointsProcessor> processor = new MutableLiveData<>();
    public WaypointsList waypointList;
    public MutableLiveData<Boolean> isPlayLoopActive = new MutableLiveData<>();
    public MutableLiveData<Boolean> isPlayOnceActive = new MutableLiveData<>();
    public MutableLiveData<String> debugMessage = new MutableLiveData<>();

    public CameraWaypointsViewModel() {
        // This prevents from saving waypoints twice
        // since loading triggers observer
        processor.setValue(null);
        waypointList = new WaypointsList();
    }

}