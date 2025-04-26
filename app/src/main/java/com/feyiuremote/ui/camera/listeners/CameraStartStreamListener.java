package com.feyiuremote.ui.camera.listeners;

import android.util.Log;

import androidx.lifecycle.LifecycleOwner;

import com.feyiuremote.MainActivity;
import com.feyiuremote.databinding.FragmentCameraBinding;
import com.feyiuremote.libs.Cameras.Panasonic.PanasonicCamera;
import com.feyiuremote.libs.Cameras.abstracts.Connection.ICameraControlListener;
import com.feyiuremote.libs.LiveStream.abstracts.LiveFeedReceiver;
import com.feyiuremote.ui.camera.CameraViewModel;

public class CameraStartStreamListener implements ICameraControlListener {

    private final String TAG = CameraStartStreamListener.class.getSimpleName();

    private final CameraViewModel cameraModel;
    private final LifecycleOwner lfOwner;

    private final FragmentCameraBinding binding;
    private final Runnable onStartedRunnable;
    private final MainActivity mainActivity;

    public CameraStartStreamListener(LifecycleOwner lfOwner, MainActivity mainActivity, CameraViewModel cameraModel, FragmentCameraBinding binding, Runnable onStarted) {
        this.cameraModel = cameraModel;
        this.lfOwner = lfOwner;
        this.binding = binding;
        this.mainActivity = mainActivity;
        this.onStartedRunnable = onStarted;
    }

    private LiveFeedReceiver createLiveFeedReceiver() {
        CameraLiveFeedReceiver receiver = new CameraLiveFeedReceiver(lfOwner, mainActivity, cameraModel, binding);
        cameraModel.liveFeedReceiver.postValue(receiver);
        Log.d(TAG, "Creating live feed receiver");

        return receiver;
    }

    @Override
    public void onSuccess() {
        try {
            PanasonicCamera camera = cameraModel.camera.getValue();

            cameraModel.status.postValue("Giving a bit of time...");
            Thread.sleep(1000);
            cameraModel.status.postValue("Stream started");

            // Its neccessary to have feed receiver a part of camera model
            // so it retains its value while switching between tabs
            if (!camera.liveViewAlreadyExists()) {
                camera.createLiveView(createLiveFeedReceiver());
                camera.getLiveView().start();
            }

            cameraModel.camera.postValue(camera);
            cameraModel.streamStarted.postValue(true);
            onStartedRunnable.run();
        } catch (InterruptedException e) {
            e.printStackTrace();
            cameraModel.streamStarted.postValue(false);
        }
    }

    @Override
    public void onFailure() {
        cameraModel.streamStarted.postValue(false);
        cameraModel.status.postValue("Failed to start stream");
    }

}
