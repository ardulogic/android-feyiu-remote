package com.feyiuremote.ui.camera;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.feyiuremote.MainActivity;
import com.feyiuremote.databinding.FragmentCameraBinding;
import com.feyiuremote.databinding.FragmentCameraJoystickBinding;

public class CameraJoystickFragment extends Fragment {

    private final String TAG = CameraJoystickFragment.class.getSimpleName();

    private FragmentCameraJoystickBinding binding;

    @SuppressLint("SetTextI18n")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentCameraJoystickBinding.inflate(inflater, container, false);


        return binding.getRoot();
    }
}
