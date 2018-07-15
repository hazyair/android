package io.github.hazyair.data;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.ConflictResolutionType;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;

import io.github.hazyair.source.Station;

@SuppressWarnings("WeakerAccess")
public class StationsContract {

    @DataType(DataType.Type.INTEGER)
    @PrimaryKey(onConflict = ConflictResolutionType.REPLACE)
    @AutoIncrement
    public static final String COLUMN__ID = "_id";

    @DataType(DataType.Type.TEXT)
    public static final String COLUMN_ID = "id";

    @DataType(DataType.Type.TEXT)
    @NotNull
    public static final String COLUMN_NAME = "name";

    @DataType(DataType.Type.REAL)
    @NotNull
    public static final String COLUMN_LATITUDE = "latitude";

    @DataType(DataType.Type.REAL)
    @NotNull
    public static final String COLUMN_LONGITUDE = "longitude";

    @DataType(DataType.Type.TEXT)
    @NotNull
    public static final String COLUMN_COUNTRY = "country";

    @DataType(DataType.Type.TEXT)
    @NotNull
    public static final String COLUMN_LOCALITY = "locality";

    @DataType(DataType.Type.TEXT)
    @NotNull
    public static final String COLUMN_ADDRESS = "address";

    @DataType(DataType.Type.TEXT)
    @NotNull
    public static final String COLUMN_SOURCE = "source";

    public static final String selection = COLUMN_ID + "=? AND " +
            COLUMN_NAME + "=? AND " +
            COLUMN_LATITUDE + "=? AND " +
            COLUMN_LONGITUDE + "=? AND " +
            COLUMN_COUNTRY + "=? AND " +
            COLUMN_LOCALITY + "=? AND " +
            COLUMN_ADDRESS + "=? AND " +
            COLUMN_SOURCE + "=?";

    public static String[] selectionArgs(Station station) {
        return new String[] {station.id, station.name, String.valueOf(station.latitude),
                String.valueOf(station.longitude), station.country, station.locality,
                station.address, station.source};
    }

/*    static final String CONTENT_AUTHORITY = "io.github.hazyair";

    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    private StationsContract() {}

    public static final class Stations implements BaseColumns {
        public static final String TABLE_NAME = "stationsUrl";

        static final String COLUMN__ID = "_id";

        public static final String COLUMN_ID = "id";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_LATITUDE = "latitude";
        public static final String COLUMN_LONGITUDE = "longitude";
        public static final String COLUMN_LOCALITY = "locality";
        public static final String COLUMN_COUNTRY = "country";
        public static final String COLUMN_ADDRESS = "address";

        static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(TABLE_NAME).build();
        static final String CONTENT_DIR_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" +
                CONTENT_AUTHORITY + "/" + TABLE_NAME;
        static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" +
                CONTENT_AUTHORITY + "/" + TABLE_NAME;

        static Uri buildUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }*/

/*    public static final class Sensors implements BaseColumns {
        public static final String TABLE_NAME = "sensors";

        static final String COLUMN__ID = "_id";

        public static final String COLUMN_ID = "id";
        public static final String COLUMN_STATION_ID = "station_id";
        public static final String COLUMN_PARAMETER_NAME = "parameter_name";
        public static final String COLUMN_PARAMETER_FORMULA = "parameter_formula";
        public static final String COLUMN_PARAMETER_CODE = "parameter_code";
        public static final String COLUMN_PARAMETER_ID = "parameter_id";

    }*/


}
