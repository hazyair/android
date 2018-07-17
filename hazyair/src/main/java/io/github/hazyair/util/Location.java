package io.github.hazyair.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

import java.util.concurrent.TimeUnit;

import io.github.hazyair.R;

public final class Location {

    public static final int PERMISSION_REQUEST_FINE_LOCATION = 0;

    private static void requestFineLocationPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_REQUEST_FINE_LOCATION);
    }

    public static boolean checkPermission(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            return true;

        // Should we show an explanation?
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.ACCESS_FINE_LOCATION)) {

            // Show an explanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.
            new AlertDialog.Builder(activity)
                    .setTitle(activity.getString(R.string.title_location))
                    .setMessage(activity.getString(R.string.message_location))
                    .setPositiveButton(
                            activity.getString(R.string.button_location),
                            (dialogInterface, i) -> {
                                //Prompt the user once explanation has been shown
                                requestFineLocationPermission(activity);
                            })
                    .create()
                    .show();


        } else {
            // No explanation needed, we can request the permission.
            requestFineLocationPermission(activity);
        }
        return false;
    }

    public static void requestUpdates(Context context, LocationManager locationManager,
                                      LocationListener locationListener) {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;
        locationManager.requestLocationUpdates(locationManager.getBestProvider(new Criteria(),
                false), TimeUnit.MINUTES.toMillis(15), 1000,
                locationListener);
    }

    public static void removeUpdates(Context context, LocationManager locationManager,
                                     LocationListener locationListener) {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            locationManager.removeUpdates(locationListener);
        }
    }


}
