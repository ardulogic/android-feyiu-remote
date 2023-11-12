package com.feyiuremote.ui.camera.observers;

import android.graphics.Color;

import com.feyiuremote.databinding.FragmentCameraBinding;

import androidx.lifecycle.Observer;

public class BluetoothConnectivityObserver implements Observer<Boolean> {
    private final FragmentCameraBinding binding;

    public BluetoothConnectivityObserver(FragmentCameraBinding binding) {
        this.binding = binding;
    }

    @Override
    public void onChanged(Boolean connected) {
        binding.textGimbalAngles.setTextColor(connected ? Color.GREEN : Color.RED);

    }

}
