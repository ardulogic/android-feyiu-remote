package com.feyiuremote.libs.Feiyu.controls;

import android.util.Log;

import com.feyiuremote.libs.Feiyu.FeyiuState;

import java.util.Comparator;
import java.util.Objects;

public class SensitivityState {
    private final static String TAG = SensitivityState.class.getSimpleName();

    public static final int TYPE_PAN = 0;
    public static final int TYPE_TILT = 1;

    public Integer sens;

    public Integer type;

    Long time;

    public SensitivityState(Integer sens, Integer type, Integer delay) {
        this.sens = sens;
        this.type = type;
        this.time = (delay == null ? 0 : delay) + System.currentTimeMillis();
    }

    public Long getTime() {
        return this.time != null ? this.time : 0;
    }

    public Long executesIn() {
        long diff = this.getTime() - System.currentTimeMillis();

        return diff <= 0 ? 0 : diff;
    }

    public boolean isSameTypeAs(SensitivityState upcommingState) {
        return Objects.equals(upcommingState.type, this.type);
    }

    public boolean differsFromCurrent() {
        if (this.type == TYPE_PAN) {
            return this.sens != FeyiuState.joy_sens_pan;
        }

        if (this.type == TYPE_TILT) {
            return this.sens != FeyiuState.joy_sens_tilt;
        }

        return false;
    }

    public void mergeWith(SensitivityState upcommingState) {
        if (Objects.equals(upcommingState.type, this.type)) {
            if (upcommingState.sens != null) {
                sens = upcommingState.sens;
            }
        } else {
            Log.e(TAG, "Cannot merge sensitivity states of different type");
        }
    }

    public Long timeDiffWith(SensitivityState upcommingState) {
        return Math.abs(this.getTime() - upcommingState.getTime());
    }

    public static class SortByTypeAndTime implements Comparator<SensitivityState> {
        @Override
        public int compare(SensitivityState state1, SensitivityState state2) {
            // Compare based on 'sensitivityType' first
            int typeComparison = state1.type.compareTo(state2.type);

            // If the 'sensitivityType' values are the same, compare based on 'time'
            if (typeComparison == 0) {
                return Long.compare(state1.executesIn(), state2.executesIn());
            }

            return typeComparison;
        }
    }

    public static class SortByTime implements Comparator<SensitivityState> {
        @Override
        public int compare(SensitivityState state1, SensitivityState state2) {
            // Compare based on the 'time' field
            return Long.compare(state1.executesIn(), state2.executesIn());
        }
    }
}