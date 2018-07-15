package io.github.hazyair.source.iface;

import java.util.List;

import io.github.hazyair.source.Station;

public interface StationsCallback {
    void onSuccess(List<Station> stations);
    void onError();
}
