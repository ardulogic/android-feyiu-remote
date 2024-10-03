package com.feyiuremote.libs.Bluetooth;


import static com.feyiuremote.libs.Bluetooth.BluetoothLeService.ACTION_GATT_DISCONNECTED;
import static com.feyiuremote.libs.Bluetooth.BluetoothLeService.EXTRA_DATA;
import static com.feyiuremote.libs.Bluetooth.BluetoothLeService.EXTRA_ID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;

public class BluetoothLeUpdateReceiver extends BroadcastReceiver {

    private static BluetoothViewModel model;
    private static IOnBluetoothServicesDiscovered servicesDiscoveredListener;
    private final static String TAG = BluetoothLeUpdateReceiver.class.getSimpleName();

    public BluetoothLeUpdateReceiver(BluetoothViewModel model) {
        super();

        BluetoothLeUpdateReceiver.model = model;
    }

    public void setOnConnectedListener(IOnBluetoothServicesDiscovered l) {
        servicesDiscoveredListener = l;
    }

    public BluetoothLeUpdateReceiver() {
        super();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (model == null) {
            return;
        }

        String action = intent.getAction();

        switch (action) {
            case BluetoothLeService.ACTION_BT_DISABLED:
                Log.e(TAG, "Bluetooth disabled!");
                model.statusMessage.setValue("Bluetooth is disabled!");
                model.connected.setValue(false);
                model.services_discovered.setValue(false);
                model.connectionStatus.setValue(action);
                break;

            case BluetoothLeService.ACTION_GATT_CONNECTED:
                Log.d(TAG, "Connected!");
                model.statusMessage.setValue("Connected.");
                model.connected.setValue(true);
                model.services_discovered.setValue(false);
                model.connectionStatus.setValue(action);
                break;

            case BluetoothLeService.ACTION_GATT_CONNECTING:
                Log.d(TAG, "Connecting...");
                model.statusMessage.setValue("Connecting...");
                model.connected.setValue(false);
                model.services_discovered.setValue(false);
                model.connectionStatus.setValue(action);
                break;

            case BluetoothLeService.ACTION_GATT_DISCONNECTING:
                Log.d(TAG, "Disconnecting...");
                model.statusMessage.setValue("Disconnecting...");
                model.connectionStatus.setValue(action);
                break;

            case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                Log.d(TAG, "Disconnected!");
                model.statusMessage.setValue("Disconnected.");
                model.connected.setValue(false);
                model.services_discovered.setValue(false);
                model.connectionStatus.setValue(action);
                break;

            case BluetoothLeService.ACTION_GATT_CONNECTION_FAILED:
                Log.d(TAG, "Connection failed!");
                model.statusMessage.setValue("Connection failed!");
                model.connected.setValue(false);
                model.services_discovered.setValue(false);
                model.connectionStatus.setValue(ACTION_GATT_DISCONNECTED);
                break;

            case BluetoothLeService.ACTION_SCAN_RESULTS:
                Log.d(TAG, "Scan results updated");
                ArrayList<ScanResult> scanResults = intent.getParcelableArrayListExtra(EXTRA_DATA);
                Log.d(TAG, scanResults.toString());
                model.scanResults.postValue(scanResults);
                break;

            case BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED:
                Log.d(TAG, "Services discovered");
                model.statusMessage.postValue("Services have been discovered");
                model.services_discovered.setValue(true);

                if (servicesDiscoveredListener != null) {
                    servicesDiscoveredListener.onServicesDiscovered();
                }
                break;

            case BluetoothLeService.ACTION_GATT_CHARACTERISTIC_UPDATE:
                String id = intent.getStringExtra(EXTRA_ID);
                if (model.characteristics.containsKey(id)) {
                    model.characteristics.get(id).setValue(intent.getByteArrayExtra(EXTRA_DATA));
                } else {
                    model.characteristics.put(id, new MutableLiveData<byte[]>(intent.getByteArrayExtra(EXTRA_DATA)));
                }
        }

        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

            switch (state) {
                case BluetoothAdapter.STATE_TURNING_ON:
                    // Bluetooth is turning on
                    model.statusMessage.setValue("Bluetooth is turning on...");
                    model.connected.postValue(false);
                    model.enabled.postValue(false);
                    model.connectionStatus.setValue(BluetoothLeService.ACTION_BT_TURNING_ON);
                    break;
                case BluetoothAdapter.STATE_ON:
                    model.connected.postValue(false);
                    model.statusMessage.setValue("Bluetooth is on.");
                    model.enabled.postValue(true);
                    model.connectionStatus.setValue(BluetoothLeService.ACTION_BT_ENABLED);
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    model.statusMessage.setValue("Bluetooth is turning off...");
                    model.connected.postValue(false);
                    model.enabled.postValue(false);
                    model.connectionStatus.setValue(BluetoothLeService.ACTION_BT_TURNING_OFF);
                    break;
                case BluetoothAdapter.STATE_OFF:
                    model.statusMessage.setValue("Bluetooth is off.");
                    model.connected.postValue(false);
                    model.enabled.postValue(false);
                    model.connectionStatus.setValue(BluetoothLeService.ACTION_BT_DISABLED);
                    break;
            }
        }
    }
}