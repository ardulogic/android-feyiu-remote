package com.feyiuremote.ui.dashboard;

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
import com.feyiuremote.databinding.FragmentDashboardBinding;
import com.feyiuremote.libs.AI.views.RectangleDrawView;
import com.feyiuremote.libs.Cameras.Panasonic.IPanasonicCameraDiscoveryListener;
import com.feyiuremote.libs.Cameras.Panasonic.PanasonicCamera;
import com.feyiuremote.libs.Cameras.Panasonic.PanasonicCameraDiscovery;
import com.feyiuremote.libs.Cameras.abstracts.Connection.ICameraControlListener;
import com.feyiuremote.libs.LiveStream.interfaces.ILiveFeedStatusListener;
import com.feyiuremote.libs.LiveStream.LiveImageView;
import com.feyiuremote.libs.LiveStream.image.LiveFeedReceiver;
import com.feyiuremote.libs.LiveStream.processors.ObjectTrackingProcessor;

public class DashboardFragment extends Fragment {

    private String TAG = DashboardFragment.class.getSimpleName();

    private FragmentDashboardBinding binding;
    private DashboardViewModel dashboardViewModel;
    private LiveImageView mLiveView;
    private ObjectTrackingProcessor mObjectTrackingProcessor;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textCameraStatus;
        final RectangleDrawView rectDrawView = binding.rectDrawView;

        dashboardViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        MainActivity mainActivity = (MainActivity) getActivity();
        PanasonicCameraDiscovery cameraDiscovery = new PanasonicCameraDiscovery(mainActivity.executor);

        mLiveView = binding.liveView;

        mObjectTrackingProcessor = new ObjectTrackingProcessor(binding.rectDrawView, mainActivity.executor);

        binding.buttonCameraConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraDiscovery.clear();

                cameraDiscovery.search(new IPanasonicCameraDiscoveryListener() {
                    @Override
                    public void onDeviceFound(PanasonicCamera camera) {
                        Log.d(TAG, "onDeviceFound:" + camera.state.url);

                        camera.updateBaseInfo(new ICameraControlListener() {
                            @Override
                            public void onSuccess() {
                                startLiveView(camera);
                            }

                            @Override
                            public void onFailure() {
                                dashboardViewModel.mText.postValue("Could not update base info!");
                            }
                        });
                    }

                    @Override
                    public void onProgressUpdate(String response) {
                        dashboardViewModel.mText.postValue(response);
                    }

                    @Override
                    public void onFailure(String response) {
                        dashboardViewModel.mText.postValue(response);
                    }
                });
            }
        });
        return root;
    }

    public void startLiveView(PanasonicCamera camera) {
        dashboardViewModel.mText.postValue("Enabling camera controls...");

        camera.controls.enable(new ICameraControlListener() {
            @Override
            public void onSuccess() {
                dashboardViewModel.mText.postValue("Starting stream...");

                camera.controls.startStream(new ICameraControlListener() {
                    @Override
                    public void onSuccess() {
                        LiveFeedReceiver liveFeedReceiver = new LiveFeedReceiver(getContext(), new ILiveFeedStatusListener() {
                            @Override
                            public void onProgress(String message) {
                                dashboardViewModel.mText.postValue(message);
                                mLiveView.refresh();
                            }
                        });
                        liveFeedReceiver.setImageProcessor(mObjectTrackingProcessor);

                        dashboardViewModel.mText.postValue("Giving a bit of time...");
                        try {
                            Thread.sleep(1000);
                            dashboardViewModel.mText.postValue("Stream started");
                            mLiveView.setLiveFeedReceiver(liveFeedReceiver);

                            camera.createLiveView(liveFeedReceiver);
                            camera.getLiveView().start();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure() {
                        dashboardViewModel.mText.postValue("Failed to start stream");
                    }
                });
            }

            @Override
            public void onFailure() {
                dashboardViewModel.mText.postValue("Failed to establish controls");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}