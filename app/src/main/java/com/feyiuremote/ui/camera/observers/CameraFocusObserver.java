package com.feyiuremote.ui.camera.observers;

import com.feyiuremote.databinding.FragmentCameraBinding;

import androidx.lifecycle.Observer;

public class CameraFocusObserver implements Observer<Double> {

    private final FragmentCameraBinding binding; // Replace 'YourBindingType' with the actual type of your 'binding' variable

    public CameraFocusObserver(FragmentCameraBinding binding) {
        this.binding = binding;
    }

    @Override
    public void onChanged(Double position) {
        binding.progressBarFocus.setProgress((int) Math.round(100 - position));
        binding.textCameraFocus.setText(String.format("%.1f%%", position));
    }
}