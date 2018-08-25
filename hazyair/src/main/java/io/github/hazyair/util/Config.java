package io.github.hazyair.util;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import io.github.hazyair.data.HazyairProvider;
import io.github.hazyair.source.Info;

public class Config {
    private final static String PARAM_UPDATE = "io.github.hazyair.PARAM_UPDATE";
    private final static String PREF_INFO = "io.github.hazyair.PREF_INFO";

    public static void setUpdate(Context context) {
        HazyairProvider.Config.set(context,
                PARAM_UPDATE,
                String.valueOf(new DateTime(DateTime.now(),
                        DateTimeZone.getDefault()).withZone(DateTimeZone.UTC)
                        .getMillis()));
    }

    public static long getUpdate(Context context) {
        String result = HazyairProvider.Config.get(context, PARAM_UPDATE);
        if (result == null || result.isEmpty()) result = "0";
        Log.e("!!!", ""+result.isEmpty()+" ["+result+"]");
        return Long.valueOf(result);
    }

    public static void setInfo(Context context, Info info) {
        if (context == null) return;
        if (info == null) {
            HazyairProvider.Config.set(context, PREF_INFO, "");
        } else {
            HazyairProvider.Config.set(context, PREF_INFO, new Gson().toJson(info));
        }
    }

    public static Info getInfo(Context context) {
        if (context == null) return null;
        String info = HazyairProvider.Config.get(context, PREF_INFO);
        if (info.isEmpty()) return null;
        return new Gson().fromJson(info, new TypeToken<Info>() {}.getType());
    }
}
