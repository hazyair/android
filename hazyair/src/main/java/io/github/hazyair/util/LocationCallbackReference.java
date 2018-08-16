package io.github.hazyair.util;

import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;

import java.lang.ref.SoftReference;

public class LocationCallbackReference extends LocationCallback {

    private final SoftReference<LocationCallback> mLocationCallbackRef;

    public LocationCallbackReference(LocationCallback locationCallback) {
        mLocationCallbackRef = new SoftReference<>(locationCallback);
    }

    @Override
    public void onLocationResult(LocationResult locationResult) {
        super.onLocationResult(locationResult);
        if (mLocationCallbackRef.get() != null) {
            mLocationCallbackRef.get().onLocationResult(locationResult);
        }
    }

    @Override
    public void onLocationAvailability(LocationAvailability locationAvailability) {
        super.onLocationAvailability(locationAvailability);
        if (mLocationCallbackRef.get() != null) {
            mLocationCallbackRef.get().onLocationAvailability(locationAvailability);
        }
    }
}
