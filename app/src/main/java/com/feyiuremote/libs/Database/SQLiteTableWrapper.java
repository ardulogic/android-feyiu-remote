package com.feyiuremote.libs.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

abstract public class SQLiteTableWrapper {

    protected static String getDatabaseName() {
        return "sqlite_db";
    }

    protected abstract String getDatabaseTableName();

    protected static SQLiteDatabase dbHandler;

    protected abstract String getTableCreateSQL();

    protected static final int DATABASE_VERSION = 1;

    protected abstract String[] getColumnNames();

    public SQLiteTableWrapper(Context context) {
        if (dbHandler == null) {
            dbHandler = context.openOrCreateDatabase(getDatabaseName(), Context.MODE_PRIVATE, null);

            if (!tableExists(getDatabaseTableName())) {
                dbHandler.execSQL(getTableCreateSQL());
            }
        }
    }

    public boolean tableExists(String tableName) {
        if (dbHandler != null) {
            String query = "select DISTINCT tbl_name from sqlite_master where tbl_name = '" + tableName + "'";
            try (Cursor cursor = dbHandler.rawQuery(query, null)) {
                if (cursor != null) {
                    if (cursor.getCount() > 0) {
                        return true;
                    }
                }
                return false;
            }
        }

        return false;
    }

    public void close() {
        dbHandler.close();
        dbHandler = null;
    }

    public void create(ContentValues values) {
        dbHandler.insert(getDatabaseTableName(), null, values);
    }

    public void delete(long rowId) {
        dbHandler.delete(getDatabaseTableName(), "_id=" + rowId, null);
    }

    protected abstract ContentValues parseRow(Cursor c);

    public List<ContentValues> getAll() {
        ArrayList<ContentValues> ret = new ArrayList<ContentValues>();
        try {
            Cursor c = dbHandler.query(getDatabaseTableName(), getColumnNames(),
                    null, null, null, null, null);

            int numRows = c.getCount();
            c.moveToFirst();

            for (int i = 0; i < numRows; ++i) {
                ret.add(parseRow(c));
                ContentValues row = new ContentValues();
                ret.add(row);
                c.moveToNext();
            }
        } catch (SQLException e) {
            Log.e("Exception on query", e.toString());
        }

        return ret;
    }

    public ContentValues get(long rowId) {
        ContentValues cv = new ContentValues();

        Cursor c = dbHandler.query(getDatabaseTableName(), getColumnNames(), "_id=" + rowId,
                null, null, null, null);

        return parseRow(c);
    }

    public void update(long rowId, ContentValues values) {
        dbHandler.update(getDatabaseTableName(), values, "_id=" + rowId, null);
    }

}
