package com.example.locationrecorder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.locationrecorder.databinding.ActivityMainBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private List<RecordedLocation> recordedLocations;
    private Polyline polyline;

    private static final int COLOR_GREEN = Color.argb(255, 0, 255, 0);
    private static final int COLOR_ORANGE = Color.argb(255, 255, 165, 0);
    private static final int COLOR_RED = Color.argb(255, 255, 0, 0);
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseApp.initializeApp(this);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAnchorView(R.id.fab)
                .setAction("Action", null).show());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        recordedLocations = new ArrayList<>();

        locationCallback = createLocationCallback(db);

        if (checkLocationPermission()) {
            startLocationUpdates();
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fetchPreviousLocations(db);
    }

    private void fetchPreviousLocations(FirebaseFirestore db) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDateStr = sdf.format(new Date());
        long startOfDay = 0;
        long endOfDay = 0;
        try {
            Date currentDate = sdf.parse(currentDateStr);
            if (currentDate != null) {
                startOfDay = currentDate.getTime();
                endOfDay = startOfDay + (24 * 60 * 60 * 1000) - 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        db.collection("locations")
                .whereGreaterThanOrEqualTo("timestamp", new Date(startOfDay))
                .whereLessThanOrEqualTo("timestamp", new Date(endOfDay))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            double latitude = document.getDouble("latitude");
                            double longitude = document.getDouble("longitude");
                            double speed = document.getDouble("speed");
                            Object timestampObj = document.get("timestamp");
                            long timestamp = 0;
                            if (timestampObj instanceof Number) {
                                timestamp = ((Number) timestampObj).longValue();
                            }
                            recordedLocations.add(new RecordedLocation(new LatLng(latitude, longitude), (float) speed, timestamp));
                        }
                        if (mMap != null) {
                            drawPreviousTraces();
                        }
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                    }
                });
    }

    private LocationCallback createLocationCallback(FirebaseFirestore db) {
        return new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    processLocationUpdate(location, db);
                }
            }
        };
    }

    private void processLocationUpdate(Location location, FirebaseFirestore db) {
        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
        float speed = location.getSpeed();
        long timestamp = location.getTime();

        recordedLocations.add(new RecordedLocation(currentLocation, speed, location.getTime()));

        int color = getColorForSpeed(speed);

        updatePolyline();

        storeLocationInFirestore(location, speed, db);
    }

    private int getColorForSpeed(float speed) {
        if (speed < 2.0) {
            return COLOR_GREEN;
        } else if (speed < 5.0) {
            return COLOR_ORANGE;
        } else {
            return COLOR_RED;
        }
    }

    private void updatePolyline() {
        if (polyline == null) {
            return;
        }

        List<LatLng> updatedPath = new ArrayList<>(polyline.getPoints());

        if (recordedLocations.size() >= 2) {
            RecordedLocation startLocation = recordedLocations.get(recordedLocations.size() - 2);
            RecordedLocation endLocation = recordedLocations.get(recordedLocations.size() - 1);

            int color = getColorForSpeed(startLocation.getSpeed());
            addSegmentToPath(updatedPath, startLocation.getLatLng(), endLocation.getLatLng(), color);
        }

        polyline.setPoints(updatedPath);
    }

    private void addSegmentToPath(List<LatLng> path, LatLng start, LatLng end, int color) {
        PolylineOptions segment = new PolylineOptions()
                .add(start, end)
                .color(color);
        mMap.addPolyline(segment);
        path.add(start);
        path.add(end);
    }

    private void storeLocationInFirestore(Location location, float speed, FirebaseFirestore db) {
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", location.getLatitude());
        locationData.put("longitude", location.getLongitude());
        locationData.put("timestamp", new Date());
        locationData.put("speed", speed);

        db.collection("locations")
                .add(locationData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Location added with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding location", e);
                });
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    startLocationUpdates();
                }
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkLocationPermission()) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                    .setWaitForAccurateLocation(false)
                    .setMinUpdateIntervalMillis(5000)
                    .setMaxUpdateDelayMillis(15000)
                    .build();

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
        drawPreviousTraces();
    }

    private void drawPreviousTraces() {
        if (recordedLocations.size() < 2) {
            return; // Not enough points to draw a line
        }

        if (polyline != null) {
            polyline.remove(); // Remove the old polyline
        }

        for (int i = 0; i < recordedLocations.size() - 1; i++) {
            RecordedLocation startLocation = recordedLocations.get(i);
            RecordedLocation endLocation = recordedLocations.get(i + 1);

            // Calculate the color based on the speed at the start location of the segment
            int color = getColorForSpeed(startLocation.getSpeed());

            // Add a polyline segment between the start and end locations with the calculated color
            PolylineOptions polylineOptions = new PolylineOptions()
                    .add(startLocation.getLatLng(), endLocation.getLatLng())
                    .color(color);
            mMap.addPolyline(polylineOptions); // Add each polyline segment separately
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(recordedLocations.get(0).getLatLng(), 15));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }
}
