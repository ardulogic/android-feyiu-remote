
package com.feyiuremote.libs.Cameras.abstracts.Connection;

import android.content.Context;

import com.feyiuremote.libs.Utils.NamedThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

abstract public class CameraControls {

    protected static final String TAG = CameraControls.class.getSimpleName();

    protected static final ThreadFactory threadFactory = new NamedThreadFactory("CameraControls");

    public static final ExecutorService executor = Executors.newFixedThreadPool(1, threadFactory);
    protected final Context context;

    public CameraControls(Context context) {
        this.context = context;
    }

}
