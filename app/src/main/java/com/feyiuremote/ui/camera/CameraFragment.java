package com.feyiuremote.ui.camera;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.feyiuremote.MainActivity;
import com.feyiuremote.databinding.FragmentCameraBinding;
import com.feyiuremote.libs.Bluetooth.BluetoothViewModel;
import com.feyiuremote.libs.Cameras.Panasonic.PanasonicCamera;
import com.feyiuremote.libs.Cameras.Panasonic.PanasonicCameraDiscovery;
import com.feyiuremote.libs.Cameras.abstracts.Connection.ICameraControlListener;
import com.feyiuremote.libs.Feiyu.FeyiuControls;
import com.feyiuremote.libs.Feiyu.FeyiuUtils;
import com.feyiuremote.libs.Feiyu.processors.GimbalFollowProcessor;
import com.feyiuremote.libs.Feiyu.processors.position.GimbalWaypointsProcessor;
import com.feyiuremote.libs.LiveStream.image.LiveFeedReceiver;
import com.feyiuremote.libs.LiveStream.processors.ObjectTrackingProcessor;
import com.feyiuremote.ui.camera.listeners.CameraControlClickListener;
import com.feyiuremote.ui.camera.listeners.CameraDiscoveryListener;
import com.feyiuremote.ui.camera.listeners.CameraFocusClickListener;
import com.feyiuremote.ui.camera.listeners.CameraLiveStreamUpdateListener;
import com.feyiuremote.ui.camera.listeners.CameraStartStreamListener;
import com.feyiuremote.ui.camera.listeners.WaypointAddClickListener;
import com.feyiuremote.ui.camera.observers.BluetoothConnectivityObserver;
import com.feyiuremote.ui.camera.observers.BluetoothGimbalPositionObserver;
import com.feyiuremote.ui.camera.observers.CameraFocusObserver;
import com.feyiuremote.ui.camera.observers.CameraObserver;
import com.feyiuremote.ui.camera.waypoints.ItemTouchHelperCallback;
import com.feyiuremote.ui.camera.waypoints.WaypointListAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;

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
        cameraViewModel.camera.observe(getViewLifecycleOwner(), new CameraObserver(binding));
        cameraViewModel.focus.observe(getViewLifecycleOwner(), new CameraFocusObserver(binding));
        mWaypointsProcessor = new GimbalWaypointsProcessor(mainActivity.getBaseContext(), cameraViewModel.waypointList);

        mBluetoothViewModel = new ViewModelProvider(requireActivity()).get(BluetoothViewModel.class);
        mBluetoothViewModel.connected.observe(getViewLifecycleOwner(), new BluetoothConnectivityObserver(binding));
        mBluetoothViewModel.characteristics.get(FeyiuUtils.NOTIFICATION_CHARACTERISTIC_ID)
                .observe(getViewLifecycleOwner(), new BluetoothGimbalPositionObserver(binding, mWaypointsProcessor));

        PanasonicCameraDiscovery cameraDiscovery = new PanasonicCameraDiscovery(mainActivity.executor);

        binding.buttonCameraConnect.setOnClickListener(view -> {
            if (cameraViewModel.streamIsStarted()) {
                stopLiveView();
            } else {
                cameraDiscovery.clear();
                cameraDiscovery.search(new CameraDiscoveryListener(cameraViewModel, this::startLiveView));
            }
        });

        WaypointListAdapter wpListAdapter = new WaypointListAdapter(mainActivity.getBaseContext(), getViewLifecycleOwner(), cameraViewModel, mWaypointsProcessor);
        binding.listWaypoints.setAdapter(wpListAdapter);
//        // Drag and drop waypoints
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelperCallback(wpListAdapter));
        itemTouchHelper.attachToRecyclerView(binding.listWaypoints);

        binding.buttonAddWaypoint.setOnClickListener(new WaypointAddClickListener(cameraViewModel, mainActivity));
        binding.buttonCameraTakePhoto.setOnClickListener(new CameraControlClickListener(cameraViewModel, camera -> camera.controls.takePicture(new CameraControlListener())));
        binding.buttonCameraTakeVideo.setOnClickListener(new CameraControlClickListener(cameraViewModel, camera -> camera.controls.toggleVideoRecording(new CameraControlListener())));
        binding.buttonCameraFocus.setOnClickListener(new CameraFocusClickListener(cameraViewModel));

        binding.buttonPlayWaypoints.setOnClickListener(view -> {
            mWaypointsProcessor.start(GimbalWaypointsProcessor.MODE_DWELL);
        });

        binding.buttonPlayWaypointsBlend.setOnClickListener(view -> {
            mWaypointsProcessor.start(GimbalWaypointsProcessor.MODE_BLEND);
        });

        if (cameraViewModel.streamIsStarted()) {
            continueLiveView();
        }

        return binding.getRoot();
    }


    public void startLiveView() {
        cameraViewModel.status.postValue("Enabling camera controls...");
        PanasonicCamera camera = cameraViewModel.camera.getValue();
        camera.controls.enable(new ICameraControlListener() {
            @Override
            public void onSuccess() {
                cameraViewModel.status.postValue("Starting stream...");

                camera.controls.startStream(new CameraStartStreamListener(getContext(), cameraViewModel, binding, () -> {
                    bindTrackersToLiveView();
                }));

                mWaypointsProcessor.setCamera(cameraViewModel.camera);
            }

            @Override
            public void onFailure() {
                cameraViewModel.status.postValue("Failed to start stream.");
            }
        });
    }

    private void bindTrackersToLiveView() {
        //  mObjectTrackingProcessor = new PoseTrackingProcessor(getContext(), mainActivity.executor);
        ObjectTrackingProcessor tracker = new ObjectTrackingProcessor(binding.rectDrawView, mainActivity.executor);
        tracker.setOnPoiUpdateListener(new GimbalFollowProcessor(mainActivity.mBluetoothLeService));

        cameraViewModel.liveFeedReceiver.getValue().setImageProcessor(tracker);

        // Prevent two trackers from fighting each other
        mWaypointsProcessor.setOnStartListener(tracker::cancel);

        cameraViewModel.objectTrackingProcessor.postValue(tracker);
    }

    /**
     * Rebind existing live stream to newly created fragment
     */
    public void continueLiveView() {
        // After switching fragments view binding is "disconnected"
        LiveFeedReceiver r = cameraViewModel.liveFeedReceiver.getValue();

        // This reattaches tracker and follow processor
        bindTrackersToLiveView();

        // This refreshes frames on update within receiver
        r.setUpdateListener(new CameraLiveStreamUpdateListener(cameraViewModel, binding));

        // This makes frame appear on the screen
        // calling this only is not sufficient frames wont be updated
        binding.liveView.setLiveFeedReceiver(r);

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
        ObjectTrackingProcessor objTracker = cameraViewModel.objectTrackingProcessor.getValue();
        if (objTracker != null) {
            objTracker.cancel();
        }

        if (mWaypointsProcessor != null) {
            mWaypointsProcessor.cancel();
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

