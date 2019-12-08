package io.github.hazyair.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;

import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import io.github.hazyair.R;

public final class Location {

    public static final int PERMISSION_REQUEST_FINE_LOCATION = 0;

    private static void requestFineLocationPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_REQUEST_FINE_LOCATION);
    }

    public static boolean checkPermission(Activity activity, boolean request) {
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            return true;
        if (!request) return false;
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
                            activity.getString(R.string.button_ok),
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

    public static boolean checkPermission(Activity activity) {
        return checkPermission(activity, true);
    }

    public static void requestUpdates(Context context, FusedLocationProviderClient
            fusedLocationProviderClient, LocationRequest locationRequest,
                                      LocationCallback locationCallback) {
        if (context == null || fusedLocationProviderClient == null || locationRequest == null ||
                locationCallback == null) return;
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback,
                null);
    }

    public static void removeUpdates(Context context, FusedLocationProviderClient
            fusedLocationProviderClient, LocationCallback locationCallback) {
        if (context == null || fusedLocationProviderClient == null || locationCallback == null)
            return;
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
    }

    public static LocationRequest createLocationRequest() {
        return LocationRequest.create().setInterval(TimeUnit.MINUTES.toMillis(5))
                .setFastestInterval(TimeUnit.MINUTES.toMillis(1))
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setSmallestDisplacement(100);
    }
}
