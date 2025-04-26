package com.feyiuremote.ui.camera;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;

import com.feyiuremote.MainActivity;
import com.feyiuremote.R;
import com.feyiuremote.databinding.FragmentCameraBinding;
import com.feyiuremote.libs.Bluetooth.BluetoothViewModel;
import com.feyiuremote.libs.Cameras.Panasonic.PanasonicCamera;
import com.feyiuremote.libs.Cameras.Panasonic.PanasonicCameraDiscovery;
import com.feyiuremote.libs.Cameras.abstracts.Connection.ICameraControlListener;
import com.feyiuremote.libs.Feiyu.FeyiuControls;
import com.feyiuremote.libs.Feiyu.processors.position.GimbalWaypointsProcessor;
import com.feyiuremote.libs.LiveStream.abstracts.FrameProcessor;
import com.feyiuremote.ui.camera.listeners.CameraControlClickListener;
import com.feyiuremote.ui.camera.listeners.CameraDiscoveryListener;
import com.feyiuremote.ui.camera.listeners.CameraFocusClickListener;
import com.feyiuremote.ui.camera.listeners.CameraLiveFeedReceiver;
import com.feyiuremote.ui.camera.listeners.CameraStartStreamListener;
import com.feyiuremote.ui.camera.listeners.WaypointAddClickListener;
import com.feyiuremote.ui.camera.observers.BluetoothConnectivityObserver;
import com.feyiuremote.ui.camera.observers.BluetoothEnabledObserver;
import com.feyiuremote.ui.camera.observers.CameraFocusObserver;
import com.feyiuremote.ui.camera.observers.CameraObserver;
import com.feyiuremote.ui.camera.waypoints.ItemTouchHelperCallback;
import com.feyiuremote.ui.camera.waypoints.WaypointListAdapter;

import java.util.Objects;
import java.util.concurrent.Executors;

public class CameraFragment extends Fragment {

    private final String TAG = CameraFragment.class.getSimpleName();

    private MainActivity mainActivity;
    private FragmentCameraBinding binding;
    private CameraViewModel cameraViewModel;
    private BluetoothViewModel mBluetoothViewModel;
    private GimbalWaypointsProcessor mWaypointsProcessor;


    @SuppressLint("SetTextI18n")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        mainActivity = (MainActivity) getActivity();
        binding = FragmentCameraBinding.inflate(inflater, container, false);
        FeyiuControls.init(mainActivity.mBluetoothLeService);

        cameraViewModel = new ViewModelProvider(requireActivity()).get(CameraViewModel.class);
        cameraViewModel.status.observe(getViewLifecycleOwner(), binding.textCameraStatus::setText);
        cameraViewModel.camera.observe(getViewLifecycleOwner(), new CameraObserver(getContext(), binding));
        cameraViewModel.focus.observe(getViewLifecycleOwner(), new CameraFocusObserver(binding));

        mBluetoothViewModel = new ViewModelProvider(requireActivity()).get(BluetoothViewModel.class);
        mBluetoothViewModel.connected.observe(getViewLifecycleOwner(), new BluetoothConnectivityObserver(binding));
        mBluetoothViewModel.enabled.observe(getViewLifecycleOwner(), new BluetoothEnabledObserver(mainActivity));

        //        mBluetoothViewModel.characteristics.get(FeyiuUtils.NOTIFICATION_CHARACTERISTIC_ID)
//                .observe(getViewLifecycleOwner(), new BluetoothGimbalPositionObserver(binding, manualTrackingFrameProcessor.mWaypointsProcessor));

        mWaypointsProcessor = new GimbalWaypointsProcessor(mainActivity.getBaseContext(), cameraViewModel.waypointList);

        binding.buttonCameraConnect.setOnClickListener(view -> {
            if (cameraViewModel.streamIsStarted()) {
                stopLiveView();
            } else {
                connectToCamera();
            }
        });

        WaypointListAdapter wpListAdapter = new WaypointListAdapter(mainActivity.getBaseContext(), getViewLifecycleOwner(), cameraViewModel, mWaypointsProcessor);
        binding.listWaypoints.setAdapter(wpListAdapter);

        mWaypointsProcessor.setStateListener((mode, isActive) -> {
            boolean endlessActive = isActive && Objects.equals(mode, GimbalWaypointsProcessor.MODE_ENDLESS);
            boolean allOnceActive = isActive && Objects.equals(mode, GimbalWaypointsProcessor.MODE_ALL);

            ColorStateList endlessBtnColor = endlessActive ? ContextCompat.getColorStateList(getContext(), R.color.button_active) : null;
            ColorStateList playBtnColor = allOnceActive ? ContextCompat.getColorStateList(getContext(), R.color.button_active) : null;

            binding.buttonPlayWaypointsEndless.setBackgroundTintList(endlessBtnColor);
            binding.buttonPlayWaypointsOnce.setBackgroundTintList(playBtnColor);
        });

        // Drag and drop waypoints
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelperCallback(wpListAdapter));
        itemTouchHelper.attachToRecyclerView(binding.listWaypoints);

        binding.buttonAddWaypoint.setOnClickListener(new WaypointAddClickListener(cameraViewModel, mainActivity));

        binding.buttonCameraTakePhoto.setOnClickListener(new CameraControlClickListener(cameraViewModel, camera -> {
            Executors.newSingleThreadExecutor().execute(() ->
                    camera.controls.takePicture(new CameraControlListener())
            );
        }));

        binding.buttonCameraTakeVideo.setOnClickListener(new CameraControlClickListener(cameraViewModel, camera -> {
            Executors.newSingleThreadExecutor().execute(() ->
                    camera.controls.toggleVideoRecording(new CameraControlListener())
            );
        }));

        binding.buttonCameraFocus.setOnClickListener(new CameraFocusClickListener(cameraViewModel));

        binding.buttonPlayWaypointsOnce.setOnClickListener(view -> {
            // TODO
//            if (!manualTrackingFrameProcessor.mWaypointsProcessor.isActive) {
//                manualTrackingFrameProcessor.mWaypointsProcessor.start(GimbalWaypointsProcessor.MODE_ALL);
//            } else {
//                manualTrackingFrameProcessor.mWaypointsProcessor.stop();
//            }
        });

//        binding.buttonPlayWaypointsBlend.setOnClickListener(view -> {
//            unifiedTrackingProcessor.mWaypointsProcessor.toggleFlag(GimbalWaypointsProcessor.FLAG_DWELL);
//        });

        binding.buttonPlayWaypointsEndless.setOnClickListener(view -> {
            boolean isActive = mWaypointsProcessor.isActive;

            if (isActive) {
                mWaypointsProcessor.stop();
            } else {
                mWaypointsProcessor.start(GimbalWaypointsProcessor.MODE_ENDLESS);
            }
        });

        if (cameraViewModel.streamIsStarted()) {
            Log.d(TAG, "Stream is already started, continuing");
            continueLiveView();
        } else {
            connectToCamera();
        }

        return binding.getRoot();
    }

    private void connectToCamera() {
        PanasonicCameraDiscovery cameraDiscovery = new PanasonicCameraDiscovery(mainActivity.executor);

        cameraDiscovery.clear();
        cameraDiscovery.search(getContext(), new CameraDiscoveryListener(cameraViewModel, this::startLiveView, this::connectToCamera));
    }

    public void startLiveView() {
        cameraViewModel.status.postValue("Enabling camera controls...");
        PanasonicCamera camera = cameraViewModel.camera.getValue();

        camera.controls.enable(new ICameraControlListener() {
            @Override
            public void onSuccess() {
                cameraViewModel.status.postValue("Starting stream...");

                camera.controls.startStream(new CameraStartStreamListener(getViewLifecycleOwner(), mainActivity, cameraViewModel, binding, () -> {
                    // Runnable when stream has started successfully
                    //                    setLiveImageProcessors(cameraViewModel.liveFeedReceiver.getValue());
                }));

                mWaypointsProcessor.setCamera(cameraViewModel.camera);
            }

            @Override
            public void onFailure() {
                cameraViewModel.status.postValue("Could not enable camera controls.");
            }
        });
    }

//    private void setLiveImageProcessors(LiveFeedReceiver r) {
//        if (r != null) {
//            r.setFrameProcessor(manualTrackingFrameProcessor);
//            Log.d(TAG, "Binding trackers to live view");
//        }
//    }

    private FrameProcessor getCurrentFrameProcessor() {
        CameraLiveFeedReceiver r = cameraViewModel.liveFeedReceiver.getValue();

        if (r != null) {
            r.frameProcessorDispatcher.getCurrentProcessor();
        }

        Log.e(TAG, "Cant get current frame processor because life feed receiver is null");
        return null;
    }

    /**
     * Rebind existing live stream to newly created fragment
     */
    public void continueLiveView() {
        // After switching fragments view binding is "disconnected"
        CameraLiveFeedReceiver r = cameraViewModel.liveFeedReceiver.getValue();

        // Updates needed for frames to actually appear on screen
        r.setBinding(binding); // and track fragment lifecycle (if minimized or not)
        r.setLifecycleOwner(getViewLifecycleOwner());
    }

    public void stopLiveView() {
        PanasonicCamera camera = cameraViewModel.camera.getValue();

        if (camera != null && camera.getLiveView() != null) {
            camera.getLiveView().stop();
        }
    }

    @Override
    public void onDestroyView() {
        cancelAll();

        super.onDestroyView();
        binding = null;
    }

    private void cancelAll() {
        CameraLiveFeedReceiver feedReceiver = cameraViewModel.liveFeedReceiver.getValue();
        if (feedReceiver != null) {
            feedReceiver.onStop("Fragment has been destroyed.");
        }
    }

    class CameraControlListener implements ICameraControlListener {
        @Override
        public void onSuccess() {
            cameraViewModel.camera.postValue(cameraViewModel.camera.getValue());
        }

        @Override
        public void onFailure() {
            cameraViewModel.camera.postValue(cameraViewModel.camera.getValue());
        }
    }

}

