package com.feyiuremote.ui.camera.fragments.tracker;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.feyiuremote.R;
import com.feyiuremote.databinding.FragmentCameraBinding;
import com.feyiuremote.databinding.FragmentCameraTrackingBinding;
import com.feyiuremote.libs.LiveStream.processors.detectors.GooglePoseDetectorProcessor;
import com.feyiuremote.libs.LiveStream.processors.trackers.BoxTrackingMediapipeProcessor;
import com.feyiuremote.libs.LiveStream.processors.trackers.BoxTrackingOpenCvProcessor;
import com.feyiuremote.ui.camera.models.CameraViewModel;

public class CameraTrackingFragment extends Fragment {

    private final String TAG = CameraTrackingFragment.class.getSimpleName();

    private CameraViewModel cameraViewModel;

    private FragmentCameraTrackingBinding binding;
    private FragmentCameraBinding cameraBinding;

    private boolean isMutatingToggle = false;

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentCameraTrackingBinding.inflate(inflater, container, false);
        cameraBinding = FragmentCameraBinding.inflate(inflater, container, false);

        cameraViewModel = new ViewModelProvider(requireActivity()).get(CameraViewModel.class);

        attachListeners();

        return binding.getRoot();
    }

    private void attachListeners() {
        // Tracker buttons
        binding.toggleGroupTracker.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isMutatingToggle || cameraViewModel.liveFeedReceiver.getValue() == null || !isChecked)
                return;

            isMutatingToggle = true;
            try {
                String processorClass = null;
                if (checkedId == R.id.btn_tracker_mediapipe) {
                    processorClass = BoxTrackingMediapipeProcessor.class.getName();
                } else if (checkedId == R.id.btn_tracker_opencv) {
                    processorClass = BoxTrackingOpenCvProcessor.class.getName();
                }

                cameraViewModel.liveFeedReceiver.getValue().setFrameProcessor(processorClass);
                binding.toggleGroupDetector.clearChecked();
            } finally {
                isMutatingToggle = false;
            }
        });

        // Detector buttons
        binding.toggleGroupDetector.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isMutatingToggle || cameraViewModel.liveFeedReceiver.getValue() == null || !isChecked)
                return;

            isMutatingToggle = true;
            try {
                String processorClass = null;
                if (checkedId == R.id.btn_detector_pose) {
                    processorClass = GooglePoseDetectorProcessor.class.getName();
                }

                cameraViewModel.liveFeedReceiver.getValue().setFrameProcessor(processorClass);
                binding.toggleGroupTracker.clearChecked();
            } finally {
                isMutatingToggle = false;
            }
        });
    }

}
