package com.example.locationrecorder;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.location.Location;

public class LocationRecorder {

    private static final String TAG = "LocationRecorder";

    private LocationManager locationManager;
    private LocationListener locationListener;

    public LocationRecorder(Context context) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new MyLocationListener();
    }

    public void startRecording() {
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            Log.d(TAG, "Location updates started.");
        } catch (SecurityException e) {
            Log.e(TAG, "Error requesting location updates: " + e.getMessage());
        }
    }

    public void stopRecording() {
        locationManager.removeUpdates(locationListener);
        Log.d(TAG, "Location updates stopped.");
    }

    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            float accuracy = location.getAccuracy();
            // Do something with the location data, e.g., save it to a database.
            Log.d(TAG, "Location changed: " + latitude + ", " + longitude + " Accuracy: " + accuracy);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(TAG, "Provider enabled: " + provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(TAG, "Provider disabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(TAG, "Provider status changed: " + provider + " Status: " + status);
        }
    }
}
