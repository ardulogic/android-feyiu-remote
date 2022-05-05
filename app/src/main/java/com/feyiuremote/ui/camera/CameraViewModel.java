package com.feyiuremote.ui.camera;

import com.feyiuremote.libs.Cameras.Panasonic.PanasonicCamera;
import com.feyiuremote.libs.LiveStream.image.LiveFeedReceiver;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class CameraViewModel extends ViewModel {

    public final MutableLiveData<String> text = new MutableLiveData<>();
    public final MutableLiveData<String> buttonText = new MutableLiveData<>();
    public final MutableLiveData<Boolean> buttonEnabled = new MutableLiveData<>();

    public final MutableLiveData<PanasonicCamera> camera = new MutableLiveData<>();
    public final MutableLiveData<Boolean> streamStarted = new MutableLiveData<>();
    public LiveFeedReceiver liveFeedReceiver;

    public CameraViewModel() {
        text.setValue("Waiting for camera...");
        buttonText.setValue("Connect");
        streamStarted.setValue(false);
        buttonEnabled.setValue(true);
    }

    public LiveData<String> getText() {
        return text;
    }

    public boolean streamIsStarted() {
        PanasonicCamera c = camera.getValue();

        if (c != null && c.getLiveView() != null) {
            return c.getLiveView().isActive();
        }

        return false;
    }

    public LiveData<String> getConnectButtonText() {
        return buttonText;
    }

    public LiveData<Boolean> getConnectButtonIsEnabled() {
        return buttonEnabled;
    }
}