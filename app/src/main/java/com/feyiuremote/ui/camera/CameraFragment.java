package com.feyiuremote.ui.camera;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.feyiuremote.MainActivity;
import com.feyiuremote.R;
import com.feyiuremote.databinding.FragmentCameraBinding;
import com.feyiuremote.libs.Bluetooth.BluetoothViewModel;
import com.feyiuremote.libs.Cameras.Panasonic.PanasonicCamera;
import com.feyiuremote.libs.Cameras.Panasonic.PanasonicCameraDiscovery;
import com.feyiuremote.libs.Cameras.abstracts.Connection.ICameraControlListener;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.controls.commands.CenterCommand;
import com.feyiuremote.libs.Feiyu.queue.FeyiuCommandQueue;
import com.feyiuremote.libs.LiveStream.abstracts.FrameProcessor;
import com.feyiuremote.ui.camera.fragments.waypoints.CameraWaypointsViewModel;
import com.feyiuremote.ui.camera.listeners.CameraControlClickListener;
import com.feyiuremote.ui.camera.listeners.CameraDiscoveryListener;
import com.feyiuremote.ui.camera.listeners.CameraFocusClickListener;
import com.feyiuremote.ui.camera.listeners.CameraLiveFeedReceiver;
import com.feyiuremote.ui.camera.listeners.CameraStartStreamListener;
import com.feyiuremote.ui.camera.models.CameraViewModel;
import com.feyiuremote.ui.camera.observers.BluetoothConnectivityObserver;
import com.feyiuremote.ui.camera.observers.BluetoothEnabledObserver;
import com.feyiuremote.ui.camera.observers.CameraFocusObserver;
import com.feyiuremote.ui.camera.observers.CameraObserver;
import com.feyiuremote.ui.camera.views.BatteryLevelView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.concurrent.Executors;

public class CameraFragment extends Fragment {

    private final String TAG = CameraFragment.class.getSimpleName();

    private MainActivity mainActivity;
    private FragmentCameraBinding binding;
    private CameraViewModel cameraViewModel;
    private CameraWaypointsViewModel waypointsModel;
    private BluetoothViewModel mBluetoothViewModel;
    private NavController cameraNavController;
    private PanasonicCameraDiscovery cameraDiscovery;


    @SuppressLint("SetTextI18n")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        mainActivity = (MainActivity) getActivity();
        binding = FragmentCameraBinding.inflate(inflater, container, false);

        FeyiuCommandQueue.assignBluetoothService(mainActivity.mBluetoothLeService);

        cameraViewModel = new ViewModelProvider(requireActivity()).get(CameraViewModel.class);
        cameraViewModel.status.observe(getViewLifecycleOwner(), binding.textCameraStatus::setText);
        cameraViewModel.camera.observe(getViewLifecycleOwner(), new CameraObserver(getContext(), binding));
        cameraViewModel.focus.observe(getViewLifecycleOwner(), new CameraFocusObserver(binding));
        cameraViewModel.isStreaming.observe(getViewLifecycleOwner(), value -> binding.buttonCameraConnect.setIconResource(value ? R.drawable.ic_baseline_disconnect_24 : R.drawable.ic_baseline_connect_24));

        mBluetoothViewModel = new ViewModelProvider(requireActivity()).get(BluetoothViewModel.class);
        mBluetoothViewModel.connected.observe(getViewLifecycleOwner(), new BluetoothConnectivityObserver(binding));
        mBluetoothViewModel.enabled.observe(getViewLifecycleOwner(), new BluetoothEnabledObserver(mainActivity));

        waypointsModel = new ViewModelProvider(requireActivity()).get(CameraWaypointsViewModel.class);
        waypointsModel.debugMessage.observe(getViewLifecycleOwner(), s -> binding.textDebug.setText(s));

        mBluetoothViewModel.feyiuStateUpdated.observe(getViewLifecycleOwner(), mFeyiuStateObserver);

        binding.buttonGimbalCenter.setOnClickListener(view -> {
            (new CenterCommand(mainActivity.mBluetoothLeService)).run();
        });

        binding.buttonCameraConnect.setOnClickListener(view -> {
            if (cameraViewModel.streamIsStarted()) {
                stopLiveView();
            } else {
                connectToCamera();
            }
        });

        binding.buttonCameraTakePhoto.setOnClickListener(new CameraControlClickListener(cameraViewModel, camera -> {
            Executors.newSingleThreadExecutor().execute(() ->
                    camera.controls.takePicture(new CameraControlListener())
            );
        }));

        binding.buttonCameraRecordVideo.setOnClickListener(new CameraControlClickListener(cameraViewModel, camera -> {
            Executors.newSingleThreadExecutor().execute(() ->
                    camera.controls.toggleVideoRecording(new CameraControlListener())
            );
        }));

        binding.buttonCameraFocus.setOnClickListener(new CameraFocusClickListener(cameraViewModel));

        if (cameraViewModel.streamIsStarted()) {
            Log.d(TAG, "Stream is already started, continuing");
            continueLiveView();
        } else {
            connectToCamera();
        }

        // This MUST be NavHostFragment, not just Fragment.
        NavHostFragment navHostFragment = (NavHostFragment)
                getChildFragmentManager().findFragmentById(R.id.nav_host_fragment_camera);

        // Obtain the NavController
        cameraNavController = navHostFragment.getNavController();

        BottomNavigationView cameraBottomNav = binding.cameraBottomNav;
        NavigationUI.setupWithNavController(cameraBottomNav, cameraNavController);

        BatteryLevelView batteryView = binding.batteryView;
        batteryView.setBatteryLevel(50); // Set to 50%

        return binding.getRoot();
    }

    @Override
    public void onPause() {
        super.onPause();
        waypointsModel.waypointList.save(mainActivity);

        Log.w(TAG, "Camera fragment resume!");
    }


    @Override
    public void onResume() {
        super.onResume();


        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Executors.newSingleThreadExecutor().execute(() -> {
                waypointsModel.waypointList.load(mainActivity);
                Log.w(TAG, "Waypoints loaded in background");
            });
        }, 500); // Delay of 500 milliseconds (adjust as needed)

        Log.w(TAG, "Camera fragment resume!");
    }

    private void connectToCamera() {
        if (cameraDiscovery == null) {
            cameraDiscovery = new PanasonicCameraDiscovery(mainActivity.executor);
        }

        cameraDiscovery.clear();
        // getContext() can be null on other tabs!
        cameraDiscovery.search(mainActivity, new CameraDiscoveryListener(cameraViewModel, this::startLiveView, this::connectToCamera));
    }

    public void startLiveView() {
        cameraViewModel.status.postValue("Enabling camera controls...");
        PanasonicCamera camera = cameraViewModel.camera.getValue();
        camera.setStateListener(state -> cameraViewModel.camera.postValue(cameraViewModel.camera.getValue()));

        camera.controls.enable(new ICameraControlListener() {
            @Override
            public void onSuccess() {
                cameraViewModel.status.postValue("Setting stream resolution...");

                camera.controls.setStreamResolution(true, new ICameraControlListener() {
                    @Override
                    public void onSuccess() {
                        cameraViewModel.status.postValue("Starting stream...");
                        camera.controls.startStream(new CameraStartStreamListener(getViewLifecycleOwner(), mainActivity, cameraViewModel, binding, () -> {
                            // Runnable when stream has started successfully
                            //                    setLiveImageProcessors(cameraViewModel.liveFeedReceiver.getValue());
                        }));
                    }

                    @Override
                    public void onFailure() {
                        cameraViewModel.status.postValue("Could set livestream resolution.");
                    }
                });
            }

            @Override
            public void onFailure() {
                cameraViewModel.status.postValue("Could not enable camera controls.");
            }
        });
    }

    private FrameProcessor getCurrentFrameProcessor() {
        CameraLiveFeedReceiver r = cameraViewModel.liveFeedReceiver.getValue();

        if (r != null) {
            return r.frameProcessorDispatcher.getCurrentProcessor();
        }

        Log.e(TAG, "Cant get current frame processor because life feed receiver is null");
        return null;
    }


    /**
     * Observer for Gimbal state updates
     */
    final Observer<Long> mFeyiuStateObserver = new Observer<Long>() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onChanged(Long timestamp) {
            binding.textGimbalSpeeds.setText(String.format("%s, %s, %s",
                    FeyiuState.getInstance().angle_pan.speedToString(),
                    FeyiuState.getInstance().angle_tilt.speedToString(),
                    FeyiuState.getInstance().angle_yaw.speedToString())
            );


            binding.textGimbalAngles.setText(Html.fromHtml(String.format("%s, %s, %s",
                    FeyiuState.getInstance().angle_pan.posToString(),
                    FeyiuState.getInstance().angle_tilt.posToString(),
                    FeyiuState.getInstance().angle_yaw.posToString())
            ));
        }
    };

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
        CameraLiveFeedReceiver feedReceiver = cameraViewModel.liveFeedReceiver.getValue();
        if (feedReceiver != null) {
            feedReceiver.onStop("Fragment has been destroyed.");
        }

        PanasonicCamera camera = cameraViewModel.camera.getValue();

        if (camera != null && camera.getLiveView() != null) {
            camera.getLiveView().stop();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cancelAll();
    }

    private void cancelAll() {
        stopLiveView();
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

