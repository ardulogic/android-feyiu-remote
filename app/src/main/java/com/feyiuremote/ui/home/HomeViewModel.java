package com.feyiuremote.ui.home;

import android.bluetooth.le.ScanResult;

import java.util.ArrayList;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {

    public final MutableLiveData<String> mHomeStatus;

    public HomeViewModel() {
        this.mHomeStatus = new MutableLiveData<>();
    }

}