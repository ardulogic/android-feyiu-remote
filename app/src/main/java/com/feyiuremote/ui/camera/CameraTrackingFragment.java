package com.feyiuremote.ui.camera;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;


import com.feyiuremote.databinding.FragmentCameraTrackingBinding;

public class CameraTrackingFragment extends Fragment {

    private final String TAG = CameraTrackingFragment.class.getSimpleName();

    private FragmentCameraTrackingBinding binding;

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentCameraTrackingBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }
}
