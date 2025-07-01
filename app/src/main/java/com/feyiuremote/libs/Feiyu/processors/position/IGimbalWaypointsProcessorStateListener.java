package com.feyiuremote.libs.Feiyu.processors.position;

public interface IGimbalWaypointsProcessorStateListener {
    void onStateChange(String mode, boolean isActive);

    void onPoseTrackingToggle(boolean isActive);

}
