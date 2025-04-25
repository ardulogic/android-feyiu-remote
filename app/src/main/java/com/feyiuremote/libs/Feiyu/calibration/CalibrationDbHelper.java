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

    public static final int PAN_ONLY = 1;
    public static final int TILT_ONLY = 2;
    public static final int LOCKED = 0;
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
        return "create table " + getDatabaseTableName() + "("
                + "_id integer primary key autoincrement, "
                + "preset VARCHAR(64) NOT NULL, "

                + "pan_only INTEGER NOT NULL CHECK (pan_only IN (0, 1)), "
                + "tilt_only INTEGER NOT NULL CHECK (tilt_only IN (0, 1)), "
                + "locked INTEGER NOT NULL CHECK (tilt_only IN (0, 1)), "

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
                + ");"

                // Add indexes for optimization
                + "CREATE INDEX idx_dir_type ON " + getDatabaseTableName() + " (dir, pan_only, tilt_only, locked); "
                + "CREATE INDEX idx_pan_speed ON " + getDatabaseTableName() + " (pan_speed); "
                + "CREATE INDEX idx_tilt_speed ON " + getDatabaseTableName() + " (tilt_speed); "
                + "CREATE INDEX idx_type_pan_speed ON " + getDatabaseTableName() + " (pan_only, pan_speed); "
                + "CREATE INDEX idx_type_tilt_speed ON " + getDatabaseTableName() + " (tilt_only, tilt_speed); ";
    }

    public void createIndexes() {
        String tableName = getDatabaseTableName();
        try {
            // Create index for dir, pan_only, tilt_only, locked
            dbHandler.execSQL("CREATE INDEX IF NOT EXISTS idx_dir_type ON " + tableName + " (dir, pan_only, tilt_only, locked);");

            // Create index for pan_speed
            dbHandler.execSQL("CREATE INDEX IF NOT EXISTS idx_pan_speed ON " + tableName + " (pan_speed);");

            // Create index for tilt_speed
            dbHandler.execSQL("CREATE INDEX IF NOT EXISTS idx_tilt_speed ON " + tableName + " (tilt_speed);");

            // Create index for pan_only and pan_speed
            dbHandler.execSQL("CREATE INDEX IF NOT EXISTS idx_type_pan_speed ON " + tableName + " (pan_only, pan_speed);");

            // Create index for tilt_only and tilt_speed
            dbHandler.execSQL("CREATE INDEX IF NOT EXISTS idx_type_tilt_speed ON " + tableName + " (tilt_only, tilt_speed);");

            Log.d("DB_INDEX", "Indexes created successfully for " + tableName);
        } catch (SQLException e) {
            Log.e("DB_INDEX", "Failed to create indexes for " + tableName + ": " + e.getMessage());
        }
    }


    @Override
    public String[] getColumnNames() {
        return new String[]{"_id", "preset", "pan_only", "tilt_only", "locked", "joy_sens", "joy_val",
                "pan_speed", "pan_angle_overshoot", "pan_angle_diff",
                "tilt_speed", "tilt_angle_overshoot", "tilt_angle_diff",
                "dir", "time_ms"
        };
    }

    @Override
    protected ContentValues parseRow(Cursor c) {
        ContentValues row = new ContentValues();

        row.put("_id", c.getLong(0));
        row.put("preset", c.getString(1));
        row.put("pan_only", c.getInt(2));
        row.put("tilt_only", c.getInt(3));
        row.put("locked", c.getInt(4));
        row.put("joy_sens", c.getInt(5));
        row.put("joy_val", c.getInt(6));
        row.put("pan_speed", c.getDouble(7));
        row.put("pan_angle_overshoot", c.getDouble(8));
        row.put("pan_angle_diff", c.getDouble(9));
        row.put("tilt_speed", c.getDouble(10));
        row.put("tilt_angle_overshoot", c.getDouble(11));
        row.put("tilt_angle_diff", c.getDouble(12));
        row.put("dir", c.getInt(13));
        row.put("time_ms", c.getInt(14));

        return row;
    }

    public void updateOrCreate(ContentValues cv) {
        int dir = cv.getAsInteger("joy_val") > 0 ? 1 : -1;
        cv.put("dir", dir);

        int id = (int) dbHandler.update(getDatabaseTableName(), cv, "preset=? AND pan_only=? AND tilt_only=? AND locked=? AND joy_val=? AND joy_sens=? AND dir=?", new String[]{
                cv.getAsString("preset"),
                cv.getAsString("pan_only"),
                cv.getAsString("tilt_only"),
                cv.getAsString("locked"),
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

    public ArrayList<ContentValues> getByJoyState(int joy_sen, int joy_val, int type) {
        String type_col = "locked";

        if (type == PAN_ONLY) {
            type_col = "pan_only";
        }

        if (type == TILT_ONLY) {
            type_col = "tilt_only";
        }

        try (Cursor c = dbHandler.query(
                getDatabaseTableName(),
                getColumnNames(),
                "(joy_val=? OR joy_val=?) AND joy_sens=? AND " + type_col + "=?",
                new String[]{
                        String.valueOf(joy_val),
                        String.valueOf(-joy_val),
                        String.valueOf(joy_sen),
                        String.valueOf(1)
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

    public ContentValues getByClosestSpeed(String axis, Double speed, int type) {
        String type_col = "locked";

        if (type == PAN_ONLY) {
            type_col = "pan_only";
        }

        if (type == TILT_ONLY) {
            type_col = "tilt_only";
        }

        try (Cursor c = dbHandler.rawQuery(
                "SELECT * FROM " + getDatabaseTableName() + " WHERE " + type_col + "=1 ORDER BY ABS(? - " + axis + "_speed) " +
                        "LIMIT 1;",
                new String[]{
                        String.valueOf(speed)
                }
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

    public ContentValues getByClosestSpeed(String axis, int sens, Double speed, int type) {
        String type_col = "locked";

        if (type == PAN_ONLY) {
            type_col = "pan_only";
        }

        if (type == TILT_ONLY) {
            type_col = "tilt_only";
        }

        try (Cursor c = dbHandler.rawQuery(
                "SELECT * FROM " + getDatabaseTableName() + " WHERE joy_sens=" + sens + " AND " + type_col + "=1 ORDER BY ABS(? - " + axis + "_speed) " +
                        "LIMIT 1;",
                new String[]{
                        String.valueOf(speed)
                }
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

    public ArrayList<ContentValues> getSlowerThan(String axis, int dir, Double max_speed, int type) {
        String type_col = "locked";

        if (type == PAN_ONLY) {
            type_col = "pan_only";
        } else if (type == TILT_ONLY) {
            type_col = "tilt_only";
        }

        String selection = "dir = ? AND " + type_col + " = 1 AND " + axis + "_speed BETWEEN ? AND ?";
        String[] selectionArgs = new String[]{
                String.valueOf(dir),
                String.valueOf(-Math.abs(max_speed)),
                String.valueOf(Math.abs(max_speed))
        };

        try (Cursor c = dbHandler.query(
                getDatabaseTableName(),
                getColumnNames(),
                selection,
                selectionArgs,
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
