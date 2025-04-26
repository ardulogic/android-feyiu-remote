package com.feyiuremote.ui.camera.listeners;

import android.graphics.Bitmap;

import com.feyiuremote.databinding.FragmentCameraBinding;
import com.feyiuremote.libs.LiveStream.interfaces.ILiveFeedUpdateListener;
import com.feyiuremote.ui.camera.CameraViewModel;

public class CameraStreamFramesListener implements ILiveFeedUpdateListener {
    private final String TAG = CameraStreamFramesListener.class.getSimpleName();
    private final CameraViewModel cameraModel;
    private final FragmentCameraBinding binding;

    public CameraStreamFramesListener(CameraViewModel cameraModel, FragmentCameraBinding binding) {
        this.cameraModel = cameraModel;
        this.binding = binding;
    }

    @Override
    public void onMessage(String message) {
        //TODO: Fix the updating mechanism
        cameraModel.status.postValue(message);
    }

    @Override
    public void onNewFrame(Bitmap bitmap) {
        if (bitmap != null) {
            binding.liveView.setFrameBitmap(bitmap);
        }
    }
}