package com.feyiuremote.ui.camera.listeners;

import android.util.Log;

import com.feyiuremote.databinding.FragmentCameraBinding;
import com.feyiuremote.libs.LiveStream.interfaces.ILiveFeedUpdateListener;
import com.feyiuremote.ui.camera.CameraViewModel;

public class CameraLiveStreamUpdateListener implements ILiveFeedUpdateListener {
    private final String TAG = CameraStartStreamListener.class.getSimpleName();
    private final CameraViewModel cameraModel;
    private final FragmentCameraBinding binding;

    public CameraLiveStreamUpdateListener(CameraViewModel cameraModel, FragmentCameraBinding binding) {
        this.cameraModel = cameraModel;
        this.binding = binding;
    }

    @Override
    public void onUpdate(String message) {
        cameraModel.status.postValue(message);

        if (binding != null) {
            binding.liveView.refresh(); // This is crucial to show the image
        } else {
            Log.e(TAG, "liveView is Null");
        }
    }
}