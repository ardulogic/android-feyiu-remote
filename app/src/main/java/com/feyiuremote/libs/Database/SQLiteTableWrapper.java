package com.feyiuremote.libs.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

abstract public class SQLiteTableWrapper {

    protected static String getDatabaseName() {
        return "sqlite_db.sqlite";
    }

    protected abstract String getDatabaseTableName();

    protected static SQLiteDatabase dbHandler;

    protected abstract String getTableCreateSQL();

    protected static final int DATABASE_VERSION = 1;

    protected abstract String[] getColumnNames();

    public SQLiteTableWrapper(Context context) {
        if (dbHandler == null) {
            dbHandler = context.openOrCreateDatabase(getDatabaseName(), Context.MODE_PRIVATE, null);
        }

        if (!tableExists(getDatabaseTableName())) {
            dbHandler.execSQL(getTableCreateSQL());
        }
    }

    protected abstract ContentValues parseRow(Cursor c);

    public boolean tableExists(String tableName) {
        String query = "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?";

        try (Cursor cursor = dbHandler.rawQuery(query, new String[]{tableName})) {
            return (cursor != null && cursor.getCount() > 0);
        } catch (SQLException e) {
            Log.e("Exception on query", e.toString());
        }

        return false;
    }

    protected int tableRowCount(String tableName) {
        // Prepare SQL query to count all rows in the specified table
        String query = "SELECT COUNT(*) FROM " + tableName; // Directly using tableName

        int count = 0; // Variable to hold the row count
        try (Cursor cursor = dbHandler.rawQuery(query, null)) { // Execute the query
            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0); // Get the count from the first column
            }
        } catch (SQLException e) {
            Log.e("Exception on count query", e.toString()); // Log any exceptions
        }

        return count; // Return the row count
    }

    public int rowCount() {
        // Prepare SQL query to count all rows in the specified table
        String query = "SELECT COUNT(*) FROM " + getDatabaseTableName(); // Directly using tableName

        int count = 0; // Variable to hold the row count
        try (Cursor cursor = dbHandler.rawQuery(query, null)) { // Execute the query
            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0); // Get the count from the first column
            }
        } catch (SQLException e) {
            Log.e("Exception on count query", e.toString()); // Log any exceptions
        }

        return count; // Return the row count
    }


    public void create(ContentValues values) {
        if (values.containsKey("_id")) {
            values.remove("_id");
        }

        dbHandler.insert(getDatabaseTableName(), null, values);
    }

    public void delete(long rowId) {
        dbHandler.delete(getDatabaseTableName(), "_id=" + rowId, null);
    }

    public ContentValues get(long rowId) {
        try (Cursor c = dbHandler.query(getDatabaseTableName(), getColumnNames(), "_id=" + rowId, null, null, null, null)) {
            if (c.getCount() > 0) {
                c.moveToFirst();
                return parseRow(c);
            }
        } catch (SQLException e) {
            Log.e("Exception on query", e.toString());
        }

        return null;
    }

    public List<ContentValues> getAll() {
        try (Cursor c = dbHandler.query(getDatabaseTableName(), getColumnNames(), null, null, null, null, null)) {
            return buildResults(c);
        } catch (SQLException e) {
            Log.e("Exception on query", e.toString());
        }

        return null;
    }

    public void update(long rowId, ContentValues values) {
        dbHandler.update(getDatabaseTableName(), values, "_id=" + rowId, null);
    }

    public void deleteAll() {
        dbHandler.delete(getDatabaseTableName(), null, null);
    }

    public void close() {
        dbHandler.close();
        dbHandler = null;
    }

    protected ContentValues buildResult(Cursor c) {
        ContentValues cv = new ContentValues();
        DatabaseUtils.cursorRowToContentValues(c, cv);
        return cv;
    }

    protected ArrayList<ContentValues> buildResults(Cursor c) {
        ArrayList<ContentValues> ret = new ArrayList<ContentValues>();

        int numRows = c.getCount();
        c.moveToFirst();

        for (int i = 0; i < numRows; ++i) {
            ret.add(parseRow(c));
            c.moveToNext();
        }

        return ret;
    }
}
