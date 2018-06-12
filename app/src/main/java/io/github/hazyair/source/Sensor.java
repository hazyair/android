package io.github.hazyair.source;

import android.database.Cursor;
import android.os.Bundle;

public class Sensor extends Base {
    public int _id;
    public int _station_id;
    public String id;
    public String station;
    public String parameter;
    public String unit;

    public static Bundle loadBundleFromCursor(Cursor cursor) {
        return new Sensor()._loadBundleFromCursor(cursor);
    }

    public Sensor() {
        this(null, null, null, null);
    }

    public Sensor(String id, String station, String parameter, String unit) {
        this.id = (id == null ? "" : id);
        this.station = (station == null ? "" : station);
        this.parameter = (parameter == null ? "" : parameter);
        this.unit = (unit == null ? "" : unit);
    }
}
