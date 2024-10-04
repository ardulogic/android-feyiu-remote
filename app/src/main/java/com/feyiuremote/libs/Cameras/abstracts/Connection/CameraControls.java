
package com.feyiuremote.libs.Cameras.abstracts.Connection;

import android.content.Context;

abstract public class CameraControls {

    protected static final String TAG = CameraControls.class.getSimpleName();

    protected final Context context;

    public CameraControls(Context context) {
        this.context = context;
    }

}
