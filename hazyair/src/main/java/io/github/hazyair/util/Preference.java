package io.github.hazyair.util;

import android.content.Context;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import io.github.hazyair.R;
import io.github.hazyair.source.Info;

public class Preference {
    private static final String SYNC_INTERVAL = "15";
    private static final String NOTIFICATIONS_INTERVAL = "60";
    private final static String PREF_INFO = "io.github.hazyair.PREF_INFO";

    public static void initialize(Context context) {
        PreferenceManager.setDefaultValues(context, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(context, R.xml.pref_notification, false);
        PreferenceManager.setDefaultValues(context, R.xml.pref_data_sync, false);
    }

    public static void putInfo(Context context, Info info) {
        if (info == null) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().remove(PREF_INFO).apply();
        } else {
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putString(PREF_INFO, new Gson().toJson(info)).apply();
        }
    }

    public static Info getInfo(Context context) {
        String info = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_INFO, "");
        if (info.isEmpty()) return null;
        return new Gson().fromJson(info, new TypeToken<Info>() {}.getType());
    }

    public static boolean isCrashlyticsEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.pref_key_crashlytics), false);
    }

    public static boolean startSync(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.pref_key_sync_on_boot), true);
    }

    public static int getSyncFrequency(Context context) {
        return Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_key_sync_frequency), SYNC_INTERVAL));
    }

    public static boolean startNotifications(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.pref_key_notifications_on_boot),
                        true);
    }

    public static int getNotificationsFrequency(Context context) {
        return Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_key_notifications_frequency),
                        NOTIFICATIONS_INTERVAL));
    }

}
