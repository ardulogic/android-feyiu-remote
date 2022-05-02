
package com.feyiuremote.libs.Feiyu;

import java.math.BigInteger;

import androidx.lifecycle.MutableLiveData;

public class FeyiuState {

    private final static String TAG = FeyiuState.class.getSimpleName();

    public MutableLiveData<Integer> pos_tilt = new MutableLiveData<>();
    public MutableLiveData<Integer> pos_pan = new MutableLiveData<>();
    public MutableLiveData<Integer> pos_yaw = new MutableLiveData<>();

    private static final FeyiuState mInstance = new FeyiuState();

    public static FeyiuState getInstance() {
        return mInstance;
    }

    public void update(byte[] data) {
        if (data.length > 15) {
            pos_tilt.setValue((new BigInteger(new byte[]{data[11], data[10]})).intValue());
            pos_pan.setValue((new BigInteger(new byte[]{data[15], data[14]})).intValue());
            pos_yaw.setValue((new BigInteger(new byte[]{data[13], data[12]})).intValue());
        }
    }

}
