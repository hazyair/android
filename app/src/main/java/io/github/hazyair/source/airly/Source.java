package io.github.hazyair.source.airly;

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
    private final static String URL = "https://airapi.airly.eu/v1/sensors";
    private final static String STATIONS = "current";

    private static  io.github.hazyair.source.iface.Source mInstance;
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
        return Uri.parse(URL).buildUpon().build().toString();
    }

    @Override
    public String dataUrl(String id) {
        return null;
    }

    @Override
    public List<io.github.hazyair.source.Station> stations(String data) {
        List<io.github.hazyair.source.Station> result = new ArrayList<>();
        List<io.github.hazyair.source.airly.Station> stations = new Gson().fromJson(data,
                new TypeToken<List<io.github.hazyair.source.gios.Station>>() {}.getType());
        for(io.github.hazyair.source.airly.Station station : stations) {
            result.add(new io.github.hazyair.source.Station(String.valueOf(station.id), station.name,
                    station.location.latitude, station.location.longitude,
                    station.address.country, station.address.locality,
                    station.address.route + " " + station.address.streetNumber,
                    mContext.getString(R.string.data_source_airly)));
        }
        return result;
    }

    @Override
    public List<Sensor> sensors(String json) {
        throw new NotImplementedException("sensors");
    }

    @Override
    public List<Data> data(String json) { throw new NotImplementedException("data"); }
}
