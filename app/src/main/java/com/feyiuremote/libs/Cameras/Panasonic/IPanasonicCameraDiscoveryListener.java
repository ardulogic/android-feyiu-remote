
package com.feyiuremote.libs.Cameras.Panasonic;

public interface IPanasonicCameraDiscoveryListener {

    void onDeviceFound(PanasonicCamera camera);
    void onProgressUpdate(String response);
    void onFailure(String response);

}
