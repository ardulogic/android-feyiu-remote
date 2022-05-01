package com.feyiuremote.libs.Cameras.abstracts.State;

import com.feyiuremote.libs.Cameras.abstracts.Connection.CameraControls;
import com.feyiuremote.libs.Cameras.abstracts.Connection.ICameraControlListener;

import java.util.concurrent.ExecutorService;

public abstract class Camera {

    protected ExecutorService executor;

    public Camera(ExecutorService executor) {
        this.executor = executor;
    }

    public abstract void updateBaseInfo(ICameraControlListener listener);

    public abstract void close();

}
