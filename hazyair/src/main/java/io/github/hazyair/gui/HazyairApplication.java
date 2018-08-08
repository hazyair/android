package io.github.hazyair.gui;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

import io.github.hazyair.BuildConfig;

@SuppressWarnings("WeakerAccess")
public class HazyairApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            if (LeakCanary.isInAnalyzerProcess(this)) {
                // This process is dedicated to LeakCanary for heap analysis.
                // You should not init your app in this process.
                return;
            }
            LeakCanary.install(this);
        }
    }
}
