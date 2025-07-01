package com.feyiuremote.libs.Bluetooth;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class BluetoothPreferencesManager {
    private static final String PREF_NAME = "gimbal_prefs";
    private static final String KEY_DEVICES = "gimbal_devices";
    private static final String KEY_ACTIVE = "active_mac";

    private SharedPreferences prefs;

    public BluetoothPreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveDevices(List<BluetoothDevice> devices) {
        Gson gson = new Gson();
        String json = gson.toJson(devices);
        prefs.edit().putString(KEY_DEVICES, json).apply();
    }

    public List<BluetoothDevice> getDevices() {
        String json = prefs.getString(KEY_DEVICES, "");
        if (json.isEmpty()) return new ArrayList<>();
        Type listType = new TypeToken<List<BluetoothDevice>>() {
        }.getType();
        return new Gson().fromJson(json, listType);
    }

    public void setActiveMac(String mac) {
        prefs.edit().putString(KEY_ACTIVE, mac).apply();
    }

    public String getActiveMac() {
        return prefs.getString(KEY_ACTIVE, null);
    }

    public String getActiveSsid() {
        String activeMac = getActiveMac();
        if (activeMac == null) return null;

        for (BluetoothDevice device : getDevices()) {
            if (activeMac.equals(device.mac)) {
                return device.name;
            }
        }

        return null;
    }
}