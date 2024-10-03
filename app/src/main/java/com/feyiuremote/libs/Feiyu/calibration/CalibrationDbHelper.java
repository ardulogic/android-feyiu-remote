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

    public final String AXIS_PAN = "pan";
    public final String AXIS_TILT = "tilt";

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
                + "name string not null,"
                + "joy_sens int not null,"
                + "joy_val int not null,"

                + "pan_speed Double not null,"
                + "pan_angle_overshoot Double not null,"
                + "pan_angle_diff Double not null,"

                + "tilt_speed Double Double null,"
                + "tilt_angle_overshoot Double not null,"
                + "tilt_angle_diff Double not null,"

                + "dir int not null,"
                + "time_ms int not null"
                + ");";
    }

    @Override
    protected String[] getColumnNames() {
        return new String[]{"_id", "name", "joy_sens", "joy_val",
                "pan_speed", "pan_angle_overshoot", "pan_angle_diff",
                "tilt_speed", "tilt_angle_overshoot", "tilt_angle_diff",
                "dir", "time_ms"
        };
    }

    @Override
    protected ContentValues parseRow(Cursor c) {
        ContentValues row = new ContentValues();

        row.put("_id", c.getLong(0));
        row.put("name", c.getString(1));
        row.put("joy_sens", c.getInt(2));
        row.put("joy_val", c.getInt(3));
        row.put("pan_speed", c.getDouble(4));
        row.put("pan_angle_overshoot", c.getDouble(5));
        row.put("pan_angle_diff", c.getDouble(6));
        row.put("tilt_speed", c.getDouble(7));
        row.put("tilt_angle_overshoot", c.getDouble(8));
        row.put("tilt_angle_diff", c.getDouble(9));
        row.put("dir", c.getInt(10));
        row.put("time_ms", c.getInt(11));

        return row;
    }

    public void updateOrCreate(ContentValues cv) {
        int dir = cv.getAsInteger("joy_val") > 0 ? 1 : -1;
        cv.put("dir", dir);

        int id = (int) dbHandler.update(getDatabaseTableName(), cv, "name=? AND joy_val=? AND joy_sens=? AND dir=?", new String[]{
                cv.getAsString("name"),
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
        try (Cursor c = dbHandler.query(
                getDatabaseTableName(),
                getColumnNames(),
                "(joy_val=? OR joy_val=?) AND joy_sens=?",
                new String[]{
                        String.valueOf(joy_val),
                        String.valueOf(-joy_val),
                        String.valueOf(joy_sen)
                },
                null,
                null,
                null,
                null
        )) {
            return buildResults(c);
        } catch (SQLException e) {
            Log.e("Exception on query", e.toString());
        }

        return new ArrayList<>();
    }

    public ContentValues getByClosestSpeed(String axis, Double v) {
        try (Cursor c = dbHandler.rawQuery(
                "SELECT * FROM " + getDatabaseTableName() + " ORDER BY ABS(? - " + axis + "_speed) " +
                        "LIMIT 1;",
                new String[]{String.valueOf(v)}
        )) {

            if (c.getCount() > 0) {
                c.moveToFirst();
                return parseRow(c);
            }
        } catch (SQLException e) {
            Log.e("Exception on query", e.toString());
        }

        return null;
    }

    public ContentValues getByClosestSpeed(String axis, int sens, Double v) {
        try (Cursor c = dbHandler.rawQuery(
                "SELECT * FROM " + getDatabaseTableName() + " WHERE joy_sens=" + sens + " ORDER BY ABS(? - " + axis + "_speed) " +
                        "LIMIT 1;",
                new String[]{String.valueOf(v)}
        )) {

            if (c.getCount() > 0) {
                c.moveToFirst();
                return parseRow(c);
            }
        } catch (SQLException e) {
            Log.e("Exception on query", e.toString());
        }

        return null;
    }

    public ArrayList<ContentValues> getSlowerThan(String axis, int dir, Double max_speed) {
        try (Cursor c = dbHandler.query(
                getDatabaseTableName(),
                getColumnNames(),
                "dir=" + dir + " and ABS(" + axis + "_speed) <= " + Math.abs(max_speed),
                null, // Selection Args DONT WORK WITH FUCKING NUMBERS!
                null,
                null,
                null,
                null
        )) {
            return buildResults(c);
        } catch (SQLException e) {
            Log.e("Exception on query", e.toString());
        }

        return null;
    }

}
