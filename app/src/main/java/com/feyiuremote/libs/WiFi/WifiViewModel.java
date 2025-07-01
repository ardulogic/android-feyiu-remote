package com.feyiuremote.libs.WiFi;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.feyiuremote.libs.Utils.WifiConnector;

import java.util.ArrayList;
import java.util.List;

public class WifiViewModel extends AndroidViewModel {
    private final WifiConnector wifiConnector;
    public MutableLiveData<String> connectionStatus = new MutableLiveData<String>();
    public MutableLiveData<String> ssid = new MutableLiveData<>();

    public WifiViewModel(@NonNull Application application) {
        super(application);

        wifiConnector = new WifiConnector(application.getApplicationContext(), this);
    }

    public WifiConnector getWifiConnector() {
        return wifiConnector;
    }

    public void saveDefaultAPs() {
        if (wifiConnector.APs == null || wifiConnector.APs.isEmpty()) {
            List<WifiConnector.AccessPoint> aps = new ArrayList<>();
            aps.add(new WifiConnector.AccessPoint("GH4-7EC534", "afcdbb37ec534"));
            aps.add(new WifiConnector.AccessPoint("G7", ""));

            wifiConnector.APs = aps;
            wifiConnector.savePresets();
        }
    }


}
