package io.github.hazyair.data;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.support.annotation.Nullable;
import android.support.v4.content.CursorLoader;
import android.net.Uri;

import io.github.hazyair.source.Data;
import io.github.hazyair.source.Sensor;
import io.github.hazyair.source.Station;

public class DataLoader extends CursorLoader {

    public DataLoader(Context context, Uri uri, int _id, int limit) {
        super(context, uri, new Data().keys(), DataContract.COLUMN__SENSOR_ID + "=?",
                new String[] { String.valueOf(_id) }, HazyairProvider.Data.DEFAULT_SORT +
                        (limit == 0 ? "" : " LIMIT " + String.valueOf(limit)));
    }

    public static DataLoader newInstanceForLastDataFromSensor(Context context, int _id) {
        return new DataLoader(context, HazyairProvider.Data.CONTENT_URI, _id, 1);
    }

    public static DataLoader newInstanceForAllDataFromSensor(Context context, int _id) {
        return new DataLoader(context, HazyairProvider.Data.CONTENT_URI, _id, 0);
    }
}