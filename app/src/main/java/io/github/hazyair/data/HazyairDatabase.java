package io.github.hazyair.data;


import android.database.sqlite.SQLiteDatabase;

import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.OnUpgrade;
import net.simonvt.schematic.annotation.Table;

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