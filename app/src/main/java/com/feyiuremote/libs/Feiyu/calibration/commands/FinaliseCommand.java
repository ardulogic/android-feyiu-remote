package com.feyiuremote.libs.Feiyu.calibration.commands;

import com.feyiuremote.libs.Bluetooth.BluetoothLeService;
import com.feyiuremote.libs.Feiyu.FeyiuUtils;

public class FinaliseCommand extends GimbalCommand {

    public FinaliseCommand(BluetoothLeService bt) {
        super(bt);
    }

    @Override
    void execute() {
        mBt.send(FeyiuUtils.SERVICE_ID, FeyiuUtils.CONTROL_CHARACTERISTIC_ID,
                FeyiuUtils.move(0, 0)
        );
    }
}
