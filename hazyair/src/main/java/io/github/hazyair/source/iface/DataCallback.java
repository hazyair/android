package io.github.hazyair.source.iface;

import java.util.List;

import io.github.hazyair.source.Data;

public interface DataCallback {
    void onSuccess(List<Data> data);
    void onError();
}
