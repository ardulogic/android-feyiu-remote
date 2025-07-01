package com.feyiuremote.libs.Bluetooth;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class BluetoothSetupDialog {

    private final Context context;
    private final BluetoothPreferencesManager prefsManager;
    private final BluetoothViewModel bluetoothViewModel;

    public BluetoothSetupDialog(Context context, BluetoothViewModel bluetoothViewModel) {
        this.context = context;
        this.bluetoothViewModel = bluetoothViewModel;
        this.prefsManager = new BluetoothPreferencesManager(context);
    }

    public void showSetupOptionsDialog() {
        String[] options = {"View Saved Gimbals", "Add From Scan Results"};
        new AlertDialog.Builder(context)
                .setTitle("Bluetooth Setup")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showSavedDevicesDialog();
                    } else {
                        showScanResultsDialog();
                    }
                })
                .show();
    }

    private void showSavedDevicesDialog() {
        List<BluetoothDevice> devices = prefsManager.getDevices();
        if (devices.isEmpty()) {
            Toast.makeText(context, "No saved gimbals.", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1);
        for (BluetoothDevice d : devices) {
            String label = d.name + " (" + d.mac + ")";
            if (d.mac.equals(prefsManager.getActiveMac())) {
                label += " [Active]";
            }
            adapter.add(label);
        }

        new AlertDialog.Builder(context)
                .setTitle("Saved Gimbals")
                .setAdapter(adapter, (dialog, which) -> {
                    showSavedDeviceActionsDialog(devices.get(which));
                })
                .show();
    }

    private void showSavedDeviceActionsDialog(BluetoothDevice device) {
        String[] actions = {"Set as Active", "Delete"};
        new AlertDialog.Builder(context)
                .setTitle(device.name + " (" + device.mac + ")")
                .setItems(actions, (dialog, which) -> {
                    List<BluetoothDevice> devices = prefsManager.getDevices();

                    if (which == 0) { // Set active
                        prefsManager.setActiveMac(device.mac);
                        bluetoothViewModel.ssid.postValue(device.name);
                        Toast.makeText(context, "Active gimbal set to: " + device.name, Toast.LENGTH_SHORT).show();
                    } else { // Delete
                        devices.removeIf(d -> d.mac.equals(device.mac));
                        prefsManager.saveDevices(devices);
                        if (device.mac.equals(prefsManager.getActiveMac())) {
                            prefsManager.setActiveMac(null);
                        }
                        Toast.makeText(context, "Deleted: " + device.name, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void showScanResultsDialog() {
        List<BluetoothDevice> scanned = new ArrayList<>();
        bluetoothViewModel.scanResults.getValue().forEach(scanResult -> {
            String name = scanResult.getDevice().getName();
            if (name != null && !name.isEmpty()) {
                scanned.add(new BluetoothDevice(name, scanResult.getDevice().getAddress()));
            }
        });

        if (scanned.isEmpty()) {
            Toast.makeText(context, "No scan results.", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1);
        for (BluetoothDevice device : scanned) {
            adapter.add(device.name + " (" + device.mac + ")");
        }

        new AlertDialog.Builder(context)
                .setTitle("Scan Results")
                .setAdapter(adapter, (dialog, which) -> {
                    BluetoothDevice selected = scanned.get(which);
                    addDeviceDialog(selected);
                })
                .show();
    }

    private void addDeviceDialog(BluetoothDevice device) {
        List<BluetoothDevice> devices = prefsManager.getDevices();
        boolean alreadySaved = devices.stream().anyMatch(d -> d.mac.equals(device.mac));

        if (alreadySaved) {
            Toast.makeText(context, "Device already saved.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(context)
                .setTitle("Add Gimbal")
                .setMessage("Add " + device.name + " to saved gimbals?")
                .setPositiveButton("Add", (dialog, which) -> {
                    devices.add(device);
                    prefsManager.saveDevices(devices);
                    Toast.makeText(context, "Saved: " + device.name, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
