package com.feyiuremote.ui.notifications;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class NotificationsViewModel extends ViewModel {

    public final MutableLiveData<String> mPosText;
    public final MutableLiveData<String> mCalResText;

    public NotificationsViewModel() {
        mCalResText = new MutableLiveData<>();
        mPosText = new MutableLiveData<>();

        mPosText.setValue("Waiting for position...");
        mCalResText.setValue("Waiting for calibration start...");
    }

    public LiveData<String> getPosText() {
        return mPosText;
    }

    public LiveData<String> getCalResultText() {
        return mCalResText;
    }
}