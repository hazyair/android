package io.github.hazyair.data;

import android.content.Context;

import androidx.loader.content.CursorLoader;
import io.github.hazyair.source.Data;

public class DataLoader extends CursorLoader {

    @SuppressWarnings("SameParameterValue")
    private DataLoader(Context context, int _id, int limit) {
        super(context, HazyairProvider.Data.CONTENT_URI, Data.keys(),
                DataContract.COLUMN__SENSOR_ID + "=?",
                new String[] { String.valueOf(_id) }, HazyairProvider.Data.DEFAULT_SORT +
                        (limit == 0 ? "" : " LIMIT " + limit));
    }

    public static DataLoader newInstanceForLastDataFromSensor(Context context, int _id) {
        return new DataLoader(context, _id, 1);
    }

    public static DataLoader newInstanceForAllDataFromSensor(Context context, int _id) {
        return new DataLoader(context, _id, 0);
    }
}