package com.feyiuremote.libs.Bluetooth;

import android.bluetooth.le.ScanResult;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.FeyiuUtils;
import com.feyiuremote.ui.gimbal.GimbalEmulator;

import java.util.ArrayList;
import java.util.HashMap;

public class BluetoothViewModel extends ViewModel {

    public final MutableLiveData<ArrayList<ScanResult>> scanResults;
    public final MutableLiveData<String> statusMessage;
    public final MutableLiveData<String> connectionStatus;
    public final MutableLiveData<Boolean> busy;

    public final MutableLiveData<Boolean> connected;
    public final MutableLiveData<Boolean> enabled;
    public final MutableLiveData<Boolean> services_discovered;

    public final HashMap<String, MutableLiveData<byte[]>> characteristics;

    public final MutableLiveData<Boolean> isEmulated = new MutableLiveData<>();

//    public final List<String> CHARACTERISTIC_IDS = new List<String>(){"0000ff02-0000-1000-8000-00805f9b34fb"};

    public BluetoothViewModel() {
        this.scanResults = new MutableLiveData<>();
        this.statusMessage = new MutableLiveData<>();
        this.connected = new MutableLiveData<>();
        this.connectionStatus = new MutableLiveData<>();
        this.enabled = new MutableLiveData<>();
        this.busy = new MutableLiveData<>();
        this.services_discovered = new MutableLiveData<>();
        this.characteristics = new HashMap<>();

        this.statusMessage.setValue("Idle...");
        this.connected.setValue(false);
        this.enabled.setValue(true);
        this.services_discovered.setValue(false);
        this.isEmulated.setValue(false);
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