package com.feyiuremote.libs.Cameras.Panasonic;

import android.content.Context;

import com.feyiuremote.libs.Cameras.abstracts.State.Camera;
import com.feyiuremote.libs.LiveStream.abstracts.LiveFeedReceiver;


public class PanasonicCamera extends Camera {

    public PanasonicCameraControls controls;
    public PanasonicCameraLiveView live;
    public PanasonicCameraState state;
    public PanasonicFocus focus;
    public IPanasonicCameraStateListener stateListener;

    /**
     * TODO: Add sound when recording is stopped
     * TODO: Add sound when camera is disconnected
     * TODO: Track recording state on screen
     *
     * @param context
     * @param ddUrl
     */
    public PanasonicCamera(Context context, String ddUrl) {
        this.updateState(new PanasonicCameraState(ddUrl));
        this.controls = new PanasonicCameraControls(context, this);
    }

    public void setFocusListener(IPanasonicCameraFocusListener listener) {
        this.focus = new PanasonicFocus(this, listener);
        this.focus.start();
    }

    public void updateState(PanasonicCameraState state) {
        this.state = state;

        if (stateListener != null) {
            stateListener.onUpdate(state);
        }
    }

    public void setStateListener(IPanasonicCameraStateListener listener) {
        this.stateListener = listener;
    }

    public boolean focusIsAvailable() {
        return this.focus != null && this.focus.isHealthy();
    }


    public boolean liveViewAlreadyExists() {
        return this.live != null;
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
