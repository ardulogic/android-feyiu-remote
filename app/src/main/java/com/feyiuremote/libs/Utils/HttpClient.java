package com.feyiuremote.libs.Utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import kotlin.jvm.internal.Intrinsics;

public class HttpClient {

    private static final String TAG = HttpClient.class.getSimpleName();

    private final Network network;

    public HttpClient(Network network) {
        this.network = network;
    }

    public HttpClient(Context context) {
        this.network = getWifiNetwork(context);
    }

    public HttpClient() {
        this.network = null;
    }

    private static Network getWifiNetwork(Context context) {

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

    public String get(@NotNull String url, int timeoutMs) {
        Intrinsics.checkNotNullParameter(url, "url");

        String replyString = "";
        int timeout = (timeoutMs < 0) ? 10000 : timeoutMs;

        HttpURLConnection httpConn = null;

        try {
            URL urlObj = new URL(url);

            if (network != null) {
                httpConn = (HttpURLConnection) network.openConnection(urlObj);
            } else {
                httpConn = (HttpURLConnection) urlObj.openConnection();
            }

            httpConn.setRequestMethod("GET");
            httpConn.setConnectTimeout(timeout);
            httpConn.setReadTimeout(timeout);

            int responseCode = httpConn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "httpGet: Response Code Error: " + responseCode + ": " + url);
                return "";
            }

            try (InputStream inputStream = httpConn.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                StringBuilder responseBuf = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    responseBuf.append(line);
                }

                replyString = responseBuf.toString();
            }

        } catch (Exception e) {
            Log.w(TAG, "httpGet: " + url + "  " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }

        return replyString;
    }


}
