package com.feyiuremote.libs.Feiyu.controls;


import com.feyiuremote.libs.Feiyu.FeyiuState;

import java.util.Comparator;

public class JoystickState {

    public Integer panJoy;
    public Integer tiltJoy;

    public boolean isFinal = false;

    public final Long time_added_ns;

    public final Long time_exec_ns;

    private final Integer delay_ms;

    private String reason;

    public JoystickState(Integer panJoy, Integer tiltJoy, Integer delay, Boolean isFinal, String reason) {
        this(panJoy, tiltJoy, delay, reason);

        this.isFinal = isFinal;
    }

    public JoystickState(Integer panJoy, Integer tiltJoy, Integer delay, String reason) {
        this.panJoy = panJoy;
        this.tiltJoy = tiltJoy;
        this.delay_ms = delay == null ? 0 : delay;
        this.time_added_ns = System.nanoTime();
        this.time_exec_ns = time_added_ns + (long) delay_ms * 1_000_000;
        this.reason = reason;
    }

    public Long executesInNs() {
        long diff = this.time_exec_ns - System.nanoTime();

        return diff;
    }

    public Long executesInMs() {
        return executesInNs() / 1_000_000;
    }

    public void mergeWith(JoystickState upcommingState) {
        if (upcommingState.panJoy != null) {
            panJoy = upcommingState.panJoy;
        }

        if (upcommingState.tiltJoy != null) {
            tiltJoy = upcommingState.tiltJoy;
        }

        this.reason += "\n" + upcommingState.reason;
    }

    public boolean matchesCurrentState() {
        boolean matches = true;

        if (this.panJoy != null && FeyiuState.joy_val_pan != this.panJoy) {
            matches = false;
        }

        if (this.tiltJoy != null && FeyiuState.joy_val_tilt != this.tiltJoy) {
            matches = false;
        }

        return matches;
    }

    public Long timeDiffNsWith(JoystickState upcommingState) {
        return Math.abs(this.executesInNs() - upcommingState.executesInNs());
    }

    public Long timeDiffMsWith(JoystickState upcommingState) {
        return timeDiffNsWith(upcommingState) / 1_000_000;
    }

    public static class SortByTimeAsc implements Comparator<JoystickState> {
        @Override
        public int compare(JoystickState state1, JoystickState state2) {
            // Compare based on the 'time' field
            return Long.compare(state1.executesInNs(), state2.executesInNs());
        }
    }

    public String toString() {
        return "JoystickState{" +
                "panJoy=" + panJoy +
                ", tiltJoy=" + tiltJoy +
                ", delay=" + delay_ms +
                ", executesIn=" + executesInMs() + "ms" +
                ", executesIn=" + executesInNs() + "ns" +
                ", isFinal=" + isFinal +
                ", reason=" + reason +
                '}';
    }
}
