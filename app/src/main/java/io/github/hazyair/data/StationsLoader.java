package io.github.hazyair.data;

import android.content.Context;
import android.support.v4.content.CursorLoader;
import android.net.Uri;

import io.github.hazyair.source.Station;

public class StationsLoader extends CursorLoader {

    public StationsLoader(Context context, Uri uri) {
        super(context, uri, new Station().keys(), null, null,
                HazyairProvider.Stations.DEFAULT_SORT);
    }

    public static StationsLoader newInstanceForAllStations(Context context) {
        return new StationsLoader(context, HazyairProvider.Stations.CONTENT_URI);
    }

}
