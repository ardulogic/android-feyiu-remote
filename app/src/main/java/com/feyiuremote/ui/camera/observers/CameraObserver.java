package com.feyiuremote.ui.camera.observers;

import android.content.Context;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;

import com.feyiuremote.R;
import com.feyiuremote.databinding.FragmentCameraBinding;
import com.feyiuremote.libs.Cameras.Panasonic.PanasonicCamera;

public class CameraObserver implements Observer<PanasonicCamera> {

    private final FragmentCameraBinding binding; // Replace 'YourBindingType' with the actual type of your 'binding' variable
    private final Context context;

    public CameraObserver(Context context, FragmentCameraBinding binding) {
        this.context = context;
        this.binding = binding;
    }

    @Override
    public void onChanged(PanasonicCamera c) {
        if (c.state.isRecording) {
            binding.buttonCameraRecordVideo.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.button_recording));
        } else {
            binding.buttonCameraRecordVideo.setBackgroundTintList(null);

        }
    }
}