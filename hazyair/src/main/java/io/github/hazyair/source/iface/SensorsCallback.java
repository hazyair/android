package io.github.hazyair.source.iface;

import java.util.List;

import io.github.hazyair.source.Sensor;

public interface SensorsCallback extends Worker {
    void onSuccess(List<Sensor> sensors);
    void onError();
}
