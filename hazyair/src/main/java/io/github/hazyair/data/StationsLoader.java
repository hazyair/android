package io.github.hazyair.data;

import android.content.Context;
import android.support.v4.content.CursorLoader;

import io.github.hazyair.source.Station;

public class StationsLoader extends CursorLoader {

    private StationsLoader(Context context) {
        super(context, HazyairProvider.Stations.CONTENT_URI, Station.keys(),
                null, null, HazyairProvider.Stations.DEFAULT_SORT);
    }

    public static StationsLoader newInstanceForAllStations(Context context) {
        return new StationsLoader(context);
    }

}
