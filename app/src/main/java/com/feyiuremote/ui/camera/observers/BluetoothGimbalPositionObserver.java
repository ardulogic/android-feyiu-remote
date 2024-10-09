package com.feyiuremote.ui.camera.observers;

import android.text.Html;

import androidx.lifecycle.Observer;

import com.feyiuremote.databinding.FragmentCameraBinding;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.processors.position.GimbalWaypointsProcessor;

public class BluetoothGimbalPositionObserver implements Observer<byte[]> {
    private final String TAG = BluetoothGimbalPositionObserver.class.getSimpleName();
    private final FragmentCameraBinding binding;
    private final GimbalWaypointsProcessor waypointsProcessor;

    public BluetoothGimbalPositionObserver(FragmentCameraBinding binding, GimbalWaypointsProcessor waypointsProcessor) {
        this.binding = binding;
        this.waypointsProcessor = waypointsProcessor;
    }

    @Override
    public void onChanged(byte[] bytes) {
        FeyiuState.getInstance().update(bytes);

        binding.textGimbalSpeeds.setText(String.format("%s, %s, %s",
                FeyiuState.getInstance().angle_pan.speedToString(),
                FeyiuState.getInstance().angle_tilt.speedToString(),
                FeyiuState.getInstance().angle_yaw.speedToString())
        );


        binding.textGimbalAngles.setText(Html.fromHtml(String.format("%s, %s, %s",
                FeyiuState.getInstance().angle_pan.posToString(),
                FeyiuState.getInstance().angle_tilt.posToString(),
                FeyiuState.getInstance().angle_yaw.posToString())
        ));

        if (waypointsProcessor != null) {
            waypointsProcessor.onGimbalUpdate();

            if (waypointsProcessor.getTarget() != null) {
                binding.textGimbalSpeeds.setText(waypointsProcessor.getTarget().toString());
            }
        }
    }
}
