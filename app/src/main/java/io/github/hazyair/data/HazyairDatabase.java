package io.github.hazyair.data;


import android.database.sqlite.SQLiteDatabase;

import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.OnUpgrade;
import net.simonvt.schematic.annotation.Table;

@Database(version = HazyairDatabase.VERSION)
public class HazyairDatabase {

    public static final int VERSION = 1;

    @Table(StationsContract.class)
    public static final String STATIONS = "stations";

    @Table(SensorsContract.class)
    public static final String SENSORS = "sensors";

    @Table(DataContract.class)
    public static final String DATA = "data";

    @OnUpgrade
    public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + STATIONS);
        db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" + STATIONS + "'");
        db.execSQL(io.github.hazyair.data.generated.HazyairDatabase.STATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + SENSORS);
        db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" + SENSORS + "'");
        db.execSQL(io.github.hazyair.data.generated.HazyairDatabase.SENSORS);
        db.execSQL("DROP TABLE IF EXISTS " + DATA);
        db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" + DATA + "'");
        db.execSQL(io.github.hazyair.data.generated.HazyairDatabase.DATA);    }

}
/*
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class StationsDBHelper extends SQLiteOpenHelper {
    private final static String DATABSE_NAME = "stations.db";
    private final static int DATABASE_VERSION = 1;

    public StationsDBHelper(Context context) {
        super(context, DATABSE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE_TABLE_STATIONS =
                "CREATE TABLE " + StationsContract.Stations.TABLE_NAME + "(" +
                        StationsContract.Stations.COLUMN__ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        StationsContract.Stations.COLUMN_ID + " INT, " +
                        StationsContract.Stations.COLUMN_ADDRESS + " TEXT NOT NULL, " +
                        StationsContract.Stations.COLUMN_NAME + " TEXT NOT NULL, " +
                        StationsContract.Stations.COLUMN_LOCALITY + " TEXT NOT NULL, " +
                        StationsContract.Stations.COLUMN_COUNTRY + " TEXT NOT NULL, " +
                        StationsContract.Stations.COLUMN_LATITUDE + " TEXT NOT NULL, " +
                        StationsContract.Stations.COLUMN_LONGITUDE + " TEXT NOT NULL);";
        db.execSQL(SQL_CREATE_TABLE_STATIONS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + StationsContract.Stations.TABLE_NAME);
        db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" +
                StationsContract.Stations.TABLE_NAME + "'");
        onCreate(db);
    }
}*/
