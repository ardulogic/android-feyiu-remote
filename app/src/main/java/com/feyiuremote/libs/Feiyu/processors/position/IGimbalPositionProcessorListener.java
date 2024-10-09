package com.feyiuremote.libs.Feiyu.processors.position;

public interface IGimbalPositionProcessorListener {
    void onTargetReached(GimbalPositionTarget target);

    void onTargetNearlyReached();
}
