package com.feyiuremote.libs.Feiyu.calibration;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.feyiuremote.libs.Database.SQLiteTableWrapper;

import java.util.ArrayList;

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
        return "create table " + getDatabaseTableName() + "(_id integer primary key autoincrement, "
                + "joy_sens int not null,"
                + "joy_val int not null,"
                + "pan_speed float not null,"
                + "pan_overshoot int not null,"
                + "pan_dist int not null,"
                + "tilt_speed float not null,"
                + "tilt_overshoot int not null,"
                + "tilt_dist int not null,"
                + "dir int not null"
                + ");";
    }

    @Override
    protected String[] getColumnNames() {
        return new String[]{"_id", "joy_sens", "joy_val",
                "pan_speed", "pan_overshoot", "pan_dist",
                "tilt_speed", "tilt_overshoot", "tilt_dist",
                "dir"
        };
    }

    @Override
    protected ContentValues parseRow(Cursor c) {
        ContentValues row = new ContentValues();

        row.put("_id", c.getLong(0));
        row.put("joy_sens", c.getInt(1));
        row.put("joy_val", c.getInt(2));
        row.put("pan_speed", c.getFloat(3));
        row.put("pan_overshoot", c.getInt(4));
        row.put("pan_dist", c.getInt(5));
        row.put("tilt_speed", c.getFloat(6));
        row.put("tilt_overshoot", c.getInt(7));
        row.put("tilt_dist", c.getInt(8));
        row.put("dir", c.getInt(9));

        return row;
    }

    public void updateOrCreate(ContentValues cv) {
        int dir = cv.getAsInteger("joy_val") > 0 ? 1 : -1;
        cv.put("dir", dir);

        int id = (int) dbHandler.update(getDatabaseTableName(), cv, "joy_val=? AND joy_sens=? AND dir=?", new String[]{
                cv.getAsString("joy_val"),
                cv.getAsString("joy_sens"),
                String.valueOf(dir),
        });

        if (id == 0) {
            dbHandler.insertWithOnConflict(getDatabaseTableName(), null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    public void update(long rowId, ContentValues values) {
        dbHandler.update(getDatabaseTableName(), values, "_id=" + rowId, null);
    }

    public ArrayList<ContentValues> getByJoyState(int joy_sen, int joy_val) {
        try {
            Cursor c = dbHandler.query(getDatabaseTableName(), getColumnNames()
                    , "(joy_val=? OR joy_val=?) AND joy_sens=?", new String[]{
                            String.valueOf(joy_val),
                            String.valueOf(-joy_val),
                            String.valueOf(joy_sen),
                    }, null, null, null, null);

            return buildResults(c);
        } catch (SQLException e) {
            Log.e("Exception on query", e.toString());
        }

        return new ArrayList<ContentValues>();
    }

    public ContentValues getByClosestPanSpeed(float v, int dir) {
        Cursor c = dbHandler.rawQuery(
                "SELECT * FROM " + getDatabaseTableName() + " WHERE dir = ? ORDER BY ABS(? - pan_speed) " +
                        "LIMIT 1;",
                new String[]{String.valueOf(dir), String.valueOf(v)}
        );

        if (c.getCount() > 0) {
            c.moveToFirst();
            return parseRow(c);
        }

        return null;
    }

    public ContentValues getByClosestTIltSpeed(float v, int dir) {
        Cursor c = dbHandler.rawQuery(
                "SELECT * FROM " + getDatabaseTableName() + " WHERE dir = ? ORDER BY ABS(? - tilt_speed) " +
                        "LIMIT 1;",
                new String[]{String.valueOf(dir), String.valueOf(v)}
        );

        if (c.getCount() > 0) {
            c.moveToFirst();
            return parseRow(c);
        }

        return null;
    }

}
