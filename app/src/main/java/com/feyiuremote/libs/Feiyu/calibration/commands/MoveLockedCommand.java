package com.feyiuremote.libs.Feiyu.calibration.commands;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.FeyiuUtils;

public class MoveLockedCommand extends GimbalCommand {
    private final int joyVal;

    public MoveLockedCommand(BluetoothLeService bt, int joy_value) {
        super(bt);

        this.joyVal = joy_value;
    }

    @Override
    void execute() {
        mBt.send(FeyiuUtils.SERVICE_ID, FeyiuUtils.CONTROL_CHARACTERISTIC_ID,
                FeyiuUtils.move(joyVal, joyVal)
        );
    }
}
