package io.github.hazyair.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.lang.Override;
import java.lang.String;

public class HazyairDatabase extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    public static final String STATIONS_TABLE = "stations";

    public static final String SENSORS_TABLE = "sensors";

    public static final String DATA_TABLE = "data";

    private static final String STATIONS = "CREATE TABLE stations ("
            + StationsContract.COLUMN__ID + " INTEGER PRIMARY KEY ON CONFLICT REPLACE AUTOINCREMENT,"
            + StationsContract.COLUMN_ID + " TEXT,"
            + StationsContract.COLUMN_NAME + " TEXT NOT NULL,"
            + StationsContract.COLUMN_LATITUDE + " REAL NOT NULL,"
            + StationsContract.COLUMN_LONGITUDE + " REAL NOT NULL,"
            + StationsContract.COLUMN_COUNTRY + " TEXT NOT NULL,"
            + StationsContract.COLUMN_LOCALITY + " TEXT NOT NULL,"
            + StationsContract.COLUMN_ADDRESS + " TEXT NOT NULL,"
            + StationsContract.COLUMN_SOURCE + " TEXT NOT NULL)";

    private static final String SENSORS = "CREATE TABLE sensors ("
            + SensorsContract.COLUMN__ID + " INTEGER PRIMARY KEY ON CONFLICT REPLACE AUTOINCREMENT,"
            + SensorsContract.COLUMN__STATION_ID + " INTEGER,"
            + SensorsContract.COLUMN_ID + " TEXT,"
            + SensorsContract.COLUMN_STATION_ID + " TEXT,"
            + SensorsContract.COLUMN_PARAMETER + " TEXT,"
            + SensorsContract.COLUMN_UNIT + " TEXT)";

    private static final String DATA = "CREATE TABLE data ("
            + DataContract.COLUMN__ID + " INTEGER PRIMARY KEY ON CONFLICT REPLACE AUTOINCREMENT,"
            + DataContract.COLUMN__STATION_ID + " INTEGER,"
            + DataContract.COLUMN__SENSOR_ID + " INTEGER,"
            + DataContract.COLUMN_TIMESTAMP + " INTEGER,"
            + DataContract.COLUMN_VALUE + " REAL)";

    private static volatile HazyairDatabase instance;

    private HazyairDatabase(Context context) {
        super(context, "hazyairDatabase.db", null, DATABASE_VERSION);
    }

    public static HazyairDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (HazyairDatabase.class) {
                if (instance == null) {
                    instance = new HazyairDatabase(context);
                }
            }
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(STATIONS);
        db.execSQL(SENSORS);
        db.execSQL(DATA);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + STATIONS_TABLE);
        db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" + STATIONS_TABLE + "'");
        db.execSQL(STATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + SENSORS_TABLE);
        db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" + SENSORS_TABLE + "'");
        db.execSQL(SENSORS);
        db.execSQL("DROP TABLE IF EXISTS " + DATA_TABLE);
        db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" + DATA_TABLE + "'");
        db.execSQL(DATA);
    }
}

/*package io.github.hazyair.data;


import android.database.sqlite.SQLiteDatabase;

import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.OnUpgrade;
import net.simonvt.schematic.annotation.Table;

@SuppressWarnings("WeakerAccess")
@Database(version = HazyairDatabase.VERSION)
public class HazyairDatabase {

    @SuppressWarnings("WeakerAccess")
    static final int VERSION = 1;

    @Table(StationsContract.class)
    public static final String STATIONS = "stations";

    @Table(SensorsContract.class)
    public static final String SENSORS = "sensors";

    @Table(DataContract.class)
    public static final String DATA = "data";

    @SuppressWarnings("unused")
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
*/
