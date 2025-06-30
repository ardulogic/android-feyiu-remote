package com.feyiuremote.ui.camera.observers;

import android.content.Context;
import android.view.View;

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
        if (c.state.battery != null) {
            if (c.state.battery.contains("1/3")) {
                this.binding.batteryView.setBatteryLevel(30);
            } else if (c.state.battery.contains("2/3")) {
                this.binding.batteryView.setBatteryLevel(60);
            } else if (c.state.battery.contains("3/3")) {
                this.binding.batteryView.setBatteryLevel(100);
            }
            this.binding.batteryView.setVisibility(View.VISIBLE);
        } else {
            this.binding.batteryView.setVisibility(View.GONE);
        }

        if (c.state.isRecording) {
            binding.buttonCameraRecordVideo.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.button_recording));
        } else {
            binding.buttonCameraRecordVideo.setBackgroundTintList(null);
        }

        if (c.state.sdCardisAvailable != null) {
            if (c.state.sdCardisAvailable) {

                if (c.state.remCapacityVideoSeconds > 0) {
                    int minutes = c.state.remCapacityVideoSeconds / 60;
                    int seconds = c.state.remCapacityVideoSeconds % 60;
                    String formatted = String.format("%02d:%02d", minutes, seconds);
                    binding.textCameraVideoRemaining.setTextColor(context.getResources().getColor(R.color.text_default));
                    binding.textCameraVideoRemaining.setText(formatted);
                } else {
                    binding.textCameraVideoRemaining.setTextColor(context.getResources().getColor(R.color.text_error));
                    binding.textCameraVideoRemaining.setText("FULL");
                }
            } else {
                binding.textCameraVideoRemaining.setTextColor(context.getResources().getColor(R.color.text_error));
                binding.textCameraVideoRemaining.setText("NO SD!");
            }
        }
    }
}