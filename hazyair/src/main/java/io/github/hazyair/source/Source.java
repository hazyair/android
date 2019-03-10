package io.github.hazyair.source;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.hazyair.source.iface.DataCallback;
import io.github.hazyair.source.iface.SensorsCallback;
import io.github.hazyair.source.iface.StationsCallback;

public class Source {

    public enum Type {
        GIOS/*,
        AIRLY,
        SMOKSMOG*/
    }

    private final Map<Type, io.github.hazyair.source.iface.Source> mSources;
    private static RequestQueue mRequestQueue;
    private Type mType;
    private Station mStation;
    private Sensor mSensor;

    private Source(Context context) {
        mSources = new HashMap<>();
        mSources.put(Type.GIOS, io.github.hazyair.source.gios.Source.getInstance());
        //mSources.put(Type.AIRLY, io.github.hazyair.source.airly.Source.getInstance(context));
        //mSources.put(Type.SMOKSMOG, io.github.hazyair.source.smoksmog.Source.getInstance(context));
        mRequestQueue = Volley.newRequestQueue(context);
    }

    public static Source with(Context context) {
        return new Source(context);
    }


    public Source load(Type type) {
        mType = type;
        return this;
    }

    public void into(StationsCallback callback) {
        if (callback == null) return;
        io.github.hazyair.source.iface.Source source = mSources.get(mType);
        if (source == null) return;
        mRequestQueue.add(new StringRequest(Request.Method.GET, source.stationsUrl(),
                (response) -> callback.onSuccess(source.stations(response)),
                (error -> callback.onError())));
    }

    public Source from(Station station) {
        mStation = station;
        return this;
    }

    public void into(SensorsCallback callback) {
        if (callback == null) return;
        if (mStation == null) {
            callback.onError();
            return;
        }
        io.github.hazyair.source.iface.Source source = mSources.get(mType);
        if (source == null) {
            callback.onError();
            return;
        }
        mRequestQueue.add(new StringRequest(Request.Method.GET, source.sensorsUrl(mStation.id),
                (response) -> {
                    List<Sensor> sensors = source.sensors(response);
                    for (Sensor sensor : sensors) {
                        sensor._station_id = mStation._id;
                    }
                    callback.onSuccess(sensors);
                }, (error -> callback.onError())));
    }

    public Source from(Sensor sensor) {
        mSensor = sensor;
        return this;
    }

    public void into(DataCallback callback) {
        if (callback == null) return;
        if (mSensor == null) {
            callback.onError();
            return;
        }
        io.github.hazyair.source.iface.Source source = mSources.get(mType);
        if (source == null) {
            callback.onError();
            return;
        }
        mRequestQueue.add(new StringRequest(Request.Method.GET, source.dataUrl(mSensor.id),
                (response) -> {
                    List<Data> data = source.data(response);
                    for (Data entry : data) {
                        entry._station_id = mSensor._station_id;
                        entry._sensor_id = mSensor._id;
                    }
                    callback.onSuccess(data);
                }, (error -> callback.onError())));
    }
}
