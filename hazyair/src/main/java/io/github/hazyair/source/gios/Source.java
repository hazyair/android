package io.github.hazyair.source.gios;

import android.net.Uri;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.github.hazyair.R;

public class Source implements io.github.hazyair.source.iface.Source {
    private final static String URL = "http://api.gios.gov.pl/pjp-api/rest";
    private final static String STATION = "station";
    private final static String FIND_ALL = "findAll";
    private final static String SENSORS = "sensors";
    private final static String DATA = "data";
    private final static String GET_DATA = "getData";
    private final static SimpleDateFormat format =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private static final Map<String, String> parameters = Collections.unmodifiableMap(
            new HashMap<String, String>() {{
                put("C6H6", "C₆H₆");
                put("CO", "CO");
                put("NO2", "NO₂");
                put("O3", "O₃");
                put("PM10", "PM10");
                put("PM2.5", "PM2.5");
                put("SO2", "SO₂");
            }});
    private static final String ug_m3 = "µg/m³";

    private static io.github.hazyair.source.iface.Source mInstance;

    private Source() {

    }

    public static io.github.hazyair.source.iface.Source getInstance() {
        if (mInstance == null) mInstance = new Source();
        return mInstance;
    }

    @Override
    public String stationsUrl() {
        return Uri.parse(URL).buildUpon().appendPath(STATION).appendPath(FIND_ALL).build()
                .toString();
    }

    @Override
    public String sensorsUrl(String id) {
        return Uri.parse(URL).buildUpon().appendPath(STATION).appendPath(SENSORS)
                .appendPath(id).build().toString();
    }

    @Override
    public String dataUrl(String id) {
        return Uri.parse(URL).buildUpon().appendPath(DATA).appendPath(GET_DATA).appendPath(id)
                .build().toString();
    }

    @Override
    public List<io.github.hazyair.source.Station> stations(String json) {
        List<io.github.hazyair.source.Station> result = new ArrayList<>();
        List<Station> stations;
        try {
            stations = new Gson().fromJson(json,
                    new TypeToken<List<Station>>() {}.getType());
        } catch (JsonSyntaxException e) {
            return result;
        }
        for(Station station : stations) {
            if (station.gegrLat == null || station.gegrLon == null || station.city == null)
                continue;
            result.add(new io.github.hazyair.source.Station(String.valueOf(station.id),
                    station.stationName, Double.valueOf(station.gegrLat),
                    Double.valueOf(station.gegrLon), R.string.data_poland, station.city.name,
                    station.addressStreet, R.string.data_source_gios));
        }
        return result;
    }

    @Override
    public List<io.github.hazyair.source.Sensor> sensors(String json) {
        List<io.github.hazyair.source.Sensor> result = new ArrayList<>();
        List<Sensor> sensors;
        try {
            sensors = new Gson().fromJson(json,
                    new TypeToken<List<Sensor>>() {}.getType());
        } catch (JsonSyntaxException e) {
            return result;
        }
        for (Sensor sensor : sensors) {
            result.add(new io.github.hazyair.source.Sensor(String.valueOf(sensor.id),
                    String.valueOf(sensor.stationId), parameters.get(sensor.param.paramFormula),
                    ug_m3));
        }
        return result;
    }

    @Override
    public List<io.github.hazyair.source.Data> data(String json) {
        List<io.github.hazyair.source.Data> result = new ArrayList<>();
        Data data;
        try {
            data = new Gson().fromJson(json, new TypeToken<Data>() {}.getType());
        } catch (JsonSyntaxException e) {
            return result;
        }
        long timestamp = 0;
        for (Value value : data.values) {
            if (value.value == null) continue;
            try {
                timestamp = format.parse(value.date).getTime();
            } catch (ParseException e) {
                Crashlytics.logException(e);
            }
            result.add(new io.github.hazyair.source.Data(new DateTime(timestamp,
                    DateTimeZone.forID("Poland")).withZone(DateTimeZone.forID("UTC")).getMillis(),
                    new BigDecimal(value.value).setScale(2,
                            RoundingMode.HALF_UP).doubleValue()));
        }
        return result;
    }
}
