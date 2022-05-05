package com.feyiuremote.ui.camera;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.feyiuremote.MainActivity;
import com.feyiuremote.databinding.FragmentCameraBinding;
import com.feyiuremote.libs.AI.views.RectangleDrawView;
import com.feyiuremote.libs.Cameras.Panasonic.IPanasonicCameraDiscoveryListener;
import com.feyiuremote.libs.Cameras.Panasonic.PanasonicCamera;
import com.feyiuremote.libs.Cameras.Panasonic.PanasonicCameraDiscovery;
import com.feyiuremote.libs.Cameras.abstracts.Connection.ICameraControlListener;
import com.feyiuremote.libs.Feiyu.processors.GimbalFollowProcessor;
import com.feyiuremote.libs.LiveStream.LiveImageView;
import com.feyiuremote.libs.LiveStream.image.LiveFeedReceiver;
import com.feyiuremote.libs.LiveStream.interfaces.ILiveFeedStatusListener;
import com.feyiuremote.libs.LiveStream.processors.PoseTrackingProcessor;

import java.util.ArrayList;

public class CameraFragment extends Fragment {

    private String TAG = CameraFragment.class.getSimpleName();

    private FragmentCameraBinding binding;
    private CameraViewModel cameraViewModel;
    private PoseTrackingProcessor mObjectTrackingProcessor;

    private MainActivity mainActivity;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        cameraViewModel = new ViewModelProvider(requireActivity()).get(CameraViewModel.class);

        binding = FragmentCameraBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        cameraViewModel.getText().observe(getViewLifecycleOwner(), binding.textCameraStatus::setText);
        cameraViewModel.getConnectButtonText().observe(getViewLifecycleOwner(), binding.buttonCameraConnect::setText);
        cameraViewModel.getConnectButtonIsEnabled().observe(getViewLifecycleOwner(), binding.buttonCameraConnect::setEnabled);

        mainActivity = (MainActivity) getActivity();
        PanasonicCameraDiscovery cameraDiscovery = new PanasonicCameraDiscovery(mainActivity.executor);

        binding.buttonCameraConnect.setOnClickListener(view -> {
            if (cameraViewModel.streamIsStarted()) {
                binding.buttonCameraConnect.setText("Connect");
                stopLiveView();
                return;
            }

            cameraViewModel.buttonText.postValue("Connecting...");
            cameraViewModel.buttonEnabled.postValue(false);

            cameraDiscovery.clear();
            cameraDiscovery.search(new IPanasonicCameraDiscoveryListener() {
                @Override
                public void onDeviceFound(PanasonicCamera camera) {
                    Log.d(TAG, "onDeviceFound:" + camera.state.url);
                    cameraViewModel.camera.postValue(camera);

                    // Requesting basic information from camera
                    // this proves that its properly connected
                    camera.updateBaseInfo(new ICameraControlListener() {
                        @Override
                        public void onSuccess() {
                            startLiveView(cameraViewModel.camera.getValue());
                            cameraViewModel.buttonText.postValue("Disconnect");
                            cameraViewModel.buttonEnabled.postValue(true);
                        }

                        @Override
                        public void onFailure() {
                            cameraViewModel.text.postValue("Could not update base info!");
                            cameraViewModel.buttonText.postValue("Connect");
                            cameraViewModel.buttonEnabled.postValue(true);
                        }
                    });
                }

                @Override
                public void onProgressUpdate(String response) {
                    cameraViewModel.text.postValue(response);
                }

                @Override
                public void onFailure(String response) {
                    cameraViewModel.text.postValue(response);
                }

                @Override
                public void onFinish(ArrayList<String> foundCamUrls) {
                    if (foundCamUrls.isEmpty()) {
                        cameraViewModel.buttonText.postValue("Connect");
                        cameraViewModel.buttonEnabled.postValue(true);
                    }
                }
            });
        });

        binding.buttonSaveCurve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GimbalFollowProcessor p = new GimbalFollowProcessor(mainActivity.mBluetoothLeService);
                p.setTrackingCurveParams(0, new Float[]{
                        Float.parseFloat(binding.editCurveA.getText().toString()),
                        Float.parseFloat(binding.editCurveB.getText().toString()),
                        Float.parseFloat(binding.editCurveC.getText().toString())
                });
                p.setTrackingCurve(0);

                mObjectTrackingProcessor.setOnPoiUpdateListener(new GimbalFollowProcessor(mainActivity.mBluetoothLeService));
            }
        });

        if (cameraViewModel.streamIsStarted()) {
            continueLiveView();
        }

        return root;
    }

    /**
     * Rebind existing live stream to newly created fragment
     */
    public void continueLiveView() {
        binding.liveView.setLiveFeedReceiver(cameraViewModel.liveFeedReceiver);
        cameraViewModel.liveFeedReceiver.setStatusListener(createLiveFeedStatusListener());
    }

    public void stopLiveView() {
        PanasonicCamera camera = cameraViewModel.camera.getValue();

        if (camera != null && camera.getLiveView() != null) {
            camera.getLiveView().stop();
        }
    }

    public void startLiveView(PanasonicCamera camera) {
        cameraViewModel.text.postValue("Enabling camera controls...");

        camera.controls.enable(new ICameraControlListener() {
            @Override
            public void onSuccess() {
                cameraViewModel.text.postValue("Starting stream...");

                camera.controls.startStream(new ICameraControlListener() {
                    @Override
                    public void onSuccess() {
                        cameraViewModel.streamStarted.postValue(true);

                        try {
                            cameraViewModel.text.postValue("Giving a bit of time...");
                            Thread.sleep(1000);
                            cameraViewModel.text.postValue("Stream started");

                            cameraViewModel.liveFeedReceiver = createLiveFeedReceiver();
                            binding.liveView.setLiveFeedReceiver(cameraViewModel.liveFeedReceiver);

                            camera.createLiveView(cameraViewModel.liveFeedReceiver);
                            camera.getLiveView().start();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure() {
                        cameraViewModel.streamStarted.postValue(false);
                        cameraViewModel.text.postValue("Failed to start stream");
                    }
                });
            }

            @Override
            public void onFailure() {
                cameraViewModel.text.postValue("Failed to establish controls");
            }
        });
    }

    /**
     * Creates Receiver & Process or live feed from camera
     *
     * @return
     */
    public LiveFeedReceiver createLiveFeedReceiver() {
        LiveFeedReceiver liveFeedReceiver = new LiveFeedReceiver(getContext());
        liveFeedReceiver.setStatusListener(createLiveFeedStatusListener());

        mObjectTrackingProcessor = new PoseTrackingProcessor(getContext(), mainActivity.executor);
        mObjectTrackingProcessor.setOnPoiUpdateListener(new GimbalFollowProcessor(mainActivity.mBluetoothLeService));
        liveFeedReceiver.setImageProcessor(mObjectTrackingProcessor);

        return liveFeedReceiver;
    }

    public ILiveFeedStatusListener createLiveFeedStatusListener() {
        return message -> {
            cameraViewModel.text.postValue(message);

            if (binding != null) {
                binding.liveView.refresh();
            } else {
                Log.d(TAG, "liveView is Null");
            }
        };
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}