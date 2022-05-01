package com.feyiuremote.libs.Feiyu.processors;

import android.graphics.Bitmap;
import android.util.Log;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.FeyiuUtils;
import com.feyiuremote.libs.LiveStream.interfaces.IPoiUpdateListener;

public class GimbalFollowProcessor implements IPoiUpdateListener {

    private BluetoothLeService mBluetoothLeService;
    private final String TAG = GimbalFollowProcessor.class.getSimpleName();

    public GimbalFollowProcessor(BluetoothLeService mBluetoothLeService) {
        this.mBluetoothLeService = mBluetoothLeService;
    }

    @Override
    public void onPoiUpdate(Bitmap bitmap, org.opencv.core.Rect poi) {
        int spd_x = calcDeviation(poi.x, poi.width, bitmap.getWidth(), false);
        int spd_y = calcDeviation(poi.y, poi.height, bitmap.getHeight(), true);

        spd_x = addDeadzone(20, spd_x);
        spd_y = addDeadzone(20, spd_y);

        mBluetoothLeService.send(FeyiuUtils.SERVICE_ID, FeyiuUtils.CONTROL_CHARACTERISTIC_ID,
                FeyiuUtils.move(spd_x, spd_y)
        );
    }

    private int calcDeviation(int point, int length, int relative_length, boolean invert_sign) {
        Float pointCenter = (float) (point + length / 2);
        Float relCenter = (float) relative_length / 2;

        int dev = (int) Math.abs(pointCenter - relCenter);

        Log.d(TAG, "PointDeviation: " + dev);

        if (pointCenter < relCenter) {
            return invert_sign ? -dev : dev;
        } else {
            return invert_sign ? dev : -dev;
        }
    }

    /**
     * Normally action begins from ~50 speed
     *
     * @param amt
     * @param speed
     * @return
     */
    private int addDeadzone(int amt, int speed) {
        return speed > 0 ? speed + amt  : speed - amt;
    }
}
