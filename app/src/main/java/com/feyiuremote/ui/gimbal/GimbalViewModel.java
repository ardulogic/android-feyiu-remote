package com.feyiuremote.ui.gimbal;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class GimbalViewModel extends ViewModel {

    public final MutableLiveData<String> mHomeStatus;

    public GimbalViewModel() {
        this.mHomeStatus = new MutableLiveData<>();
    }

}