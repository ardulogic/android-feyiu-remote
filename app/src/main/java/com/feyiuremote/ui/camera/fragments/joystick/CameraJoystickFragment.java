package com.feyiuremote.ui.camera.fragments.joystick;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.feyiuremote.databinding.FragmentCameraJoystickBinding;
import com.feyiuremote.libs.Feiyu.Axes;
import com.feyiuremote.libs.Feiyu.queue.FeyiuControls;

public class CameraJoystickFragment extends Fragment {

    private final String TAG = CameraJoystickFragment.class.getSimpleName();

    private FragmentCameraJoystickBinding binding;

    @SuppressLint("SetTextI18n")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentCameraJoystickBinding.inflate(inflater, container, false);
        binding.joystick.setDeadzone(15);
        binding.joystick.setJoystickListener((xPercent, yPercent) -> {
            int min = 55;
            int max = 240;

            int panValue = scaledOutput(-xPercent, min, max);
            int tiltValue = scaledOutput(yPercent, min, max); // Invert Y for natural control

            FeyiuControls.setLooseJoyFor(Axes.Axis.PAN, panValue, 5000);
            FeyiuControls.setLooseJoyFor(Axes.Axis.TILT, tiltValue, 5000);

            Log.d("Joystick", String.valueOf(xPercent) + " yP" + String.valueOf(yPercent));
        });

        binding.buttonJoystickSpeedSlow.setOnClickListener(v -> {
            FeyiuControls.setPanSensitivity(25);
            FeyiuControls.setTiltSensitivity(25);
        });

        binding.buttonJoystickSpeedMed.setOnClickListener(v -> {
            FeyiuControls.setPanSensitivity(60);
            FeyiuControls.setTiltSensitivity(60);
        });

        binding.buttonJoystickSpeedFast.setOnClickListener(v -> {
            FeyiuControls.setPanSensitivity(230);
            FeyiuControls.setTiltSensitivity(60);
        });

        init();

        return binding.getRoot();
    }

    private int scaledOutput(float input, int min, int max) {
        if (Math.abs(input) < 0.05f) return 0; // Optional: dead zone

        int direction = input > 0 ? 1 : -1;
        float magnitude = Math.abs(input); // from 0.0 to 1.0

        return direction * (int) (magnitude * (max - min) + min);
    }

    private void init() {
        FeyiuControls.setSensitivity(Axes.Axis.PAN, 25);
        FeyiuControls.setSensitivity(Axes.Axis.TILT, 25);
    }
}
