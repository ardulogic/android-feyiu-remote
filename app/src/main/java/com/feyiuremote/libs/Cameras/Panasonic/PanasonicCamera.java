package com.feyiuremote.libs.Cameras.Panasonic;

import android.content.Context;
import android.net.Network;

import com.feyiuremote.libs.Cameras.abstracts.Connection.ICameraControlListener;
import com.feyiuremote.libs.Cameras.abstracts.State.Camera;
import com.feyiuremote.libs.LiveStream.image.LiveFeedReceiver;
import com.feyiuremote.libs.Utils.SimpleHttpClient;
import com.feyiuremote.libs.Utils.XmlParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutorService;


public class PanasonicCamera extends Camera {

    private final Network network;
    public PanasonicCameraControls controls;
    public PanasonicCameraLiveView live;
    public PanasonicCameraState state;
    public PanasonicFocus focus;

    /**
     * TODO: Add sound when recording is stopped
     * TODO: Add sound when camera is disconnected
     * TODO: Track recording state on screen
     *
     * @param context
     * @param ddUrl
     */
    public PanasonicCamera(Context context, String ddUrl) {
        this.state = new PanasonicCameraState(ddUrl);
        this.network = SimpleHttpClient.getWifiNetwork(context);
        this.controls = new PanasonicCameraControls(network, this);
    }

    public void setFocusListener(IPanasonicCameraFocusListener listener) {
        this.focus = new PanasonicFocus(this, listener);
        this.focus.start();
    }

    public boolean focusIsAvailable() {
        return this.focus != null && this.focus.isHealthy();
    }

    public void createLiveView(LiveFeedReceiver feedReceiver) {
        this.live = new PanasonicCameraLiveView(this, feedReceiver);
    }

    public PanasonicCameraLiveView getLiveView() {
        return this.live;
    }

    @Override
    public void close() {
        if (this.live != null) {
            this.live.stop();
        }
    }

}
