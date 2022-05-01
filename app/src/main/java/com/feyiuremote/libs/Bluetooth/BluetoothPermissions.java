/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.feyiuremote.libs.Bluetooth;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Manages bluetooth permissions
 */
public class BluetoothPermissions {

    private final static String TAG = BluetoothPermissions.class.getSimpleName();

    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 101;

    private static final String[] permissions = new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
    };

    private static final String[] ext_permissions = new String[]{
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
    };

    public static void request(Activity activity) {
        ActivityCompat.requestPermissions(activity, permissions, REQUEST_ID_MULTIPLE_PERMISSIONS);
    }

    public static boolean check(Context context) {
        Integer result = null;
        List<String> listPermissionsNeeded = new ArrayList<>();

        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(context.getApplicationContext(), p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
                Log.d(TAG, "Permission not granted:" + p);
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            for (String p : ext_permissions) {
                result = ContextCompat.checkSelfPermission(context.getApplicationContext(), p);
                if (result != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsNeeded.add(p);
                    Log.d(TAG, "Permission not granted:" + p);
                }
            }
        }

        return listPermissionsNeeded.isEmpty();
    }
}
