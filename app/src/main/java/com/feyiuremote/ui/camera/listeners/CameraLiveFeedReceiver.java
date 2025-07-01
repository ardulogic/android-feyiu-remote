package com.feyiuremote.ui.camera.listeners;


import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.viewbinding.ViewBinding;

import com.feyiuremote.MainActivity;
import com.feyiuremote.databinding.FragmentCameraBinding;
import com.feyiuremote.libs.Cameras.abstracts.CameraFrame;
import com.feyiuremote.libs.LiveStream.abstracts.FrameProcessor;
import com.feyiuremote.libs.LiveStream.abstracts.LiveFeedReceiver;
import com.feyiuremote.libs.LiveStream.processors.FrameProcessorDispatcher;
import com.feyiuremote.libs.LiveStream.processors.trackers.BoxTrackingMediapipeProcessor;
import com.feyiuremote.ui.camera.models.CameraViewModel;

import java.lang.reflect.Constructor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraLiveFeedReceiver extends LiveFeedReceiver {
    private final String TAG = CameraLiveFeedReceiver.class.getSimpleName();
    private final CameraViewModel cameraModel;
    private final MainActivity mainActivity;
    private LifecycleOwner lifecycleOwner;
    private FragmentCameraBinding binding;

    private final ExecutorService messageExecutor =
            Executors.newSingleThreadExecutor();

    public final FrameProcessorDispatcher frameProcessorDispatcher;

    public CameraLiveFeedReceiver(LifecycleOwner lfOwner, MainActivity mainActivity, CameraViewModel cameraModel, FragmentCameraBinding binding) {
        this.cameraModel = cameraModel;
        this.binding = binding;
        this.mainActivity = mainActivity;
        this.lifecycleOwner = lfOwner;

        this.frameProcessorDispatcher = new FrameProcessorDispatcher();
        setFrameProcessor(BoxTrackingMediapipeProcessor.class.getName());
//        setFrameProcessor(BoxTrackingOpenCvProcessor.class.getName());
    }

    public FrameProcessor getFrameProcessor() {
        return frameProcessorDispatcher.getCurrentProcessor();
    }

    public void cancelFrameProcessor() {
        if (this.frameProcessorDispatcher.getCurrentProcessor() != null) {
            this.frameProcessorDispatcher.terminateProcessor();
        }
    }

    public void setFrameProcessor(String processorClassName) {
        if (this.frameProcessorDispatcher.getCurrentProcessor() != null) {
            this.frameProcessorDispatcher.terminateProcessor();
        }

        if (processorClassName == null) {
            this.frameProcessorDispatcher.setFrameProcessor(null);
            return;
        }

        try {
            Class<?> clazz = Class.forName(processorClassName);
            Constructor<?> constructor = clazz.getConstructor(
                    binding.overlayView.getClass(), // or View.class
                    MainActivity.class,      // or Activity.class / LifecycleOwner.class
                    cameraModel.getClass()          // or CameraModel.class
            );

            Object processorInstance = constructor.newInstance(
                    binding.overlayView,
                    mainActivity,
                    cameraModel
            );

            this.frameProcessorDispatcher.setFrameProcessor((FrameProcessor) processorInstance);
            setBinding(binding);
        } catch (Exception e) {
            e.printStackTrace(); // Ideally replace with proper logging
        }
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

    @Override
    public void onIsStreamingChanged(boolean value) {
        cameraModel.isStreaming.postValue(value);
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
        showMessage(message);
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