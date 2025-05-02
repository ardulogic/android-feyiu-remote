package com.feyiuremote.libs.Feiyu.controls;


import android.util.Log;

import androidx.annotation.NonNull;

import com.feyiuremote.libs.Feiyu.FeyiuState;

import java.util.Comparator;

public class JoystickState {
    private final static String TAG = JoystickState.class.getSimpleName();
    public static final int AXIS_PAN = 0;
    public static final int AXIS_TILT = 1;

    public final Integer axis;
    public Integer joy_value;
    public boolean strictTimingIgnored = false;

    private Integer duration_ms;

    public final Integer delay_ms;

    private boolean executed = false;

    private boolean cancelled = false;

    public boolean hasStrictTiming = false;

    public int executions = 0;

    public String reason;
    public String comment = "";

    public final Long time_added_ns;
    public Long time_start_ns;
    public Long time_end_ns;
    private Long time_last_execution = null;

    private Long time_first_execution = null;
    private Long first_execution_timing_error = null;

    public JoystickState(Integer joyValue, Integer duration, Integer axis, Integer delay, String reason) {
        this.joy_value = joyValue;
        this.duration_ms = duration;
        this.axis = axis;

        if (delay != null) {
            hasStrictTiming = true;
            this.delay_ms = delay;
        } else {
            this.delay_ms = 0;
        }

        this.time_added_ns = System.nanoTime();
        this.time_start_ns = time_added_ns + (long) delay_ms * 1_000_000;
        this.time_end_ns = time_start_ns + (long) duration * 1_000_000;

        this.reason = reason;
    }

    public boolean hasStrictTiming() {
        return this.hasStrictTiming && !this.strictTimingIgnored;
    }

    public boolean strictTimingIgnored() {
        return this.strictTimingIgnored;
    }

    public Long executesInMs() {
        return executesInNs() / 1_000_000;
    }

    public Long executesInNs() {
        return this.time_start_ns - System.nanoTime();
    }

    public boolean hasStarted() {
        return this.time_start_ns < System.nanoTime();
    }


    public boolean executesInFuture() {
        return !isExpired() && !isCancelled() && !isExecuted();
    }

    public boolean isExecuting() {
        return hasStarted() && !isExpired() && !isCancelled();
    }


    public boolean isExpired() {
        return this.time_end_ns - System.nanoTime() < 0;
    }

    public void onExecution() {
        if (time_last_execution == null) {
            time_last_execution = System.nanoTime();

            // If same state is repeated, we dont need delay the second time
            if (hasStrictTiming) {
                ignoreStrictTiming();
            }

            // Save execution timing error:
            this.time_first_execution = System.nanoTime();
            this.first_execution_timing_error = executionTimingError();
        }

        if (time_end_ns < time_last_execution) {
            executed = true;
        }

        executions++;
    }

    private Long executionTimingError() {
        if (hasStrictTiming && time_last_execution != null) {
            return (Math.abs(time_last_execution - this.time_start_ns) - executesInNs()) / 1_000_000;
        }

        return null;
    }

    public Long getFirstExecutionTimingError() {
        return first_execution_timing_error;
    }

    public boolean isExecuted() {
        return executed;
    }

    public boolean isExecutedAtLeastOnce() {
        return time_first_execution != null;
    }

    public boolean isCancelled() {
        return cancelled;
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
        return this.time_end_ns - upcommingState.time_end_ns;
    }

    public boolean matchesStoppedState() {
        int state_value = axis == AXIS_PAN ? FeyiuState.joy_val_pan : FeyiuState.joy_val_tilt;
        boolean isTrue = joy_value == 0 && state_value == 0;

        return isTrue;
    }

    public void onCancelled() {
        cancelled = true;
    }

    public void onCancelled(String reason) {
        Log.i(TAG, "State(" + this + ") cancelled:" + reason);
        onCancelled();
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void ignoreStrictTiming() {
        this.strictTimingIgnored = true;
    }

    public static class SortByExecutionTimeAsc implements Comparator<JoystickState> {
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
        String flags = "";
        if (isExecuting()) flags += "E";
        if (isCancelled()) flags += "C";
        if (isExpired()) flags += "O";
        if (hasStrictTiming()) flags += "S";
        if (strictTimingIgnored()) flags += "SI";

        String s = "JoyState (" + flags + "):" + axisToString() + ": " + joy_value;

        if (hasStrictTiming) {
            if (isExecuted()) {
                s += " Lag: " + executionTimingError();
            } else {
                s += " in " + executesInMs() + "ms ";
            }
        }

        s += " " + comment;

        return s;
    }
}
