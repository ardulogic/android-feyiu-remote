package com.feyiuremote.libs.Utils;

import android.content.Context;
import android.net.Network;
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
        this.network = Networks.getWifiNetwork(context);
    }

    public HttpClient() {
        this.network = null;
    }

    public String get(@NotNull String url, int timeoutMs) {
        Log.w(TAG, "httpGet: " + url);

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
            return null;
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }

        return replyString;
    }


}
