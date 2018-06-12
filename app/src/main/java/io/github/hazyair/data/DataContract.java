package io.github.hazyair.data;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.ConflictResolutionType;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.PrimaryKey;

public class DataContract {
    @DataType(DataType.Type.INTEGER)
    @PrimaryKey(onConflict = ConflictResolutionType.REPLACE)
    @AutoIncrement
    public static final String COLUMN__ID = "_id";

    @DataType(DataType.Type.INTEGER)
    public static final String COLUMN__STATION_ID = "_station_id";

    @DataType(DataType.Type.INTEGER)
    public static final String COLUMN__SENSOR_ID = "_sensor_id";

    @DataType(DataType.Type.INTEGER)
    public static final String COLUMN_TIMESTAMP = "timestamp";

    @DataType(DataType.Type.REAL)
    public static final String COLUMN_VALUE = "value";
}
