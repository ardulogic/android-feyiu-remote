package com.feyiuremote.libs.Cameras.abstracts.Connection;

public interface ISSDPDiscoveryListener {

    void onFinish();
    void onDeviceFound(String ssdpReplyMessage);
    void onProgressUpdate(String response);
    void onFailure(String response);

}
