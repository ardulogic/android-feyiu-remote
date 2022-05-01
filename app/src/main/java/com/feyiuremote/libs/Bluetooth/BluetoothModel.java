package com.feyiuremote.libs.Bluetooth;

import android.bluetooth.le.ScanResult;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class BluetoothModel extends ViewModel {

    public final MutableLiveData<ArrayList<ScanResult>> mScanResults;
    public final MutableLiveData<String> mStatus;
    public final MutableLiveData<Boolean> mConnected;
    public final MutableLiveData<Boolean> mServicesDiscovered;
    public final HashMap<String, MutableLiveData<byte[]>> mCharacteristics;

//    public final List<String> CHARACTERISTIC_IDS = new List<String>(){"0000ff02-0000-1000-8000-00805f9b34fb"};

    public BluetoothModel() {
        this.mScanResults = new MutableLiveData<>();
        this.mStatus = new MutableLiveData<>();
        this.mConnected = new MutableLiveData<>();
        this.mServicesDiscovered = new MutableLiveData<>();
        this.mCharacteristics = new HashMap<>();

        this.mStatus.setValue("Idle...");
        this.mConnected.setValue(false);
        this.mServicesDiscovered.setValue(false);
    }

    /**
     * Add future observable characteristic
     * @param id
     */
    public void registerCharacteristic(String id) {
        if (!this.mCharacteristics.containsKey(id)) {
            this.mCharacteristics.put(id, new MutableLiveData<>());
        }
    }

}