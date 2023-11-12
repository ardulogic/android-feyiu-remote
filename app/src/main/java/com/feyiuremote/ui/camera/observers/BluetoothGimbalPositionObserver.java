package com.feyiuremote.ui.camera.observers;

import android.text.Html;
import android.util.Log;

import com.feyiuremote.databinding.FragmentCameraBinding;
import com.feyiuremote.libs.Feiyu.FeyiuControls;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.processors.position.GimbalWaypointsProcessor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import androidx.lifecycle.Observer;

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

        if (waypointsProcessor != null) {
            waypointsProcessor.onGimbalUpdate();
            Log.i(TAG, "Waypoints processor tick");
        }

        Executor executor = Executors.newSingleThreadExecutor();
        // Code to run in the background
        executor.execute(FeyiuControls::tick);

        binding.textGimbalSpeeds.setText(String.format("%s, %s, %s",
                FeyiuState.getInstance().angle_pan.speedToString(),
                FeyiuState.getInstance().angle_tilt.speedToString(),
                FeyiuState.getInstance().angle_yaw.speedToString())
        );

        if (waypointsProcessor.getTarget() != null) {
            binding.textGimbalSpeeds.setText(waypointsProcessor.getTarget().toString());
        }

        binding.textGimbalAngles.setText(Html.fromHtml(String.format("%s, %s, %s",
                FeyiuState.getInstance().angle_pan.posToString(),
                FeyiuState.getInstance().angle_tilt.posToString(),
                FeyiuState.getInstance().angle_yaw.posToString())
        ));
    }
}
