package com.feyiuremote.libs.Feiyu.calibration;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.feyiuremote.libs.Database.SQLiteTableWrapper;

public class CalibrationPresetDbHelper extends CalibrationDbHelper {

    public CalibrationPresetDbHelper(Context context) {
        super(context);
    }

    @Override
    protected String getDatabaseTableName() {
        return "calibration_preset";
    }


}