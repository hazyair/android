package io.github.hazyair.source.smoksmog;

import android.content.Context;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.List;

import io.github.hazyair.R;
import io.github.hazyair.source.Data;
import io.github.hazyair.source.Sensor;

public class Source implements io.github.hazyair.source.iface.Source {

    private final static String URL = "http://api.smoksmog.jkostrz.name/api";
    private final static String STATIONS = "stations";

    private static io.github.hazyair.source.iface.Source mInstance;
    private final Context mContext;

    private Source(Context context) {
        mContext = context;
    }

    public static io.github.hazyair.source.iface.Source getInstance(Context context) {
        if (mInstance == null) mInstance = new Source(context);
        return mInstance;
    }

    @Override
    public String stationsUrl() {
        return Uri.parse(URL).buildUpon().appendPath(STATIONS).build().toString();
    }

    @Override
    public String sensorsUrl(String id) {
        return stationsUrl();
    }

    @Override
    public String dataUrl(String id) {
        return null;
    }

    @Override
    public List<io.github.hazyair.source.Station> stations(String data) {
        List<io.github.hazyair.source.Station> result = new ArrayList<>();
        List<io.github.hazyair.source.smoksmog.Station> stations = new Gson().fromJson(data,
                new TypeToken<List<Station>>() {}.getType());
        for (io.github.hazyair.source.smoksmog.Station station: stations) {
            String[] string = station.name.split(" - ");
            String locality = "";
            if (string.length > 0) locality = string[0];
            String address = "";
            if (string.length > 1) address = string[1];
            result.add(new io.github.hazyair.source.Station(String.valueOf(station.id), station.name,
                    Double.valueOf(station.latitude), Double.valueOf(station.longitude),
                    mContext.getString(R.string.data_poland), locality, address,
                    mContext.getString(R.string.data_source_smoksmog)));

        }
        return result;
    }

    @Override
    public List<Sensor> sensors(String json) {
        throw new NotImplementedException("sensors");
    }

    @Override
    public List<Data> data(String json) {
        throw new NotImplementedException("data");
    }

}
