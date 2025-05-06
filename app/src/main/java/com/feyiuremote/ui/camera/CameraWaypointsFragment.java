package com.feyiuremote.ui.camera;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;

import com.feyiuremote.MainActivity;
import com.feyiuremote.R;
import com.feyiuremote.databinding.FragmentCameraWaypointsBinding;
import com.feyiuremote.libs.Bluetooth.BluetoothViewModel;
import com.feyiuremote.libs.Feiyu.processors.position.GimbalWaypointsProcessor;
import com.feyiuremote.ui.camera.listeners.WaypointAddClickListener;
import com.feyiuremote.ui.camera.models.CameraViewModel;
import com.feyiuremote.ui.camera.models.CameraWaypointsViewModel;
import com.feyiuremote.ui.camera.waypoints.ItemTouchHelperCallback;
import com.feyiuremote.ui.camera.waypoints.WaypointListAdapter;

import java.util.Objects;

public class CameraWaypointsFragment extends Fragment {

    private final String TAG = CameraWaypointsFragment.class.getSimpleName();

    private FragmentCameraWaypointsBinding binding;
    private MainActivity mainActivity;
    private CameraViewModel cameraViewModel;
    private CameraWaypointsViewModel waypointsViewModel;
    private BluetoothViewModel mBluetoothViewModel;

    private GimbalWaypointsProcessor mWaypointsProcessor;
    private WaypointListAdapter wpListAdapter;

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentCameraWaypointsBinding.inflate(inflater, container, false);

        mainActivity = (MainActivity) getActivity();
        waypointsViewModel = new ViewModelProvider(requireActivity()).get(CameraWaypointsViewModel.class);
        cameraViewModel = new ViewModelProvider(requireActivity()).get(CameraViewModel.class);

        mWaypointsProcessor = new GimbalWaypointsProcessor(getContext(), waypointsViewModel.waypointList);
        waypointsViewModel.processor.setValue(mWaypointsProcessor);
        waypointsViewModel.processor.observe(getViewLifecycleOwner(), gimbalWaypointsProcessor -> {
            mWaypointsProcessor = gimbalWaypointsProcessor;
        });

        mWaypointsProcessor.setCamera(cameraViewModel.camera);

        mBluetoothViewModel = new ViewModelProvider(requireActivity()).get(BluetoothViewModel.class);
        mBluetoothViewModel.feyiuStateUpdated.observe(getViewLifecycleOwner(), mFeyiuStateObserver);

        // Drag and drop waypoints
        wpListAdapter = new WaypointListAdapter(getContext(), getViewLifecycleOwner(), waypointsViewModel);
        binding.listWaypoints.setAdapter(wpListAdapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelperCallback(wpListAdapter));
        itemTouchHelper.attachToRecyclerView(binding.listWaypoints);

        mWaypointsProcessor.setStateListener((mode, isActive) -> {
            waypointsViewModel.isPlayLoopActive.postValue(isActive && Objects.equals(mode, GimbalWaypointsProcessor.MODE_PLAY_LOOP));
            waypointsViewModel.isPlayOnceActive.postValue(isActive && Objects.equals(mode, GimbalWaypointsProcessor.MODE_PLAY_ONCE));
        });

        waypointsViewModel.isPlayLoopActive.observe(getViewLifecycleOwner(), isPlayLoopActive -> {
            int icon = isPlayLoopActive ? R.drawable.ic_baseline_play_on_24 : R.drawable.ic_baseline_play_24;
            int iconTint = ContextCompat.getColor(mainActivity, isPlayLoopActive ? R.color.icon_light_green : R.color.icon_default);

            binding.buttonWaypointPlayLoop.setIconResource(icon);
            binding.buttonWaypointPlayLoop.setIconTint(ColorStateList.valueOf(iconTint));
            binding.buttonWaypointPlayLoop.setChecked(isPlayLoopActive);
        });

        waypointsViewModel.isPlayOnceActive.observe(getViewLifecycleOwner(), isPlayOnceActive -> {
            int icon = isPlayOnceActive ? R.drawable.ic_baseline_play_on_24 : R.drawable.ic_baseline_play_24;
            int iconTint = ContextCompat.getColor(mainActivity, isPlayOnceActive ? R.color.icon_light_green : R.color.icon_default);

            binding.buttonWaypointPlayOnce.setIconResource(icon);
            binding.buttonWaypointPlayOnce.setIconTint(ColorStateList.valueOf(iconTint));
            binding.buttonWaypointPlayOnce.setChecked(isPlayOnceActive);
        });

        binding.buttonWaypointAdd.setOnClickListener(new WaypointAddClickListener(cameraViewModel, waypointsViewModel, mainActivity));

        binding.buttonWaypointPlayOnce.setOnClickListener(view -> {
            if (Boolean.TRUE.equals(mBluetoothViewModel.connected.getValue()) && !mWaypointsProcessor.isActive) {
                mWaypointsProcessor.start(GimbalWaypointsProcessor.MODE_PLAY_ONCE);
            } else {
                mWaypointsProcessor.stop();
            }
        });

        binding.buttonWaypointPlayLoop.setOnClickListener(view -> {
            if (Boolean.TRUE.equals(mBluetoothViewModel.connected.getValue()) && !mWaypointsProcessor.isActive) {
                mWaypointsProcessor.start(GimbalWaypointsProcessor.MODE_PLAY_LOOP);
            } else {
                mWaypointsProcessor.stop();
            }
        });

//        binding.buttonPlayWaypointsBlend.setOnClickListener(view -> {
//            unifiedTrackingProcessor.mWaypointsProcessor.toggleFlag(GimbalWaypointsProcessor.FLAG_DWELL);
//        });

        return binding.getRoot();
    }

    /**
     * Observer for Gimbal state updates
     */
    final Observer<Long> mFeyiuStateObserver = new Observer<Long>() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onChanged(Long timestamp) {
            if (mWaypointsProcessor != null) {
                mWaypointsProcessor.onGimbalUpdate();

                if (mWaypointsProcessor.getTarget() != null) {
                    // Outputs debug values
                    waypointsViewModel.debugMessage.postValue(mWaypointsProcessor.toString());
                }
            }
        }
    };
}
