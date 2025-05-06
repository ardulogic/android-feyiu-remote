package com.feyiuremote.libs.Feiyu.calibration;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.feyiuremote.libs.Database.SQLiteTableWrapper;
import com.feyiuremote.libs.Feiyu.FeyiuState;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CalibrationDB extends SQLiteTableWrapper {
    private final String TAG = CalibrationDB.class.getSimpleName();

    public static final String AXIS_PAN = "pan";
    public static final String AXIS_TILT = "tilt";

    public static CalibrationDB instance;

    public static void init(Context context) {
        instance = new CalibrationDB(context);
    }

    public static CalibrationDB get() {
        return instance;
    }

    public CalibrationDB(Context context) {
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

                + "tilt_speed Double not null,"
                + "tilt_angle_overshoot Double not null,"

                + "dir int not null,"
                + "time_to_accel int not null,"
                + "pan_angle_to_accel Double not null,"
                + "tilt_angle_to_accel Double not null"
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
                "pan_speed", "pan_angle_overshoot",
                "tilt_speed", "tilt_angle_overshoot",
                "dir", "time_to_accel", "pan_angle_to_accel", "tilt_angle_to_accel"
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
        row.put("tilt_speed", c.getDouble(10));
        row.put("tilt_angle_overshoot", c.getDouble(11));
        row.put("dir", c.getInt(13));
        row.put("time_to_accel", c.getInt(14));
        row.put("pan_angle_to_accel", c.getInt(15));
        row.put("tilt_angle_to_accel", c.getInt(16));


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

    public ContentValues getByJoyState(
            String axis,
            int joy_sen,
            int joy_val
    ) {
        // build the SQL via your f() helper and a double-brace inline map
        String sql = f(
                "SELECT * FROM {table} \n" +
                        "WHERE {axis}_only = 1 \n" +
                        "  AND (joy_val = {joyVal}) \n" +
                        "  AND joy_sens = {joySen} \n" +
                        "LIMIT 1",
                new HashMap<String, Object>() {{
                    put("table", getDatabaseTableName());
                    put("axis", axis);
                    put("joyVal", joy_val);
                    put("joySen", joy_sen);
                }}
        );

        try (Cursor c = dbHandler.rawQuery(sql, null)) {
            return buildOneRow(c);
        } catch (SQLException e) {
            Log.e("DB_QUERY_ERROR", "getByJoyState failed", e);
        }

        return null;
    }

    public ContentValues getFastest(String axis, double angleDiff, double reqMaxSpeed) {
        double minTime = FeyiuState.getInstance().getAverageUpdateIntervalMs() / 1000.0;
        double maxSpeed = Math.min(
                reqMaxSpeed,
                Math.abs(angleDiff) * 1000 / FeyiuState.getInstance().getAverageUpdateIntervalMs()
        );

        // single SQL literal
        String sql = f(
                "SELECT * FROM {table} \n" +
                        "WHERE {axis}_only = 1 \n" +
                        "  AND {axis}_speed {angleSign} 0 \n" +
                        "  AND ABS({axis}_speed) <= {maxSpeed} \n" +
                        "  AND {absAngleDiff}/ABS({axis}_speed) >= {minTime} \n" +
                        "ORDER BY {absAngleDiff}/ABS({axis}_speed) ASC \n" +
                        "LIMIT 1",
                new HashMap<String, Object>() {{
                    put("table", getDatabaseTableName());
                    put("axis", axis);
                    put("angleSign", angleDiff > 0 ? ">" : "<");
                    put("maxSpeed", maxSpeed);
                    put("absAngleDiff", Math.abs(angleDiff));
                    put("minTime", minTime);
                }}
        );

        try (Cursor c = dbHandler.rawQuery(sql, null)) {
            return buildOneRow(c);
        } catch (SQLException e) {
            Log.e("DB_QUERY_ERROR", "findFastestCalibration failed", e);
        }

        Log.e("DB_QUERY_ERROR", "No suitable calibration found for " + axis);
        return null;
    }


    public ContentValues getClosestToSpeed(
            String axis,
            Double signedTargetSpeed
    ) {

        String sql = f("SELECT * FROM {table} " +
                        "WHERE {axis}_speed {moreLess} 0 " +
                        "AND {axis}_only = 1 " +
                        "ORDER BY ABS(ABS({axis}_speed) - {absTargetSpeed}) ASC " +
                        "LIMIT 1",
                new HashMap<String, Object>() {{
                    put("table", getDatabaseTableName());
                    put("axis", axis);
                    put("moreLess", signedTargetSpeed >= 0 ? ">" : "<");
                    put("absTargetSpeed", Math.abs(signedTargetSpeed));
                }}
        );

        try (Cursor c = dbHandler.rawQuery(sql, null)) {
            return buildOneRow(c);
        } catch (SQLException e) {
            Log.e("DB_QUERY_ERROR", "getClosestToSpeed failed", e);
        }

        return null;
    }

    protected ContentValues buildOneRow(Cursor c) {
        if (c != null && c.moveToFirst()) {
            ContentValues cv = new ContentValues();
            DatabaseUtils.cursorRowToContentValues(c, cv);
            return cv;
        }

        return null;
    }

    /**
     * Replaces all occurrences of ${key} in the template with vars.get(key).toString().
     * Any placeholder without a corresponding key in vars is left untouched.
     */
    static String f(String template, Map<String, ?> vars) {
        for (Map.Entry<String, ?> entry : vars.entrySet()) {
            // match literal {key}
            String pattern = "\\{" + Pattern.quote(entry.getKey()) + "\\}";
            String replacement = Matcher.quoteReplacement(entry.getValue().toString());
            template = template.replaceAll(pattern, replacement);
        }
        return template;
    }

}
