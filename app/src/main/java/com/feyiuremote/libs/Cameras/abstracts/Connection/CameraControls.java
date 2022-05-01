
package com.feyiuremote.libs.Cameras.abstracts.Connection;

import com.feyiuremote.libs.Cameras.Panasonic.PanasonicCamera;
import com.feyiuremote.libs.Cameras.abstracts.State.Camera;

import java.util.concurrent.ExecutorService;

abstract public class CameraControls {

    protected static final String TAG = CameraControls.class.getSimpleName();

    protected final ExecutorService executor;

    public CameraControls(ExecutorService executor) {
        this.executor = executor;
    }

}
