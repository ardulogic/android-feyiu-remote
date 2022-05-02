package com.feyiuremote.libs.Feiyu.calibration;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.feyiuremote.libs.Database.SQLiteTableWrapper;

public class CalibrationDbHelper extends SQLiteTableWrapper {

    public CalibrationDbHelper(Context context) {
        super(context);
    }

    @Override
    protected String getDatabaseTableName() {
        return "calibration";
    }

    @Override
    protected String getTableCreateSQL() {
        return "create table calibration(_id integer primary key autoincrement, "
                + "joy_sens int not null,"
                + "joy_val int not null,"
                + "pan_speed float not null,"
                + "pan_overshoot int not null,"
                + "pan_dist int not null,"
                + "tilt_speed float not null,"
                + "tilt_overshoot int not null,"
                + "tilt_dist int not null"
                + ");";
    }

    @Override
    protected String[] getColumnNames() {
        return new String[]{"joy_sens", "joy_val",
                "speed_pan", "overshoot_pan", "dist_pan",
                "speed_tilt", "overshoot_tilt", "dist_tilt",
        };
    }

    @Override
    protected ContentValues parseRow(Cursor c) {
        ContentValues row = new ContentValues();

        row.put("_id", c.getLong(0));
        row.put("joy_sens", c.getInt(1));
        row.put("joy_val", c.getInt(2));
        row.put("pan_speed", c.getInt(3));
        row.put("pan_overshoot", c.getInt(4));
        row.put("pan_dist", c.getInt(5));
        row.put("tilt_speed", c.getInt(6));
        row.put("tilt_overshoot", c.getInt(7));
        row.put("tilt_dist", c.getInt(7));

        return row;
    }

    public void updateOrCreate(ContentValues cv) {

        int id = (int) dbHandler.insertWithOnConflict(getDatabaseTableName(), null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        if (id == -1) {
            dbHandler.update(getDatabaseTableName(), cv, "joy_val=? AND joy_sens=?", new String[] {
                    cv.getAsString("joy_val"),
                    cv.getAsString("joy_sens"),
            });
        }
    }

    public void update(long rowId, ContentValues values) {
        dbHandler.update(getDatabaseTableName(), values, "_id=" + rowId, null);
    }

}