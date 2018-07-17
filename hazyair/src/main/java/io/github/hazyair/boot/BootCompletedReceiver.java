package io.github.hazyair.boot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;
import io.github.hazyair.notifications.NotificationService;
import io.github.hazyair.sync.DatabaseSyncService;
import io.github.hazyair.util.Preference;

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            switch (action) {
                case android.content.Intent.ACTION_BOOT_COMPLETED:
                case android.content.Intent.ACTION_LOCKED_BOOT_COMPLETED:
                    if (Preference.isCrashlyticsEnabled(context))
                        Fabric.with(context, new Crashlytics());
                    if (Preference.startSync(context))
                        DatabaseSyncService.schedule(context);
                    if (Preference.startNotifications(context))
                        NotificationService.schedule(context);

                    break;
            }
        } else {
            switch (action) {
                case android.content.Intent.ACTION_BOOT_COMPLETED:
                    if (Preference.isCrashlyticsEnabled(context))
                        Fabric.with(context, new Crashlytics());
                    if (Preference.startSync(context))
                        DatabaseSyncService.schedule(context);
                    if (Preference.startNotifications(context))
                        NotificationService.schedule(context);
                    break;
            }
        }


    }
}
