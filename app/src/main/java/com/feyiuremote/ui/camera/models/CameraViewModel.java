package com.feyiuremote.ui.camera.models;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.feyiuremote.R;
import com.feyiuremote.libs.Cameras.Panasonic.PanasonicCamera;
import com.feyiuremote.libs.Utils.BitmapHelper;
import com.feyiuremote.ui.camera.listeners.CameraLiveFeedReceiver;
import com.feyiuremote.ui.camera.waypoints.Waypoint;

import java.util.ArrayList;

public class CameraViewModel extends ViewModel {

    private final String TAG = CameraViewModel.class.getSimpleName();

    public final MutableLiveData<String> status = new MutableLiveData<>();
    public final MutableLiveData<PanasonicCamera> camera = new MutableLiveData<>();
    public MutableLiveData<Double> focus = new MutableLiveData<>();
    public final MutableLiveData<CameraLiveFeedReceiver> liveFeedReceiver = new MutableLiveData<>();
    public MutableLiveData<Boolean> isStreaming = new MutableLiveData<>();
    public MutableLiveData<String> debugMessage = new MutableLiveData<>();

    public CameraViewModel() {
        status.setValue("Waiting for camera...");

        isStreaming.setValue(false);
    }

    public boolean streamIsStarted() {
        return (Boolean.TRUE.equals(isStreaming.getValue()));
    }

    public Bitmap getLastImage(Context context) {
        if (liveFeedReceiver.getValue() != null) {
            // TODO: Might cause concurrent access
            return liveFeedReceiver.getValue().getFrame().bitmap();
        }

        return BitmapHelper.getBitmapFromResource(context, R.drawable.video_unavailable);
    }

}