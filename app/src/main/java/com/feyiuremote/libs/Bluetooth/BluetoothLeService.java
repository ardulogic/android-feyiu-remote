/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.feyiuremote.libs.Bluetooth;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();
    private static final long SCAN_PERIOD = 60000;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    public final static String ACTION_BT_DISABLED =
            "bluetooth.le.ACTION_BT_DISABLED";

    public final static String ACTION_BT_ENABLED =
            "bluetooth.le.ACTION_BT_ENABLED";

    public final static String ACTION_BT_TURNING_ON =
            "bluetooth.le.ACTION_BT_TURNING_ON";

    public final static String ACTION_BT_TURNING_OFF =
            "bluetooth.le.ACTION_BT_TURNING_OFF";

    public final static String ACTION_BT_WRONG_ADDRESS =
            "bluetooth.le.ACTION_BT_WRONG_ADDRESS";
    public final static String ACTION_SCAN_RESULTS =
            "bluetooth.le.ACTION_SCAN_RESULTS";
    public final static String ACTION_GATT_CONNECTED =
            "bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_CONNECTING =
            "bluetooth.le.ACTION_GATT_CONNECTING";

    public final static String ACTION_GATT_DISCONNECTING =
            "bluetooth.le.ACTION_GATT_DISCONNECTING";
    public static final String ACTION_GATT_CONNECTION_FAILED =
            "bluetooth.le.ACTION_GATT_CONNECTION_FAILED";
    public final static String ACTION_GATT_DISCONNECTED =
            "bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_GATT_CHARACTERISTIC_UPDATE =
            "bluetooth.le.ACTION_CHARACTERISTIC_UPDATE";
    public final static String EXTRA_ID =
            "bluetooth.le.EXTRA_ID";
    public final static String EXTRA_DATA =
            "bluetooth.le.EXTRA_DATA";

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    BluetoothLeScanner bluetoothLeScanner;

    private Handler handler = new Handler();

    private boolean scanning = false;

    private ArrayList<ScanResult> mScanResults = new ArrayList<>();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    public final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        return true;
    }

    @SuppressLint("MissingPermission")
    private void stopScanAfter(long delay) {
        handler.postDelayed(() -> {
            Log.d(TAG, "Stopping scan...");
            scanning = false;
            withBluetoothEnabled(() -> bluetoothLeScanner.stopScan(bleScanCallback));
        }, delay);
    }

    @SuppressLint("MissingPermission")
    public void scan() {
        withBluetoothEnabled(() -> {
            try {
                if (!scanning) {
                    Log.d(TAG, "Starting scan...");
                    scanning = true;
                    bluetoothLeScanner.startScan(bleScanCallback);
                    stopScanAfter(SCAN_PERIOD);
                } else {
                    Log.d(TAG, "Stopping scan...");
                    scanning = false;
                    bluetoothLeScanner.stopScan(bleScanCallback);
                }
            } catch (NullPointerException e) {
                Log.e(TAG, "Scanner is not available yet");
            }
        });

    }


    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    @SuppressLint("MissingPermission")
    public void connect(final String address) {
        withBluetoothEnabled(() -> {
            if (address == null) {
                broadcastGattUpdate(ACTION_BT_WRONG_ADDRESS);
                Log.e(TAG, "BluetoothAdapter not initialized or unspecified address.");
                return;
            }

            if (previousConnectionExists(address)) {
                Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");

                if (mBluetoothGatt.connect()) {
                    broadcastGattUpdate(ACTION_GATT_CONNECTING);
                    return;
                }
            }

            final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            if (device != null) {
                mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
                mBluetoothDeviceAddress = address;
                broadcastGattUpdate(ACTION_GATT_CONNECTING);
            } else {
                Log.e(TAG, "Could not connect to device, it no longer exists!");
                broadcastGattUpdate(ACTION_GATT_CONNECTION_FAILED);
            }
        });
    }

    /**
     * Sends message to a specific characteristic in specific service
     *
     * @param serviceId
     * @param characteristicId
     * @param message
     */
    @SuppressLint("MissingPermission")
    public void send(String serviceId, String characteristicId, String message) {
        this.whenDeviceIsAvailable(() -> {
            BluetoothGattService gattService = mBluetoothGatt.getService(UUID.fromString(serviceId));

            if (gattService != null) {
                BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(UUID.fromString(characteristicId));

                if (gattCharacteristic != null) {
                    Log.e(TAG, "Sending:" + message);
                    byte[] b = hexStringToByteArray(message);
                    gattCharacteristic.setValue(b); // call this BEFORE(!) you 'write' any stuff to the server

                    mBluetoothGatt.writeCharacteristic(gattCharacteristic);
                } else {
                    Log.e(TAG, "Characteristic not found:" + characteristicId);
                }
            } else {
                Log.e(TAG, "Service not found:" + serviceId);
            }
        });
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    @SuppressLint("MissingPermission")
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        mBluetoothGatt.disconnect();
        mBluetoothGatt.close();
        mBluetoothGatt = null;

        broadcastGattUpdate(ACTION_GATT_DISCONNECTED);
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    @SuppressLint("MissingPermission")
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public BluetoothGattService getService(String uuid) {
        return mBluetoothGatt.getService(UUID.fromString(uuid));
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    @SuppressLint("MissingPermission")
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        whenDeviceIsAvailable(() -> mBluetoothGatt.readCharacteristic(characteristic));
    }

    /**
     * Enables or disables notification on a given characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    @SuppressLint("MissingPermission")
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        whenDeviceIsAvailable(() -> {
            mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(BluetoothGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        });
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    /**
     * Converts String to Byte Array
     *
     * @param s
     * @return
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    broadcastGattUpdate(ACTION_GATT_CONNECTED);
                    // Attempts to discover services after successful connection.
                    Log.i(TAG, "Connected to GATT server.");
                    Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
                    break;

                case BluetoothProfile.STATE_CONNECTING:
                    Log.i(TAG, "Connecting...");
                    broadcastGattUpdate(ACTION_GATT_CONNECTING);
                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.i(TAG, "Disconnected from GATT server.");
                    broadcastGattUpdate(ACTION_GATT_DISCONNECTED);
                    break;

                case BluetoothProfile.STATE_DISCONNECTING:
                    Log.i(TAG, "Disconnecting...");
                    broadcastGattUpdate(ACTION_GATT_DISCONNECTING);
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastGattUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastGattUpdate(ACTION_GATT_CHARACTERISTIC_UPDATE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastGattUpdate(ACTION_GATT_CHARACTERISTIC_UPDATE, characteristic);
        }
    };

    // Device scan callback.
    private ScanCallback bleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            boolean newScanResult = true;

            if (!mScanResults.isEmpty()) {
                for (ScanResult existingResult : mScanResults) {
                    if (Objects.equals(existingResult.getDevice().getAddress(), result.getDevice().getAddress())) {
                        newScanResult = false;
                    }
                }
            }

            if (newScanResult) {
                mScanResults.add(result);
            }

            if (newScanResult) {
                Intent intent = new Intent();
                intent.setAction(ACTION_SCAN_RESULTS);
                intent.putParcelableArrayListExtra(EXTRA_DATA, (ArrayList<? extends Parcelable>) mScanResults);

                sendBroadcast(intent);
            }
        }
    };

    public boolean previousConnectionExists(String address) {
        return mBluetoothGatt != null && address.equals(mBluetoothDeviceAddress);
    }

    private void withBluetoothEnabled(Runnable r) {
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            r.run();
        } else {
            broadcastGattUpdate(ACTION_BT_DISABLED);
        }
    }

    private void whenDeviceIsAvailable(Runnable r) {
        withBluetoothEnabled(() -> {
            if (mBluetoothGatt != null) {
                BluetoothDevice device = mBluetoothGatt.getDevice();
                if (device != null) {
                    r.run();
                }
            }
        });
    }

    private void broadcastGattUpdate(final String state) {
        final Intent intent = new Intent(state);
        sendBroadcast(intent);
    }

    private void broadcastGattUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        String id = characteristic.getUuid().toString();
        intent.putExtra(EXTRA_ID, id);

        // For all other profiles, writes the data formatted in HEX.
        final byte[] data = characteristic.getValue();
        intent.putExtra(EXTRA_DATA, data);

        sendBroadcast(intent);
    }
}
