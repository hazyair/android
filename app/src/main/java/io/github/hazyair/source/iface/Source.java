package io.github.hazyair.source.iface;

import android.content.Context;

import java.util.List;

import io.github.hazyair.source.Data;
import io.github.hazyair.source.Sensor;
import io.github.hazyair.source.Station;

public interface Source {
    String stationsUrl();
    String sensorsUrl(String id);
    String dataUrl(String id);
    List<Station> stations(String json);
    List<Sensor> sensors(String json);
    List<Data> data(String json);
}
