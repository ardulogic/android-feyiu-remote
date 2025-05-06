package com.feyiuremote.ui.gimbal;

import android.content.ContentValues;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.feyiuremote.libs.Bluetooth.BluetoothViewModel;
import com.feyiuremote.libs.Feiyu.Axes;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.calibration.CalibrationDB;

public class GimbalEmulator {
    private static final String TAG = GimbalEmulator.class.getSimpleName();
    public static boolean isEnabled = false;
    public static BluetoothViewModel btModel = null;
    private static int panSens = 25;
    private static int tiltSens = 25;
    private static int panJoy = 0;
    private static int tiltJoy = 0;

    private static double pan_angle = 0;
    private static double tilt_angle = 0;

    private static CalibrationDB calDB;

    public static ContentValues panCal;
    public static ContentValues tiltCal;

    private static long last_pan_update = 0;
    private static long last_tilt_update = 0;
    private static boolean logging = true;

    private static boolean overshootEnabled = true;

    public static void init(BluetoothViewModel btViewModel) {
        calDB = CalibrationDB.get();
        if (calDB == null) {
            throw new RuntimeException("Please provide CalibrationDB!");
        }

        btModel = btViewModel;

        btModel.connected.setValue(true);

        FeyiuState.getInstance().angle_pan.update(0);
        FeyiuState.getInstance().angle_tilt.update(0);
        FeyiuState.getInstance().angle_yaw.update(0);
        FeyiuState.getInstance().last_update = System.currentTimeMillis();
        FeyiuState.getInstance().update(panSens, tiltSens);

        btModel.feyiuStateUpdated.postValue(System.currentTimeMillis());
        Log.w("WhoIsCallingUpdate", "Emulator");

    }

    private static void applyPanOvershoot() {
        if (panCal != null) {
            double overshoot = panCal.getAsDouble("pan_angle_overshoot");
            pan_angle += overshoot;

            logW("Applying pan overshoot of:" + overshoot);
        }
    }

    private static void applyTiltOvershoot() {
        if (tiltCal != null) {
            double overshoot = tiltCal.getAsDouble("tilt_angle_overshoot");
            tilt_angle += overshoot;

            logW("Applying tilt overshoot of:" + overshoot);
        }
    }

    public static void simulatePanAngle() {
        long timePassed = timeSinceLastPanUpdate();

        if (panJoy != 0) {
            panCal = getPanCal();

            pan_angle += panCal.getAsDouble("pan_speed") * (timePassed / 1000.0);
            logD("Simulating pan angle (" + panJoy + "): " + pan_angle + " deg");
        }

        last_pan_update = System.currentTimeMillis();
    }

    public static void simulateTiltAngle() {
        long timePassed = timeSinceLastTiltUpdate();

        if (tiltJoy != 0) {
            tiltCal = getTiltCal();

            tilt_angle += tiltCal.getAsDouble("tilt_speed") * (timePassed / 1000.0);
            ;
            logD("Simulating tilt angle (" + tiltJoy + "): " + tilt_angle + " deg");
        }

        last_tilt_update = System.currentTimeMillis();
    }

    public static void emulateGimbalPosition() {
        simulatePanAngle();
        simulateTiltAngle();

        FeyiuState.getInstance().angle_pan.update((int) (Math.round(pan_angle * 100)));
        FeyiuState.getInstance().angle_tilt.update((int) (Math.round(tilt_angle * 100)));
        FeyiuState.getInstance().angle_yaw.update(0);
        FeyiuState.getInstance().last_update = System.currentTimeMillis();

        last_pan_update = System.currentTimeMillis();
        last_tilt_update = System.currentTimeMillis();

        btModel.feyiuStateUpdated.postValue(System.currentTimeMillis());
    }

    public static long timeSinceLastPanUpdate() {
        return System.currentTimeMillis() - last_pan_update;
    }

    public static long timeSinceLastTiltUpdate() {
        return System.currentTimeMillis() - last_tilt_update;
    }


    public static ContentValues getPanCal() {
        if (panJoy != 0) {
            ContentValues cal = calDB.getByJoyState(CalibrationDB.AXIS_PAN, panSens, panJoy);
//            logE("Getting sim pan calibration by: s" + panSens + " v" + panJoy);

            if (cal == null) {
                logE("Could not get pan calibration for: " + panSens + " " + panJoy);
            }

            return cal;
        }

        return null;
    }

    public static ContentValues getTiltCal() {
        if (tiltJoy != 0) {
            ContentValues cal = calDB.getByJoyState(CalibrationDB.AXIS_TILT, tiltSens, tiltJoy);
//            logE("Getting sim tilt calibration by: s" + panSens + " v" + panJoy);

            if (cal == null) {
                logE("Could not get tilt calibration for: " + tiltSens + " " + tiltJoy);
            }

            return cal;
        }

        return null;
    }

    public static void setJoySensitivity(Axes.Axis axis, int value) {
        if (axis == Axes.Axis.PAN) {
            setPanSensitivity(value);
        } else {
            setTiltSensitivity(value);
        }

    }

    public static void setPanSensitivity(int value) {
        simulatePanAngle();

        panSens = value;
        FeyiuState.joy_sens_pan = value;
    }

    public static void setTiltSensitivity(int value) {
        simulateTiltAngle();

        tiltSens = value;
        FeyiuState.joy_sens_tilt = value;
    }

    public static void setPanJoy(int value) {
        FeyiuState.joy_val_pan = value;

        if (value == panJoy) return;

        logD("Seting pan joy (" + panJoy + ") to: " + value);
        simulatePanAngle();

        if (overshootEnabled) {
            if (panJoy != 0 && value == 0) {
                applyPanOvershoot();
            }
        }

        if (value == 0) {
            panCal = null;
        }

        panJoy = value;
        FeyiuState.joy_val_pan = panJoy;
    }

    public static void setTiltJoy(int value) {
        FeyiuState.joy_val_tilt = value;

        if (value == tiltJoy) return;

        logD("Setting tilt joy (" + tiltJoy + ") to: " + value);
        simulateTiltAngle();

        if (overshootEnabled) {
            if (tiltJoy != 0 && value == 0) {
                applyTiltOvershoot();
            }
        }

        if (value == 0) {
            tiltCal = null;
        }

        tiltJoy = value;

        FeyiuState.joy_val_tilt = tiltJoy;
    }


    public static void enable() {
        gimbalHandler.post(gimbalRunnable);
        isEnabled = true;
    }

    private static final Handler gimbalHandler = new Handler(Looper.getMainLooper());
    private static final Runnable gimbalRunnable = new Runnable() {
        @Override
        public void run() {
            emulateGimbalPosition();
            gimbalHandler.postDelayed(this, FeyiuState.getInstance().getAverageUpdateIntervalMs());
        }
    };

    public static void logW(String msg) {
        if (logging) {
            Log.w(TAG, msg);
        }
    }

    public static void logD(String msg) {
        if (logging) {
            Log.d(TAG, msg);
        }
    }

    public static void logE(String msg) {
        Log.e(TAG, msg);
    }


}
