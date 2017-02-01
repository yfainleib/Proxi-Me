package com.example.jbt.proxi_me;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Elena Fainleib on 12/11/2016.
 *
 * This class builds the tables of the database that holds the search results and the user favorite places
 */

public class PlaceDBHelper extends SQLiteOpenHelper {

    public static final String SEARCH_HISTORY_TABLE_NAME = "search_history";
    public static final String FAVORITES_TABLE_NAME = "favorites";

    public static final String ID_COL = "id";
    public static final String NAME_COL = "name";
    public static final String ADDRESS_COL = "address";
    public static final String PHONE_COL = "phone";
    public static final String ICON_REF_COL = "icon_ref";
    public static final String PHOTO_REF_COL = "photo_ref";
    public static final String LATITUDE_COL = "latitude";
    public static final String LONGITUDE_COL = "longitude";

    public PlaceDBHelper(Context context) {
        super(context, "places.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // Creates the tabls in the first run
        String buildTable = String.format("CREATE TABLE %s ( %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s REAL, %s REAL)"
                ,SEARCH_HISTORY_TABLE_NAME, NAME_COL, ADDRESS_COL, PHONE_COL, ICON_REF_COL, PHOTO_REF_COL, LATITUDE_COL, LONGITUDE_COL);
        sqLiteDatabase.execSQL(buildTable);

        buildTable = String.format("CREATE TABLE %s ( %s INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s REAL, %s REAL)"
                ,FAVORITES_TABLE_NAME, ID_COL, NAME_COL, ADDRESS_COL, PHONE_COL, ICON_REF_COL, PHOTO_REF_COL, LATITUDE_COL, LONGITUDE_COL);
        sqLiteDatabase.execSQL(buildTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
