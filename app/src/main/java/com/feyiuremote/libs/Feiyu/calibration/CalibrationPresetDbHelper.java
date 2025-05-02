package com.feyiuremote.libs.Feiyu.calibration;

import android.content.Context;

public class CalibrationPresetDbHelper extends CalibrationDB {

    public CalibrationPresetDbHelper(Context context) {
        super(context);
    }

    @Override
    protected String getDatabaseTableName() {
        return "calibration_preset";
    }


}