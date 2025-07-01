package com.feyiuremote.libs.Bluetooth;

import android.bluetooth.le.ScanResult;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.FeyiuUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class BluetoothViewModel extends ViewModel {

    public final MutableLiveData<ArrayList<ScanResult>> scanResults;
    public final MutableLiveData<String> statusMessage;
    public final MutableLiveData<String> connectionStatus;
    public final MutableLiveData<Boolean> busy;
    public final MutableLiveData<Boolean> connected;
    public final MutableLiveData<Boolean> enabled;
    public final MutableLiveData<Boolean> services_discovered;
    private final HashMap<String, MutableLiveData<byte[]>> characteristics;
    public final MutableLiveData<Boolean> isEmulated = new MutableLiveData<>();
    public final MutableLiveData<Long> feyiuStateUpdated = new MutableLiveData<>();
    public final MutableLiveData<String> ssid = new MutableLiveData<>();

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
            characteristics.putIfAbsent(id, new MutableLiveData<>());
        }
    }

    public void onCharacteristicsUpdate(String id, byte[] bytes) {
        if (characteristics.containsKey(id)) {
            characteristics.get(id).postValue(bytes);
        } else {
            characteristics.put(id, new MutableLiveData<byte[]>(bytes));
        }

        // Don't want to update the feyiuInstance left and right, centralized updated instead.
        if (Objects.equals(id, FeyiuUtils.NOTIFICATION_CHARACTERISTIC_ID)) {
            FeyiuState.getInstance().update(bytes);

            // This can be safely observed from wherever
            feyiuStateUpdated.setValue(System.currentTimeMillis()); // or use postValue from background thread
        }
    }
}