package io.github.hazyair.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AlertDialog;

import io.github.hazyair.R;

public final class Network {
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if (connectivityManager != null) networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    public static void showWarning(Context context) {
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.title_network))
                .setMessage(context.getString(R.string.message_network))
                .setPositiveButton(
                        context.getString(R.string.button_ok),
                        null)
                .create()
                .show();
    }
}
