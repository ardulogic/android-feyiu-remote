
package com.feyiuremote.libs.Cameras.Panasonic;

import java.util.ArrayList;

public interface IPanasonicCameraDiscoveryListener {

    void onDeviceFound(PanasonicCamera camera);
    void onProgressUpdate(String response);
    void onFailure(String response);
    void onFinish(ArrayList<String> foundCamUrls);
}
