package com.feyiuremote.ui.camera;

import android.content.Context;
import android.graphics.Bitmap;

import com.feyiuremote.R;
import com.feyiuremote.libs.Cameras.Panasonic.PanasonicCamera;
import com.feyiuremote.libs.LiveStream.image.LiveFeedReceiver;
import com.feyiuremote.libs.LiveStream.processors.ObjectTrackingProcessor;
import com.feyiuremote.libs.Utils.BitmapHelper;
import com.feyiuremote.ui.camera.waypoints.Waypoint;

import java.util.ArrayList;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class CameraViewModel extends ViewModel {

    private final String TAG = CameraViewModel.class.getSimpleName();

    public final MutableLiveData<String> status = new MutableLiveData<>();
    public final MutableLiveData<PanasonicCamera> camera = new MutableLiveData<>();
    public MutableLiveData<Double> focus = new MutableLiveData<>();
    public final MutableLiveData<Boolean> streamStarted = new MutableLiveData<>();

    public final MutableLiveData<LiveFeedReceiver> liveFeedReceiver = new MutableLiveData<>();

    // Waypoints (This helps to retain data when switching  fragments
    public MutableLiveData<ArrayList<Waypoint>> waypointList = new MutableLiveData<>();
    public MutableLiveData<Boolean> waypointsLoaded = new MutableLiveData<>();
    public MutableLiveData<ObjectTrackingProcessor> objectTrackingProcessor = new MutableLiveData<>();

    public CameraViewModel() {
        status.setValue("Waiting for camera...");
        streamStarted.setValue(false);

        // This prevents from saving waypoints twice
        // since loading triggers observer
        waypointsLoaded.setValue(false);
        waypointList.setValue(new ArrayList<Waypoint>());
    }

    public boolean streamIsStarted() {
        PanasonicCamera c = camera.getValue();

        if (c != null && c.getLiveView() != null) {
            return c.getLiveView().isActive();
        }

        return false;
    }

    public Bitmap getLastImage(Context context) {
        if (liveFeedReceiver.getValue() != null) {
            return liveFeedReceiver.getValue().getImage(0);
        }

        return BitmapHelper.getBitmapFromResource(context, R.drawable.video_unavailable);
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

    public void addWaypoint(Waypoint wp) {
        addWaypoint(wp, true);
    }


}