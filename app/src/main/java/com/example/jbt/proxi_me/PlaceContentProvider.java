package com.example.jbt.proxi_me;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;

/**
 * Created by Elena Fainleib on 12/11/2016.
 *
 * This class is a content provider for the search result and the favorites database
 */

public class PlaceContentProvider extends ContentProvider {

    private static final String AUTHORITY = "com.example.jbt.proxi_me";

    public static final Uri SEARCH_HISTORY_TABLE_NAME = Uri.parse("content://" + AUTHORITY + "/" + PlaceDBHelper.SEARCH_HISTORY_TABLE_NAME);
    public static final Uri FAVORITES_TABLE_NAME = Uri.parse("content://" +AUTHORITY + "/" + PlaceDBHelper.FAVORITES_TABLE_NAME);

    private PlaceDBHelper placeDBHelper;

    @Override
    public boolean onCreate() {

        placeDBHelper = new PlaceDBHelper(getContext());
        if (placeDBHelper == null) {
            return false;
        }
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] columns, String whereClause, String[] whereArgs, String sortOrder) {
        // Brings data from database by received parameters
        SQLiteDatabase db = placeDBHelper.getReadableDatabase();
        Cursor cursor = db.query(uri.getLastPathSegment(), columns, whereClause, whereArgs, null, null, sortOrder);

        return cursor;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        // Inserts received values into the database
        SQLiteDatabase db = placeDBHelper.getWritableDatabase();
        db.insert(uri.getLastPathSegment(), null, contentValues);
        db.close();

        return uri;
    }

    @Override
    public int delete(Uri uri, String whereClause, String[] whereArgs) {
        // Deletes records from database by received parameters
        SQLiteDatabase db = placeDBHelper.getWritableDatabase();
        int deletedRows = db.delete(uri.getLastPathSegment(), whereClause, whereArgs);
        db.close();

        return deletedRows;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String whereClause, String[] whereArgs) {

        // Updates specific records in the database - didn't have to use it yet but maybe in the future
        SQLiteDatabase db = placeDBHelper.getWritableDatabase();
        int updatedRows = db.update(uri.getLastPathSegment(),contentValues, whereClause, whereArgs);

        return updatedRows;
    }
}
