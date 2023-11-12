package com.feyiuremote.libs.Bluetooth;

import android.bluetooth.le.ScanResult;

import java.util.ArrayList;
import java.util.HashMap;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class BluetoothViewModel extends ViewModel {

    public final MutableLiveData<ArrayList<ScanResult>> scanResults;
    public final MutableLiveData<String> status_message;
    public final MutableLiveData<String> connection_status;
    public final MutableLiveData<Boolean> busy;

    public final MutableLiveData<Boolean> connected;
    public final MutableLiveData<Boolean> services_discovered;
    public final HashMap<String, MutableLiveData<byte[]>> characteristics;

//    public final List<String> CHARACTERISTIC_IDS = new List<String>(){"0000ff02-0000-1000-8000-00805f9b34fb"};

    public BluetoothViewModel() {
        this.scanResults = new MutableLiveData<>();
        this.status_message = new MutableLiveData<>();
        this.connected = new MutableLiveData<>();
        this.connection_status = new MutableLiveData<>();
        this.services_discovered = new MutableLiveData<>();
        this.busy = new MutableLiveData<>();
        this.characteristics = new HashMap<>();

        this.status_message.setValue("Idle...");
        this.connected.setValue(false);
        this.services_discovered.setValue(false);
    }

    /**
     * Add future observable characteristic
     *
     * @param id
     */
    public void registerCharacteristic(String id) {
        if (!this.characteristics.containsKey(id)) {
            this.characteristics.put(id, new MutableLiveData<>());
        }
    }

}