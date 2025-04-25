package com.feyiuremote.libs.Feiyu.controls;


import android.util.Log;

import androidx.annotation.NonNull;

import com.feyiuremote.libs.Feiyu.FeyiuState;

import java.util.Comparator;
import java.util.Objects;

public class JoystickState {

    public static final int AXIS_PAN = 0;
    public static final int AXIS_TILT = 1;
    private static final String TAG = "JoystickState";

    public Integer joy_value;

    private Integer duration_ms;
    public final Integer axis;

    public final Integer delay_ms;

    public final Long time_added_ns;

    public Long time_start_ns;
    public Long time_end_ns;

    private Long time_first_execution = null;
    private Long time_last_execution = null;


    public String reason;

    public JoystickState(Integer joyValue, Integer duration, Integer axis, Integer delay, String reason) {
        this.joy_value = joyValue;
        this.duration_ms = duration;
        this.axis = axis;
        this.delay_ms = delay == null ? 0 : delay;

        this.time_added_ns = System.nanoTime();
        this.time_start_ns = time_added_ns + (long) delay_ms * 1_000_000;
        this.time_end_ns = time_start_ns + (long) duration * 1_000_000;

        this.reason = reason;
    }

    public boolean hasDelay() {
        return this.delay_ms > 0;
    }

    public Long executesInMs() {
        return executesInNs() / 1_000_000;
    }
    public Long executesInNs() {
        return this.time_start_ns - System.nanoTime();
    }

    public boolean overlappingWith(JoystickState newState) {
        if (hasSameAxis(newState)) {
            return this.hasEnded() || (this.isExecuting() && newState.isExecuting());
        }

        return false;
    }

    public boolean hasStarted() {
        return this.time_start_ns < System.nanoTime();
    }

    public boolean hasEnded() {
        return this.time_end_ns > System.nanoTime();
    }

    public boolean isExecuting() {
        return hasStarted() && !hasEnded();
    }


    public Long executionEndsInNs() {
        return this.time_end_ns - System.nanoTime();
    }

    public Long executionEndsInMs() {
        return executionEndsInNs() / 1_000_000;
    }

    public boolean executionEnded() {
        return executionEndsInNs() < 0;
    }

    public boolean hasSameAxis(JoystickState upcommingState) {
        return Objects.equals(upcommingState.axis, this.axis);
    }

    public void onExecution() {
        if (time_first_execution == null) {
            time_first_execution = System.nanoTime();

            long timeDiff = (Math.abs(time_first_execution - this.time_start_ns) - executesInNs()) / 1_000_000;

            if (timeDiff > 40) {
                Log.w(TAG, axisToString() + " execution error:" + timeDiff + "ms (" + delay_ms + " ms delay)");
            }
        }

        time_last_execution = System.currentTimeMillis();
    }

    public boolean replaceIfSameAxis(JoystickState upcommingState) {
        if (this.hasSameAxis(upcommingState)) {
            this.joy_value = upcommingState.joy_value;

            this.duration_ms = upcommingState.duration_ms;
            this.time_start_ns = upcommingState.time_start_ns;
            this.time_end_ns = upcommingState.time_end_ns;

            this.reason += "\n" + upcommingState.reason;

            return true;
        }

        return false;
    }

    public Long executesInDiffNs(JoystickState upcommingState) {
        return this.executesInNs() - upcommingState.executesInNs();
    }

    public Long executesInDiffMs(JoystickState upcommingState) {
        return executesInDiffNs(upcommingState) / 1_000_000;
    }

    public Long executionEndsDiffMs(JoystickState upcommingState) {
        return executionEndsDiffNs(upcommingState) / 1_000_000;
    }

    public Long executionEndsDiffNs(JoystickState upcommingState) {
        return this.executionEndsInNs() - upcommingState.executionEndsInNs();
    }

    public boolean matchesStoppedState() {
        int state_value = axis == AXIS_PAN ? FeyiuState.joy_val_pan : FeyiuState.joy_val_tilt;

        return joy_value == 0 && state_value == 0;
    }


    public static class SortByTimeAsc implements Comparator<JoystickState> {
        @Override
        public int compare(JoystickState state1, JoystickState state2) {
            // Compare based on the 'time' field
            return Long.compare(state1.executesInNs(), state2.executesInNs());
        }
    }

    public String axisToString() {
        if (axis == AXIS_PAN) {
            return "Pan";
        } else if (axis == AXIS_TILT) {
            return "Tilt";
        }

        return "Unknown";
    }

    @NonNull
    public String toString() {
        return "JoystickState{" +
                axisToString() + "=" + joy_value +
                ", durationMs=" + duration_ms +
                ", delayMs=" + delay_ms +
                ", executesInMs=" + executesInMs() + "ms" +
                ", reason=" + reason +
                '}';
    }
}
