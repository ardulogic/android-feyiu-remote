package com.feyiuremote.libs.Cameras.Panasonic;

import android.util.Log;

import com.feyiuremote.libs.Utils.NamedThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class PanasonicFocus {

    private final static String TAG = PanasonicFocus.class.getSimpleName();
    private final IPanasonicCameraFocusListener baseListener;

    private PanasonicCamera mCamera;

    protected static final ThreadFactory threadFactory = new NamedThreadFactory("PanasonicFocus");

    protected static final ExecutorService executor = Executors.newFixedThreadPool(1, threadFactory);

    private boolean focusActive = false;

    private Double focusCurrent = null;

    private final int MAX_EXCEPTIONS = 2;
    private int requestRetries = 0;

    private int focusSpeed = 0;

    // Scenario 1: Switch at AFS/AFF
    // First AEL lock has to be done so that it would receive focus commands!
    // Same goes to video, if you dont do AEL before start recording, say goodbye to fucking focus

    // Scenario 2: Switch at MF
    // Seems like this is the answer to all the bullshit
    // It should work only via focus commands without having to send AEL
    // Works with both video and photo, fuck yeah

    // Smothness
    // Smoothness depends on resolution, 4k is laggy
    // 24fps - everything is laggy

    // Best practice so far:
    // System:
    // Set to NTSC - 60fps
    // Set FHD to 30fps
    // Turn off display on camera

    // Does NOT work when the "Focus square is displayed" when adjusting focus by hand!
    private long time_ael = 0;

    private Double focusTarget = null;

    private Double[] stepSizes = {1.95, 15.62};

    private Double focusPrevious = null;


    /**
     * TODO: Probably gonna have to be reworked since switching between fragments
     * will reset current focus point
     *
     * @param camera
     * @param updateListener
     */
    public PanasonicFocus(PanasonicCamera camera, IPanasonicCameraFocusListener updateListener) {
        this.mCamera = camera;
        this.baseListener = updateListener;
        Log.d(TAG, "Created a panasonic focus");
    }

    public Double[] getStepSizes() {
        return this.stepSizes;
    }

    /**
     * Opens UDP port and starts receiver thread
     */
    public void start() {
        this.calibrate();
    }


    public void calibrate() {
        mCamera.controls.focus(1, new IPanasonicCameraFocusControlListener() {
            @Override
            public void onSuccess(double position) {
                baseListener.onUpdate(position);
                focusCurrent = position;
                focusPrevious = focusCurrent;

                boolean invert = focusCurrent < 20;

                mCamera.controls.focus(invert ? -1 : 1, new IPanasonicCameraFocusControlListener() {
                    @Override
                    public void onSuccess(double position) {
                        baseListener.onUpdate(position);
                        focusCurrent = position;
                        stepSizes[0] = Math.abs(focusCurrent - focusPrevious);
                        Log.d(TAG, "Calibration for step sizes[0] is complete");
                        focusPrevious = focusCurrent;
                        mCamera.controls.focus(invert ? -2 : 2, new IPanasonicCameraFocusControlListener() {

                            @Override
                            public void onSuccess(double position) {
                                baseListener.onUpdate(position);
                                focusCurrent = position;
                                stepSizes[1] = Math.abs(focusCurrent - focusPrevious);
                                Log.d(TAG, "Calibration for step sizes[1] is complete");
                            }

                            @Override
                            public void onFailure() {
                                baseListener.onFailure();
                            }
                        });
                    }

                    @Override
                    public void onFailure() {
                        baseListener.onFailure();
                    }
                });
            }

            @Override
            public void onFailure() {

            }
        });
    }

    public void update() {
        executor.execute(() -> {
            // To retrieve position just slightly bump focus
            mCamera.controls.focus(1, new IPanasonicCameraFocusControlListener() {
                @Override
                public void onSuccess(double position) {
                    focusCurrent = position;
                    baseListener.onUpdate(position);
                }

                @Override
                public void onFailure() {
                    baseListener.onFailure();
                }
            });
        });
    }

    public void update(IPanasonicCameraFocusControlListener listener) {
        executor.execute(() -> {
            // To retrieve position just slightly bump focus
            mCamera.controls.focus(1, new IPanasonicCameraFocusControlListener() {
                @Override
                public void onSuccess(double position) {
                    focusCurrent = position;
                    baseListener.onUpdate(position);
                    listener.onSuccess(position);
                }

                @Override
                public void onFailure() {
                    baseListener.onFailure();
                    listener.onFailure();
                }
            });
        });
    }

    public void stop() {
        focusActive = false;
    }

    public boolean isActive() {
        return focusActive;
    }

    public double getAbsFocusDiff() {
        return Math.abs(focusTarget - focusCurrent);
    }

    public boolean focusTargetReached() {
        return (getAbsFocusDiff() < stepSizes[0] * 1.1);
    }

    private int chooseBestSpeed() {
        int speed = focusCurrent < focusTarget ? -1 : 1;

        if (getAbsFocusDiff() > stepSizes[1] * 2) {
            speed *= 2;
        }

        return speed;
    }

    /**
     * After ~300 frames camera halts the stream
     * it needs to be requested again
     */
    public void focusTo(double target) {
        this.focusTarget = target;

        if (!focusActive) {
            executeFocusStep();
        }
    }

    private void executeFocusStep() {
        focusActive = true;
        if (isHealthy()) {
            executor.execute(() -> {
                // To retrieve position just slightly bump focus
                if (!focusTargetReached()) {
                    int speed = chooseBestSpeed();
                    mCamera.controls.focus(speed, new IPanasonicCameraFocusControlListener() {
                        @Override
                        public void onSuccess(double position) {
                            focusCurrent = position;
                            baseListener.onUpdate(position);
                            executeFocusStep();
                        }

                        @Override
                        public void onFailure() {
                            requestRetries += 1;

                            if (requestRetries < MAX_EXCEPTIONS) {
                                executeFocusStep();
                            } else {
                                baseListener.onFailure();
                            }
                        }
                    });
                } else {
                    focusActive = false;
                    baseListener.onTargetReached(focusCurrent);
                }
            });
        } else {
            Log.e(TAG, "Could not focus, because focus position is uknown!");
        }
    }

    public Double getTarget() {
        return focusTarget;
    }

    public boolean isHealthy() {
        return focusCurrent != null;
    }

    public Double getCurrent() {
        return focusCurrent;
    }
}