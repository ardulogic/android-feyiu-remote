package com.feyiuremote.ui.camera.listeners;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.processors.GimbalFollowProcessor;
import com.feyiuremote.libs.Feiyu.processors.position.GimbalWaypointsProcessor;

import org.opencv.core.Rect;

public class CameraPoiUpdateListener extends GimbalFollowProcessor {
    private final GimbalWaypointsProcessor waypointsProcessor;

    public CameraPoiUpdateListener(BluetoothLeService mBluetoothLeService, GimbalWaypointsProcessor waypointsProcessor) {
        super(mBluetoothLeService);

        this.waypointsProcessor = waypointsProcessor;
    }

    @Override
    public void onPoiLock(Rect poi) {
        this.waypointsProcessor.cancel();

        super.onPoiLock(poi);
    }
}
