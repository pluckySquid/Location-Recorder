// RecordedLocation.java
package com.example.locationrecorder;

import com.google.android.gms.maps.model.LatLng;

public class RecordedLocation {
    private final LatLng latLng;
    private final float speed;
    private final long timestamp;

    public RecordedLocation(LatLng latLng, float speed, long timestamp) {
        this.latLng = latLng;
        this.speed = speed;
        this.timestamp = timestamp;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public float getSpeed() {
        return speed;
    }

    public long getTimestamp() {
        return timestamp;
    }
}