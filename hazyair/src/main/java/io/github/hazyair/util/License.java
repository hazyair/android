package io.github.hazyair.util;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AlertDialog;

import io.github.hazyair.R;
import io.github.hazyair.gui.MainActivity;

public class License {

    private static void dismiss(Activity activity) {
        Preference.setLicense(activity, false);
        if (activity instanceof MainActivity) {
            activity.finish();
        } else {
            Intent intent = new Intent(activity.getApplicationContext(),
                    MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(MainActivity.PARAM_EXIT, true);
            activity.startActivity(intent);
        }
    }

    public static void showLicense(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.title_license))
                .setMessage(activity.getString(R.string.message_license))
                .setPositiveButton(activity.getString(R.string.button_agree),
                        (dialog, which) -> Preference.setLicense(activity, true))
                .setNegativeButton(activity.getString(R.string.button_exit),
                        (dialog, which) -> dismiss(activity))
                .setCancelable(false)
                .create()
                .show();
    }
}
