package com.feyiuremote.ui.camera.listeners;

import android.content.Context;
import android.util.Log;

import com.feyiuremote.databinding.FragmentCameraBinding;
import com.feyiuremote.libs.Cameras.Panasonic.PanasonicCamera;
import com.feyiuremote.libs.Cameras.abstracts.Connection.ICameraControlListener;
import com.feyiuremote.libs.LiveStream.image.LiveFeedReceiver;
import com.feyiuremote.ui.camera.CameraViewModel;

public class CameraStartStreamListener implements ICameraControlListener {

    private final String TAG = CameraStartStreamListener.class.getSimpleName();

    private final CameraViewModel cameraModel;
    private final Context context;

    private final FragmentCameraBinding binding;
    private final Runnable onStartedRunnable;

    public CameraStartStreamListener(Context context, CameraViewModel cameraModel, FragmentCameraBinding binding, Runnable onStarted) {
        this.cameraModel = cameraModel;
        this.context = context;
        this.binding = binding;
        this.onStartedRunnable = onStarted;
    }

    private LiveFeedReceiver getLiveFeedReceiver() {
        LiveFeedReceiver receiver = cameraModel.liveFeedReceiver.getValue();
        if (receiver == null) {
            receiver = new LiveFeedReceiver(this.context);
            receiver.setUpdateListener(new CameraLiveStreamUpdateListener(cameraModel, binding)); // Sets messages, updates frame
            cameraModel.liveFeedReceiver.postValue(receiver);

            Log.d(TAG, "Creating live feed receiver");
            return receiver;
        } else {
            Log.d(TAG, "Reinitalizing existing live feed receiver");
            return receiver;
        }
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
            LiveFeedReceiver liveFeedReceiver = getLiveFeedReceiver();
            camera.createLiveView(liveFeedReceiver);
            camera.getLiveView().start();

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
