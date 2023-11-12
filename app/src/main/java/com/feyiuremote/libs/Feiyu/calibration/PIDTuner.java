package com.feyiuremote.libs.Feiyu.calibration;

public class PIDTuner {

    private float targetInputValue;
    private long loopInterval;
    private float minOutput;
    private float maxOutput;
    private ZNMode znMode;
    private int cycles;
    private int i;
    private boolean output;
    private float outputValue;
    private long t1;
    private long t2;
    private long microseconds;

    private Long prev_timestamp = null;
    private long tHigh;
    private long tLow;
    private float max;
    private float min;
    private float pAverage;
    private float iAverage;
    private float dAverage;
    private float kp;
    private float ki;
    private float kd;

    // Enumeration for Ziegler-Nichols tuning mode
    public enum ZNMode {
        ZNModeBasicPID,
        ZNModeLessOvershoot,
        ZNModeNoOvershoot
    }

    public PIDTuner() {
    }

    public void setTargetInputValue(float target) {
        targetInputValue = target;
    }

    public void setOutputRange(float min, float max) {
        minOutput = min;
        maxOutput = max;
    }

    public void setZNMode(ZNMode zn) {
        znMode = zn;
    }

    public void setTuningCycles(int tuneCycles) {
        cycles = tuneCycles;
    }

    public void startTuningLoop(long us) {
        i = 0;
        output = true;
        outputValue = maxOutput;
        t1 = t2 = us;
        microseconds = tHigh = tLow = 0;
        max = -1000000000000f;
        min = 1000000000000f;
        pAverage = iAverage = dAverage = 0;
    }

    public float tunePID(float input, long us) {
        if (prev_timestamp == null) {
            prev_timestamp = us;
            return 0;
        }

        loopInterval = us - prev_timestamp;
        prev_timestamp = us;

        microseconds = us;
        max = Math.max(max, input);
        min = Math.min(min, input);

        if (output && input > targetInputValue) {
            output = false;
            outputValue = minOutput;
            t1 = us;
            tHigh = t1 - t2;
            max = targetInputValue;
        }

        if (!output && input < targetInputValue) {
            output = true;
            outputValue = maxOutput;
            t2 = us;
            tLow = t2 - t1;
            float ku = (4.0f * ((maxOutput - minOutput) / 2.0f)) / ((float) Math.PI * (max - min) / 2.0f);
            float tu = tLow + tHigh;
            float kpConstant, tiConstant, tdConstant;

            if (znMode == ZNMode.ZNModeBasicPID) {
                kpConstant = 0.6f;
                tiConstant = 0.5f;
                tdConstant = 0.125f;
            } else if (znMode == ZNMode.ZNModeLessOvershoot) {
                kpConstant = 0.33f;
                tiConstant = 0.5f;
                tdConstant = 0.33f;
            } else {
                kpConstant = 0.2f;
                tiConstant = 0.5f;
                tdConstant = 0.33f;
            }

            kp = kpConstant * ku;
            ki = (kp / (tiConstant * tu)) * getInterval();
            kd = (tdConstant * kp * tu) / getInterval();

            if (i > 1) {
                pAverage += kp;
                iAverage += ki;
                dAverage += kd;
            }

            min = targetInputValue;
            i++;
        }

        if (i >= cycles) {
            output = false;
            outputValue = minOutput;
            kp = pAverage / (i - 1);
            ki = iAverage / (i - 1);
            kd = dAverage / (i - 1);
        }

        return outputValue;
    }

    public float getKp() {
        return kp;
    }

    public float getKi() {
        return ki;
    }

    public float getKd() {
        return kd;
    }

    public boolean isFinished() {
        return (i >= cycles);
    }

    public int getCycle() {
        return i;
    }

    public long getInterval() {
        return loopInterval;
    }
}
