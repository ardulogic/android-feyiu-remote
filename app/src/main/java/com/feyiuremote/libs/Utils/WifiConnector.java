package com.feyiuremote.libs.Utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class WifiConnector {
    private static final String TAG = "WifiConnector";
    private static final String PREFS_NAME = "wifi_presets";
    private static final String KEY_PRESETS = "presets";

    private final Context context;
    private final WifiManager wifiManager;
    private final ConnectivityManager connectivityManager;

    public List<AccessPoint> APs;

    public WifiConnector(Context context) {
        this.context = context.getApplicationContext();
        this.wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
        this.connectivityManager = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        loadPresets();
    }

    /**
     * Simple data holder
     */
    public static class AccessPoint {
        public String ssid;
        public String password;

        public AccessPoint(String ssid, String password) {
            this.ssid = ssid;
            this.password = password;
        }
    }

    /* ─────────────────────────────────────── PERSISTENCE ─────────────────────────────────────── */

    public void savePresets() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = new Gson().toJson(APs);
        prefs.edit().putString(KEY_PRESETS, json).apply();
    }

    public void loadPresets() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_PRESETS, null);
        if (json == null) APs = new ArrayList<>();
        else {
            Type type = new TypeToken<List<AccessPoint>>() {
            }.getType();
            APs = new Gson().fromJson(json, type);
        }
    }

    /* ─────────────────────────────────────── UI HELPERS ─────────────────────────────────────── */

    /**
     * Pops up a dialog where the user can <b>add</b>, <b>edit</b> or <b>delete</b> Wi‑Fi presets.
     * The list is stored instantly after every change.
     */
    public void editAPs(Activity activity) {
        if (APs == null) loadPresets();

        // Build the list (existing SSIDs + "Add new…")
        String[] items = new String[APs.size() + 1];
        for (int i = 0; i < APs.size(); i++) items[i] = APs.get(i).ssid;
        items[APs.size()] = "Add new network…";

        new AlertDialog.Builder(activity)
                .setTitle("Manage Wi‑Fi presets")
                .setItems(items, (dialog, which) -> {
                    if (which == APs.size()) {
                        // Add
                        showAddEditDialog(null, -1, activity);
                    } else {
                        // Edit / Delete existing entry
                        showAPOptionsDialog(which, activity);
                    }
                })
                .show();
    }

    /**
     * Shows a dialog with the actions that can be performed on the chosen AP (edit / delete).
     */
    private void showAPOptionsDialog(int index, Activity activity) {
        String[] choices = {"Edit", "Delete", "Cancel"};
        new AlertDialog.Builder(activity)
                .setTitle(APs.get(index).ssid)
                .setItems(choices, (d, which) -> {
                    switch (which) {
                        case 0: // Edit
                            showAddEditDialog(APs.get(index), index, activity);
                            break;
                        case 1: // Delete
                            APs.remove(index);
                            savePresets();
                            Toast.makeText(activity, "Deleted", Toast.LENGTH_SHORT).show();
                            break;
                        default:
                            d.dismiss();
                    }
                })
                .show();
    }

    /**
     * Shows a form (SSID + password) either to <b>add</b> a new preset (index < 0) or to
     * <b>edit</b> an existing one (index >= 0).
     */
    private void showAddEditDialog(@Nullable AccessPoint apToEdit, int index, Activity activity) {
        // Simple vertical layout with two EditTexts
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * activity.getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, 0);

        final EditText ssidInput = new EditText(activity);
        ssidInput.setHint("SSID");
        ssidInput.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(ssidInput);

        final EditText passInput = new EditText(activity);
        passInput.setHint("Password (leave blank for open)");
        passInput.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(passInput);

        if (apToEdit != null) {
            ssidInput.setText(apToEdit.ssid);
            passInput.setText(apToEdit.password);
        }

        new AlertDialog.Builder(activity)
                .setTitle(apToEdit == null ? "Add network" : "Edit network")
                .setView(layout)
                .setPositiveButton("Save", (d, w) -> {
                    String newSsid = ssidInput.getText().toString().trim();
                    String newPass = passInput.getText().toString();

                    if (newSsid.isEmpty()) {
                        Toast.makeText(activity, "SSID cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (index >= 0) {
                        // Update existing
                        APs.get(index).ssid = newSsid;
                        APs.get(index).password = newPass;
                    } else {
                        // Add new
                        APs.add(new AccessPoint(newSsid, newPass));
                    }
                    savePresets();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /* ─────────────────────────────────────── CONNECTING ─────────────────────────────────────── */

    @SuppressLint("MissingPermission")
    public void askToChooseAP(Activity activity) {
        if (APs == null) loadPresets();

        selectAndConnectTo(APs, activity);
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE})
    public void selectAndConnectTo(List<AccessPoint> aps, Activity context) {
        if (aps == null || aps.isEmpty()) return;

        String[] ssidList = new String[aps.size()];
        for (int i = 0; i < aps.size(); i++) {
            ssidList[i] = aps.get(i).ssid;
        }

        new AlertDialog.Builder(context)
                .setTitle("Select Wi‑Fi network")
                .setItems(ssidList, (dialogInterface, which) -> connectTo(aps.get(which)))
                .show();
    }

    public boolean isCurrentlyConnectedToWifi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(activeNetwork);
            return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            return wifiManager.isWifiEnabled() && wifiManager.getConnectionInfo() != null && wifiManager.getConnectionInfo().getNetworkId() != -1;
        }
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE})
    private void connectTo(AccessPoint ap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                    .setSsid(ap.ssid)
                    .setWpa2Passphrase(ap.password)
                    .build();

            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .setNetworkSpecifier(specifier)
                    .build();

            ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    super.onAvailable(network);
                    Log.d(TAG, "Connected to network: " + ap.ssid);
                    connectivityManager.bindProcessToNetwork(network);
                }

                @Override
                public void onUnavailable() {
                    super.onUnavailable();
                    Log.d(TAG, "Failed to connect to: " + ap.ssid);
                }
            };

            connectivityManager.requestNetwork(request, callback);

        } else {
            if (!wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(true);
            }

            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = String.format("\"%s\"", ap.ssid);

            if (ap.password == null || ap.password.isEmpty()) {
                wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            } else {
                wifiConfig.preSharedKey = String.format("\"%s\"", ap.password);
            }

            int netId = wifiManager.addNetwork(wifiConfig);
            if (netId == -1) {
                for (WifiConfiguration config : wifiManager.getConfiguredNetworks()) {
                    if (config.SSID != null && config.SSID.equals(String.format("\"%s\"", ap.ssid))) {
                        netId = config.networkId;
                        break;
                    }
                }
            }

            if (netId != -1) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(netId, true);
                wifiManager.reconnect();
            } else {
                Log.e(TAG, "Failed to add or find network config for: " + ap.ssid);
            }
        }
    }
}
