package com.feyiuremote.ui.camera.listeners;


import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.viewbinding.ViewBinding;

import com.feyiuremote.MainActivity;
import com.feyiuremote.databinding.FragmentCameraBinding;
import com.feyiuremote.libs.Cameras.abstracts.CameraFrame;
import com.feyiuremote.libs.LiveStream.abstracts.LiveFeedReceiver;
import com.feyiuremote.libs.LiveStream.processors.BoxTrackingMediapipeProcessor;
import com.feyiuremote.libs.LiveStream.processors.FrameProcessorDispatcher;
import com.feyiuremote.ui.camera.CameraViewModel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraLiveFeedReceiver extends LiveFeedReceiver {
    private final String TAG = CameraLiveFeedReceiver.class.getSimpleName();
    private final CameraViewModel cameraModel;
    private LifecycleOwner lifecycleOwner;
    private FragmentCameraBinding binding;

    private final ExecutorService messageExecutor =
            Executors.newSingleThreadExecutor();

    public final FrameProcessorDispatcher frameProcessorDispatcher;

    public CameraLiveFeedReceiver(LifecycleOwner lfOwner, MainActivity mainActivity, CameraViewModel cameraModel, FragmentCameraBinding binding) {
        this.cameraModel = cameraModel;
        this.binding = binding;
        this.lifecycleOwner = lfOwner;

        this.frameProcessorDispatcher = new FrameProcessorDispatcher();
//        this.frameProcessorDispatcher.setFrameProcessor(new FrameDummyProcessor(binding.overlayView));
        this.frameProcessorDispatcher.setFrameProcessor(new BoxTrackingMediapipeProcessor(
                binding.overlayView,
                mainActivity,
                cameraModel
        ));
    }

    @Override
    public void onNewFrame(CameraFrame frame) {
        super.onNewFrame(frame);

        // 1) Skip if the view‐lifecycle isn't at least STARTED
        if (feedIsVisibleToUser()) {
            if (frame.bitmap() != null) {
                binding.liveView.setFrameBitmap(frame.bitmap());
                showMessage("Frames Received: " + getFramesReceivedCount());

                frameProcessorDispatcher.processFrame(frame);
            }
        } else {
            frameProcessorDispatcher.stopProcessor();
        }
    }


    private boolean feedIsVisibleToUser() {
        return lifecycleOwner.getLifecycle()
                .getCurrentState()
                .isAtLeast(Lifecycle.State.STARTED);
    }

    @Override
    public void onError(String message) {
        showMessage(message);
    }

    @Override
    public void onWarning(String message) {
        showMessage(message);
    }

    @Override
    public void onInfo(String message) {
        showMessage(message);
    }

    @Override
    public void onStop(String message) {
        frameProcessorDispatcher.stopProcessor();
    }

    @Override
    public void setBinding(ViewBinding binding) {
        this.binding = (FragmentCameraBinding) binding;
        this.frameProcessorDispatcher.getCurrentProcessor().setOverlayView(((FragmentCameraBinding) binding).overlayView);
    }

    public void setLifecycleOwner(LifecycleOwner owner) {
        this.lifecycleOwner = owner;
    }

    private void showMessage(String message) {
        messageExecutor.execute(() ->
                cameraModel.status.postValue(message)
        );
    }
}