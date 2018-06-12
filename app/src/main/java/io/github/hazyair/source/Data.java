package io.github.hazyair.source;

import android.database.Cursor;
import android.os.Bundle;

public class Data extends Base {
    public int _id;
    public int _station_id;
    public int _sensor_id;
    public long timestamp;
    public Double value;

    public static Bundle loadBundleFromCursor(Cursor cursor) {
        return new Data()._loadBundleFromCursor(cursor);
    }

    public Data() {
        this(0, 0.0);
    }

    public Data(long timestamp, Double value) {
        this.timestamp = timestamp;
        this.value = value;
    }
}
