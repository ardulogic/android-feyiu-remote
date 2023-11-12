package com.feyiuremote.libs.Feiyu.controls;


import java.util.Comparator;

public class JoystickState {
    public Integer panJoy;
    public Integer tiltJoy;

    Long time;

    public JoystickState(Integer panJoy, Integer tiltJoy, Integer delay) {
        this.panJoy = panJoy;
        this.tiltJoy = tiltJoy;
        this.time = (delay == null ? 0 : delay) + System.currentTimeMillis();
    }

    public Long getTime() {
        return this.time != null ? this.time : 0;
    }

    public Long executesIn() {
        long diff = this.getTime() - System.currentTimeMillis();

        return diff <= 0 ? 0 : diff;
    }

    public void mergeWith(JoystickState upcommingState) {
        if (upcommingState.panJoy != null) {
            panJoy = upcommingState.panJoy;
        }

        if (upcommingState.tiltJoy != null) {
            tiltJoy = upcommingState.tiltJoy;
        }
    }

    public Long timeDiffWith(JoystickState upcommingState) {
        return Math.abs(this.getTime() - upcommingState.getTime());
    }

    public static class SortByTime implements Comparator<JoystickState> {
        @Override
        public int compare(JoystickState state1, JoystickState state2) {
            // Compare based on the 'time' field
            return Long.compare(state1.getTime(), state2.getTime());
        }
    }
}
