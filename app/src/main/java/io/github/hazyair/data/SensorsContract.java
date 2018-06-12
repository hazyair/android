package io.github.hazyair.data;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.ConflictResolutionType;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.PrimaryKey;

public class SensorsContract {

    @DataType(DataType.Type.INTEGER)
    @PrimaryKey(onConflict = ConflictResolutionType.REPLACE)
    @AutoIncrement
    public static final String COLUMN__ID = "_id";

    @DataType(DataType.Type.INTEGER)
    public static final String COLUMN__STATION_ID = "_station_id";

    @DataType(DataType.Type.TEXT)
    public static final String COLUMN_ID = "id";

    @DataType(DataType.Type.TEXT)
    public static final String COLUMN_SENSOR = "station";

    @DataType(DataType.Type.TEXT)
    public static final String COLUMN_PARAMETER = "parameter";

    @DataType(DataType.Type.TEXT)
    public static final String COLUMN_UNIT = "unit";



}
