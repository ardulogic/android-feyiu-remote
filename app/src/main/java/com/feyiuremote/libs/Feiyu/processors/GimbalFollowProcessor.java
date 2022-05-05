package com.feyiuremote.libs.Feiyu.processors;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.util.Log;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.FeyiuUtils;
import com.feyiuremote.libs.Feiyu.calibration.CalibrationPresetDbHelper;
import com.feyiuremote.libs.LiveStream.interfaces.IPoiUpdateListener;

import java.util.ArrayList;

public class GimbalFollowProcessor implements IPoiUpdateListener {

    private final CalibrationPresetDbHelper mDbPreset;
    private int ldev_y;
    private int ldev_x;
    private BluetoothLeService mBluetoothLeService;

    private ArrayList<Float[]> mTrackCurves = new ArrayList<Float[]>() {{
        add(new Float[]{(float) 0.0000193939, (float) 0.00424242, (float) 0.0181818});
        add(new Float[]{(float) 0.0000674982, (float) 0.00306131, (float) 0.00932636});
    }};

    private final String TAG = GimbalFollowProcessor.class.getSimpleName();
    private int curve;

    public GimbalFollowProcessor(BluetoothLeService mBluetoothLeService) {
        this.mDbPreset = new CalibrationPresetDbHelper(mBluetoothLeService.getApplicationContext());
        this.mBluetoothLeService = mBluetoothLeService;

        this.ldev_x = 0;
        this.ldev_y = 0;
    }

    public void setTrackingCurveParams(int index, Float[] params) {
        this.mTrackCurves.set(index, params);
    }

    public void setTrackingCurve(int index) {
        this.curve = index;
    }

    @Override
    public void onPoiUpdate(Bitmap bitmap, org.opencv.core.Rect poi) {
        track(bitmap, poi, this.curve);
    }

    private void track(Bitmap bitmap, org.opencv.core.Rect poi, int curve) {
        int dev_x = calcDeviation(poi.x, poi.width, bitmap.getWidth(), false);
        int dev_y = calcDeviation(poi.y, poi.height, bitmap.getHeight(), true);

        int ddev_x = dev_x - ldev_x;
        int ddev_y = dev_y - ldev_y;

        float spd_x = calcSpeed(dev_x, curve); //+ calcSpeed(ddev_x, curve) * (ddev_x > 0 ? 1 : -1);
        float spd_y = calcSpeed(dev_y, curve);// + calcSpeed(ddev_y, curve)* (ddev_y > 0 ? 1 : -1);

        ContentValues panSettings = mDbPreset.getByClosestPanSpeed(spd_x, dev_x > 0 ? -1 : 1);
        ContentValues tiltSettings = mDbPreset.getByClosestPanSpeed(spd_y, dev_y > 0 ? -1 : 1);

        if (panSettings == null || tiltSettings == null) {
            Log.e(TAG, "Please calibrate first!");
            return;
        }

        if (Math.abs(dev_x) < 10) {
            panSettings.put("joy_val", 0);
        } else {
            mBluetoothLeService.send(FeyiuUtils.SERVICE_ID, FeyiuUtils.CONTROL_CHARACTERISTIC_ID,
                    FeyiuUtils.setPanSensitivity(panSettings.getAsInteger("joy_sens")));
        }

        if (Math.abs(dev_y) < 10) {
            tiltSettings.put("joy_val", 0);
        } else {
            mBluetoothLeService.send(FeyiuUtils.SERVICE_ID, FeyiuUtils.CONTROL_CHARACTERISTIC_ID,
                    FeyiuUtils.setTiltSensitivity(tiltSettings.getAsInteger("joy_sens")));
        }

        Log.d(TAG, "Pan(" + dev_x + " : " + ddev_x + "): " + panSettings.getAsFloat("pan_speed")
                + " Tilt(" + dev_y + " : " + ddev_y + "):" + tiltSettings.getAsFloat("tilt_speed"));


        mBluetoothLeService.send(FeyiuUtils.SERVICE_ID, FeyiuUtils.CONTROL_CHARACTERISTIC_ID,
                FeyiuUtils.move(panSettings.getAsInteger("joy_val"), tiltSettings.getAsInteger("joy_val")));

        ldev_x = dev_x;
        ldev_y = dev_y;
    }

    private float calcSpeed(int dev, int curve_index) {
        return (float) (mTrackCurves.get(curve_index)[0] * dev * dev
                + mTrackCurves.get(curve_index)[1] * Math.abs(dev)
                + mTrackCurves.get(curve_index)[2]);
    }

    private int calcDeviation(int point, int length, int relative_length, boolean invert_sign) {
        Float pointCenter = (float) (point + length / 2);
        Float relCenter = (float) relative_length / 2;

        int dev = (int) Math.abs(pointCenter - relCenter);

        if (pointCenter < relCenter) {
            return invert_sign ? -dev : dev;
        } else {
            return invert_sign ? dev : -dev;
        }
    }

}
