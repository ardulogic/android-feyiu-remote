package com.feyiuremote.libs.Bluetooth;

import android.content.IntentFilter;

public class BluetoothIntent {

    public static IntentFilter getFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_BT_DISABLED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTING);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_SCAN_RESULTS);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CHARACTERISTIC_UPDATE);
        return intentFilter;
    }

}
