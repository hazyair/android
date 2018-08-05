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

    @DataType(DataType.Type.INTEGER)
    @NotNull
    public static final String COLUMN_COUNTRY = "country";

    @DataType(DataType.Type.TEXT)
    @NotNull
    public static final String COLUMN_LOCALITY = "locality";

    @DataType(DataType.Type.TEXT)
    @NotNull
    public static final String COLUMN_ADDRESS = "address";

    @DataType(DataType.Type.INTEGER)
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
        return new String[] { station.id, station.name, String.valueOf(station.latitude),
                String.valueOf(station.longitude), String.valueOf(station.country), station.locality,
                station.address, String.valueOf(station.source) };
    }
}