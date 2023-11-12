package com.feyiuremote.libs.Bluetooth;


import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;

import androidx.lifecycle.MutableLiveData;

import static com.feyiuremote.libs.Bluetooth.BluetoothLeService.EXTRA_DATA;
import static com.feyiuremote.libs.Bluetooth.BluetoothLeService.EXTRA_ID;

public class BluetoothLeUpdateReceiver extends BroadcastReceiver {

    private final BluetoothViewModel model;
    private final static String TAG = BluetoothLeUpdateReceiver.class.getSimpleName();

    public BluetoothLeUpdateReceiver(BluetoothViewModel model) {
        super();

        this.model = model;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        switch (action) {
            case BluetoothLeService.ACTION_BT_DISABLED:
                Log.e(TAG, "Bluetooth disabled!");
                model.status_message.setValue("Bluetooth is disabled!");
                model.connected.setValue(false);
                model.services_discovered.setValue(false);
                model.connection_status.setValue(action);
                break;

            case BluetoothLeService.ACTION_GATT_CONNECTED:
                Log.d(TAG, "Connected!");
                model.status_message.setValue("Connected.");
                model.connected.setValue(true);
                model.services_discovered.setValue(false);
                model.connection_status.setValue(action);
                break;

            case BluetoothLeService.ACTION_GATT_CONNECTING:
                Log.d(TAG, "Connecting...");
                model.status_message.setValue("Connecting...");
                model.connected.setValue(false);
                model.services_discovered.setValue(false);
                model.connection_status.setValue(action);
                break;

            case BluetoothLeService.ACTION_GATT_DISCONNECTING:
                Log.d(TAG, "Disconnected!");
                model.status_message.setValue("Disconnecting...");
                model.connection_status.setValue(action);
                break;

            case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                Log.d(TAG, "Disconnected!");
                model.status_message.setValue("Disconnected.");
                model.connected.setValue(false);
                model.services_discovered.setValue(false);
                model.connection_status.setValue(action);
                break;

            case BluetoothLeService.ACTION_SCAN_RESULTS:
                Log.d(TAG, "Scan results updated");
                ArrayList<ScanResult> scanResults = intent.getParcelableArrayListExtra(EXTRA_DATA);
                Log.d(TAG, scanResults.toString());
                model.status_message.postValue("Scan results have been updated");
                model.scanResults.postValue(scanResults);
                break;

            case BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED:
                Log.d(TAG, "Services discovered");
                model.status_message.postValue("Services have been discovered");
                model.services_discovered.setValue(true);
                break;

            case BluetoothLeService.ACTION_GATT_CHARACTERISTIC_UPDATE:
                String id = intent.getStringExtra(EXTRA_ID);
                if (model.characteristics.containsKey(id)) {
                    model.characteristics.get(id).setValue(intent.getByteArrayExtra(EXTRA_DATA));
                } else {
                    model.characteristics.put(id, new MutableLiveData<byte[]>(intent.getByteArrayExtra(EXTRA_DATA)));
                }
        }
    }
}