
package com.feyiuremote.libs.Cameras.abstracts.Connection;

import com.feyiuremote.libs.Cameras.abstracts.State.Camera;

public interface ICameraDiscoveryListener {

    void onFinish();
    void onDeviceFound(Camera camera);
    void onProgressUpdate(String response);
    void onFailure(String response);

}
