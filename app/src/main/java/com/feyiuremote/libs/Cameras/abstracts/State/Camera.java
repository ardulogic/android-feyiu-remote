package com.feyiuremote.libs.Cameras.abstracts.State;

import com.feyiuremote.libs.Cameras.abstracts.Connection.CameraControls;
import com.feyiuremote.libs.Cameras.abstracts.Connection.ICameraControlListener;
import com.feyiuremote.libs.Utils.NamedThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public abstract class Camera {

    protected static final ThreadFactory threadFactory = new NamedThreadFactory("Camera");

    protected static final ExecutorService executor = Executors.newFixedThreadPool(1, threadFactory);

    public Camera() {

    }

    public abstract void close();

}
