package com.feyiuremote.libs.Feiyu.calibration;

import android.content.ContentValues;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.FeyiuUtils;

public class CalibrationRunnable implements Runnable {

    private final int mJoySens;
    private final int mJoyVal;
    private final BluetoothLeService mBt;
    private final ICalibrationListener mListener;

    // Start moving only after latest characteristic received
    // for more accurate results
    private boolean mCharacteristicReady = false;

    public CalibrationRunnable(int joy_sens, int joy_val, BluetoothLeService bt, ICalibrationListener listener) {
        this.mJoySens = joy_sens;
        this.mJoyVal = joy_val;
        this.mBt = bt;
        this.mListener = listener;

        setSensitivity();
    }

    public void characteristicTick() {
        this.mCharacteristicReady = true;
    }

    private void setSensitivity() {
        mBt.send(FeyiuUtils.SERVICE_ID, FeyiuUtils.CONTROL_CHARACTERISTIC_ID,
                FeyiuUtils.setPanSensitivity(mJoySens)
        );

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mBt.send(FeyiuUtils.SERVICE_ID, FeyiuUtils.CONTROL_CHARACTERISTIC_ID,
                FeyiuUtils.setTiltSensitivity(mJoySens)
        );
    }

    @Override
    public void run() {
        mCharacteristicReady = false;

        while (!mCharacteristicReady) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Integer pan_pos_start = FeyiuState.getInstance().pos_pan.getValue();
        Integer tilt_pos_start = FeyiuState.getInstance().pos_tilt.getValue();

        long t_start = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            mBt.send(FeyiuUtils.SERVICE_ID, FeyiuUtils.CONTROL_CHARACTERISTIC_ID,
                    FeyiuUtils.move(mJoyVal, mJoyVal)
            );
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        long t_stop = System.currentTimeMillis();
        Integer pan_pos_stop_start = FeyiuState.getInstance().pos_pan.getValue();
        Integer tilt_pos_stop_start = FeyiuState.getInstance().pos_tilt.getValue();
        mBt.send(FeyiuUtils.SERVICE_ID, FeyiuUtils.CONTROL_CHARACTERISTIC_ID,
                FeyiuUtils.move(0, 0)
        );

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int pan_pos_end = FeyiuState.getInstance().pos_pan.getValue();
        int tilt_pos_end = FeyiuState.getInstance().pos_tilt.getValue();

        ContentValues cv = new ContentValues();
        cv.put("joy_sens", mJoySens);
        cv.put("joy_val", mJoyVal);

        cv.put("pan_dist", Math.abs(pan_pos_start - pan_pos_end));
        cv.put("pan_speed", (float) cv.getAsInteger("pan_dist") / (t_stop - t_start));
        cv.put("pan_overshoot", Math.abs(pan_pos_end - pan_pos_stop_start));

        cv.put("tilt_dist", Math.abs(tilt_pos_start - tilt_pos_end));
        cv.put("tilt_speed", (float) cv.getAsInteger("tilt_dist") / (t_stop - t_start));
        cv.put("tilt_overshoot", Math.abs(tilt_pos_end - tilt_pos_stop_start));

        this.mListener.onCalFinished(cv);
    }
}
