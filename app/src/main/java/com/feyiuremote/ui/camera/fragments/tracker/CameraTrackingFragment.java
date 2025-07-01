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
import com.feyiuremote.databinding.FragmentCameraTrackingBinding;
import com.feyiuremote.libs.LiveStream.processors.detectors.GooglePoseDetectorProcessor;
import com.feyiuremote.libs.LiveStream.processors.trackers.BoxTrackingMediapipeProcessor;
import com.feyiuremote.libs.LiveStream.processors.trackers.BoxTrackingOpenCvProcessor;
import com.feyiuremote.ui.camera.models.CameraViewModel;
import com.google.android.material.button.MaterialButton;

public class CameraTrackingFragment extends Fragment {

    private final String TAG = CameraTrackingFragment.class.getSimpleName();

    private CameraViewModel cameraViewModel;

    private FragmentCameraTrackingBinding binding;

    private boolean isMutatingToggle = false;

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentCameraTrackingBinding.inflate(inflater, container, false);
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

        // Grid Buttons - Pan and Tilt
        binding.btnPanDecr.setOnClickListener(v -> cameraViewModel.adjPanOffset(-0.05));
        binding.btnPanIncr.setOnClickListener(v -> cameraViewModel.adjPanOffset(+0.05));
        binding.btnTiltDecr.setOnClickListener(v -> cameraViewModel.adjTiltOffset(-0.05));
        binding.btnTiltIncr.setOnClickListener(v -> cameraViewModel.adjTiltOffset(+0.05));

        // Grid Buttons - Rule of Thirds placeholders
        View.OnClickListener ruleClickListener = v -> {
            int id = v.getId();
            switch (id) {
                case R.id.btnRuleTopLeft:
                    cameraViewModel.setPanOffset(-0.20);
                    cameraViewModel.setTiltOffset(0.15);
                    break;
                case R.id.btnRuleTopRight:
                    cameraViewModel.setPanOffset(0.20);
                    cameraViewModel.setTiltOffset(0.15);
                    break;
                case R.id.btnRuleBottomLeft:
                    cameraViewModel.setPanOffset(-0.20);
                    cameraViewModel.setTiltOffset(-0.15);
                    break;
                case R.id.btnRuleBottomRight:
                    cameraViewModel.setPanOffset(0.20);
                    cameraViewModel.setTiltOffset(-0.15);
                    break;
            }
        };

        binding.btnRuleTopLeft.setOnClickListener(ruleClickListener);
        binding.btnRuleTopRight.setOnClickListener(ruleClickListener);
        binding.btnRuleBottomLeft.setOnClickListener(ruleClickListener);
        binding.btnRuleBottomRight.setOnClickListener(ruleClickListener);
        binding.buttonOffsetReset.setOnClickListener(v -> {
            cameraViewModel.setPanOffset(0);
            cameraViewModel.setTiltOffset(0);
        });

        // Live offset update
        cameraViewModel.panOffset.observe(getViewLifecycleOwner(), pan -> {
            updateOffsetsText(pan, cameraViewModel.tiltOffset.getValue());
            updateRuleButtons(pan, cameraViewModel.tiltOffset.getValue());
        });

        cameraViewModel.tiltOffset.observe(getViewLifecycleOwner(), tilt -> {
            updateOffsetsText(cameraViewModel.panOffset.getValue(), tilt);
            updateRuleButtons(cameraViewModel.panOffset.getValue(), tilt);
        });
    }

    private void updateOffsetsText(Double pan, Double tilt) {
        if (pan == null || tilt == null) return;

        binding.buttonOffsetReset.setText(
                String.format("Tilt: %.2f\nPan: %.2f", tilt, pan));
    }

    private void updateRuleButtons(Double pan, Double tilt) {
        if (pan == null || tilt == null) return;

        // Define a small tolerance to account for rounding errors
        final double TOLERANCE = 0.01;

        boolean topLeft = Math.abs(pan + 0.20) < TOLERANCE && Math.abs(tilt - 0.15) < TOLERANCE;
        boolean topRight = Math.abs(pan - 0.20) < TOLERANCE && Math.abs(tilt - 0.15) < TOLERANCE;
        boolean bottomLeft = Math.abs(pan + 0.20) < TOLERANCE && Math.abs(tilt + 0.15) < TOLERANCE;
        boolean bottomRight = Math.abs(pan - 0.20) < TOLERANCE && Math.abs(tilt + 0.15) < TOLERANCE;

        setButtonOutline(binding.btnRuleTopLeft, topLeft);
        setButtonOutline(binding.btnRuleTopRight, topRight);
        setButtonOutline(binding.btnRuleBottomLeft, bottomLeft);
        setButtonOutline(binding.btnRuleBottomRight, bottomRight);
    }

    private void setButtonOutline(MaterialButton button, boolean active) {
        if (active) {
            button.setBackgroundColor(getResources().getColor(R.color.waypoint_active));
        } else {
            button.setBackgroundColor(getResources().getColor(R.color.waypoint_inactive));
        }
    }

}
