package com.feyiuremote.libs.Utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.util.Log;

public class Networks {
    private static final String TAG = "Networks";

    public static Network getWifiNetwork(Context context) {

        // Get the ConnectivityManager
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // Find the available networks
        if (connectivityManager != null) {
            Network[] networks = connectivityManager.getAllNetworks();

            // Iterate through the networks
            for (Network network : networks) {
                NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);

                // Check if it's a WiFi network
                if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    Log.d(TAG, "Found WiFi network: " + network);

                    // Open a connection on this network
                    return network;
                }
            }
        }

        return null;
    }
}
