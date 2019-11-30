package com.dinein.dinein;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, Observer {

    // Constants
    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSIONS_REQUEST = 0x1;
    private static final int LOCATION_SETTINGS_REQUEST = 0x2;
    private static final float ZOOM_FACTOR = 17.3f;

    // Variables
    private boolean hasLocationSettings = true;
    private FoundLocation foundLocation;
    private LocationRequest locationRequest;
    private GoogleMap map;
    private RecyclerView nearbyListView;
    private NearbyListViewAdapter nearbyListViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Instantiate map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        /*
        Instantiate nearbyPlaces && foundLocation objects and
        add observers to respond to changes in data.
        The nearbyPlaces object updates the places found in
        response to changes in location.
        The mainActivity updates the displayed list
        in response to new data about the places nearby.
        */
        NearbyPlaces nearbyPlaces = new NearbyPlaces(this);
        nearbyPlaces.addObserver(this);
        foundLocation = new FoundLocation();
        foundLocation.addObserver(nearbyPlaces);

        // Check & request location permissions and settings
        checkLocationSettings();

        // Set up recyclerView of nearbyPlaces
        nearbyListView = findViewById(R.id.nearby_list_view);
        nearbyListViewAdapter = new NearbyListViewAdapter(null);
        nearbyListView.setAdapter(nearbyListViewAdapter);
        nearbyListView.setLayoutManager(new LinearLayoutManager(this));
    }




    private void checkLocationSettings() {
        // Check if location permissions are enabled
        if (ActivityCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "checkLocationSettings: location permissions not granted, requesting");
            ActivityCompat
                    .requestPermissions(
                            this,
                            new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_PERMISSIONS_REQUEST
                    );
        } else {
            // Check if location settings are enabled
            Log.d(TAG, "checkLocationSettings: location permissions ok, checking if settings enabled");
            locationRequest = LocationRequest.create()
                    .setInterval(30000)
                    .setFastestInterval(30000)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            LocationSettingsRequest.Builder locationSettingsRequest = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
            SettingsClient locationSettingsClient = LocationServices.getSettingsClient(this);
            Task<LocationSettingsResponse> locationSettingsTask = locationSettingsClient.checkLocationSettings(locationSettingsRequest.build());

            locationSettingsTask
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            if (exception instanceof ResolvableApiException) {
                                try {
                                    Log.d(TAG, "onFailure: location settings not yet enabled, attempting to resolve");
                                    ResolvableApiException resolvableException = (ResolvableApiException) exception;
                                    resolvableException.startResolutionForResult(MainActivity.this,LOCATION_SETTINGS_REQUEST);
                                } catch (IntentSender.SendIntentException sendEx) {
                                    hasLocationSettings = false;
                                }
                            }
                        }
                    })
                    .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                        @Override
                        public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                            Log.d(TAG, "onSuccess: location settings enabled, getting location");
                            getDeviceLocation();
                        }
                    });
        }
    }

    private void getDeviceLocation() {
        FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(this);
        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.d(TAG, "onLocationResult: location result is null");
//                    return;
                } else {
                    foundLocation.setLocation(locationResult.getLastLocation());
                    updateMapView(foundLocation.getLocation());
                }
            }
        };
        locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void getInputLocation() {

    }

    private void updateNearbyListView(ArrayList<HashMap<String,String>> nearbyPlacesList) {
        nearbyListViewAdapter.notifyDataSetChanged();
    }

    private void updateMapView(Location location) {
        // Set the blue location tracker dot for Google maps
        // (can only be done if permissions are already granted)
        if (hasLocationSettings) {
            map.setMyLocationEnabled(true);
        }
        LatLng locationCoordinates = new LatLng(location.getLatitude(), location.getLongitude());
//        map.animateCamera(CameraUpdateFactory.newLatLngZoom(locationCoordinates, ZOOM_FACTOR));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(locationCoordinates, ZOOM_FACTOR));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSIONS_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onRequestPermissionsResult: location permissions now granted, checking settings");
                    checkLocationSettings();
                } else if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Log.d(TAG, "onRequestPermissionsResult: location permissions not yet granted, creating dialog");
                    AlertDialog.Builder permissionsDialog = new AlertDialog.Builder(MainActivity.this);
                    permissionsDialog
                            .setMessage(R.string.permissions_dialog)
                            .setCancelable(true)
                            .setPositiveButton("Yes, let's use my device location", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    checkLocationSettings();
                                }
                            })
                            .setNegativeButton("No, I'll enter a location", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.d(TAG, "onClick: user clicked to cancel");
                                    hasLocationSettings = false;
                                }
                            })
                            .show();
                } else {
                    Log.d(TAG, "onRequestPermissionsResult: permissions permanently denied");
                    hasLocationSettings = false;
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case LOCATION_SETTINGS_REQUEST:
                if (resultCode == RESULT_OK) {
                    getDeviceLocation();
                } else {
                    hasLocationSettings = false;
                }
                break;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setMyLocationButtonEnabled(false);
    }

    @Override
    public void update(Observable observedObject, Object newData) {
        if (newData instanceof ArrayList) {
            ArrayList<HashMap<String,String>> updateList = (ArrayList<HashMap<String,String>>) newData;
            updateNearbyListView(updateList);
        }
    }
}
