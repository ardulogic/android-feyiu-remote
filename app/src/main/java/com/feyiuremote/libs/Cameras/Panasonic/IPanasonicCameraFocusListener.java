
package com.feyiuremote.libs.Cameras.Panasonic;

public interface IPanasonicCameraFocusListener {

    void onUpdate(double position);

    void onFailure();

    void onTargetReached(double position);
}
