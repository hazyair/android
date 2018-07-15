package io.github.hazyair.util;

import android.content.Context;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import io.github.hazyair.source.Info;

public class Preference {
    private final static String INFO = "io.github.hazyair.INFO";

    public static void saveInfo(Context context, Info info) {
        if (info == null) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().remove(INFO).apply();
        } else {
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putString(INFO, new Gson().toJson(info)).apply();
        }
    }

    public static Info restoreInfo(Context context) {
        String info = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(INFO, "");
        if (info.isEmpty()) return null;
        return new Gson().fromJson(info, new TypeToken<Info>() {}.getType());
    }
}
