package io.github.hazyair.source.iface;

import java.util.List;

import io.github.hazyair.source.Sensor;

public interface SensorsCallback {
    void onSuccess(List<Sensor> sensors);
    void onError();
}
