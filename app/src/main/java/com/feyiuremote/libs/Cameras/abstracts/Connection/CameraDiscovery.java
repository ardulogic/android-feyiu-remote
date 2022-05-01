package com.feyiuremote.libs.Cameras.abstracts.Connection;

import android.content.Context;

import com.feyiuremote.libs.Cameras.abstracts.State.Camera;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;


abstract public class CameraDiscovery {

    private static final String TAG = CameraDiscovery.class.getSimpleName();
    protected final ExecutorService executor;
    protected ArrayList<Camera> foundCameras = new ArrayList<>();

    public CameraDiscovery(ExecutorService executor) {
        this.executor = executor;
    }

    public ArrayList<Camera> getAll() {
        return foundCameras;
    }
}
