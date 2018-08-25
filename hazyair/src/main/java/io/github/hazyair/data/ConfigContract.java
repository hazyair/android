package io.github.hazyair.data;

import net.simonvt.schematic.annotation.ConflictResolutionType;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.PrimaryKey;

@SuppressWarnings("WeakerAccess")
public class ConfigContract {

    @DataType(DataType.Type.TEXT)
    @PrimaryKey(onConflict = ConflictResolutionType.REPLACE)
    public static final String COLUMN_KEY = "key";

    @DataType(DataType.Type.TEXT)
    public static final String COLUMN_VALUE = "value";
}
