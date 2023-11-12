package com.feyiuremote.ui.camera.observers;

import com.feyiuremote.R;
import com.feyiuremote.databinding.FragmentCameraBinding;
import com.feyiuremote.libs.Cameras.Panasonic.PanasonicCamera;

import androidx.lifecycle.Observer;

public class CameraObserver implements Observer<PanasonicCamera> {

    private final FragmentCameraBinding binding; // Replace 'YourBindingType' with the actual type of your 'binding' variable

    public CameraObserver(FragmentCameraBinding binding) {
        this.binding = binding;
    }

    @Override
    public void onChanged(PanasonicCamera c) {
        if (c.state.isRecording) {
            binding.buttonCameraTakeVideo.setIconResource(R.drawable.record_active);
            binding.buttonCameraTakeVideo.setText("STOP");
        } else {
            binding.buttonCameraTakeVideo.setIconResource(R.drawable.record_paused);
            binding.buttonCameraTakeVideo.setText("REC");
        }
    }
}