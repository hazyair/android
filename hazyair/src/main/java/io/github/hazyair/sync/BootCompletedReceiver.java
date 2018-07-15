package io.github.hazyair.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import timber.log.Timber;

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Timber.plant(new Timber.DebugTree());
        Log.e("@@@@@", "BOOT_COMPLETED");
        Timber.e("BOOT_COMPLETED");
        String action = intent.getAction();
        if (action == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            switch (action) {
                case android.content.Intent.ACTION_BOOT_COMPLETED:
                case android.content.Intent.ACTION_LOCKED_BOOT_COMPLETED:
                    DatabaseSyncService.schedule(context);
                    break;
            }
        } else {
            switch (action) {
                case android.content.Intent.ACTION_BOOT_COMPLETED:
                    DatabaseSyncService.schedule(context);
                    break;
            }
        }

    }
}
