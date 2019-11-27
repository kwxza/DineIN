package com.dinein.dinein;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

public class NearbyPlaces extends Observable implements Observer {
    private static final String TAG = "NearbyPlaces";
    private String GOOGLE_API_KEY;
    private Context context;
    private Location currentLocation;
    private ArrayList<HashMap<String,String>> nearbyPlacesList, listUpdater;

    // Constructor
    NearbyPlaces(Context currentContext) {
        this.context = currentContext;
        this.GOOGLE_API_KEY = context.getResources().getString(R.string.api_key);
        this.currentLocation = null;
        this.nearbyPlacesList = new ArrayList<>();
        this.listUpdater = new ArrayList<>();
        this.addObserver(this);
    }

    // Method for constructing query url to get nearby restaurants
    private void createNearbyUrl(@Nullable String pageToken) {
        StringBuilder urlBuilder = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");

        if (pageToken == null) {
            urlBuilder
                    .append("location=")
                    .append(currentLocation.getLatitude())
                    .append(",")
                    .append(currentLocation.getLongitude())
                    .append("&radius=1000&type=restaurant&opennow");
        } else {
            urlBuilder.append("&pagetoken=").append(pageToken);
        }
        urlBuilder.append("&key=").append(GOOGLE_API_KEY);

        this.setNearbyUrlData(urlBuilder.toString(), pageToken);
    }

    // Method that sets nearby url data and notifies observers of changes
    private void setNearbyUrlData(String newUrl, @Nullable String pageToken) {
        String[] nearbyUrlData = new String[] {newUrl, pageToken};
        setChanged();
        notifyObservers(nearbyUrlData);
    }

    // Method that requests nearby places from Google and parses the JSON response
    private void getNearbyPlacesFromUrl(String[] nearbyUrlData) {
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        final String nearbyUrl = nearbyUrlData[0];
        final String previousPageToken = nearbyUrlData[1];

        JsonObjectRequest nearbyPlacesRequest = new JsonObjectRequest
                (Request.Method.GET, nearbyUrl, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        try {
                            String status = response.getString("status");

                            switch (status) {
                                case "OK":
                                    final String nextPageToken =
                                            response.has("next_page_token") ?
                                                    response.getString("next_page_token") : null;

                                    JSONArray results = response.getJSONArray("results");
                                    if (previousPageToken == null) {
                                        listUpdater.clear();
                                    }

                                    for (int i = 0; i < results.length(); i++) {
                                        HashMap<String,String> place = new HashMap<>();

                                        place.put("name", results.getJSONObject(i).getString("name"));
                                        place.put("place_id", results.getJSONObject(i).getString("place_id"));
                                        place.put("lat", results.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").getString("lat"));
                                        place.put("lng", results.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").getString("lng"));

                                        listUpdater.add(place);
                                    }

                                    if (nextPageToken != null) {
                                        // Delay requesting the next page of results
                                        // in order to avoid INVALID_REQUEST error
                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                createNearbyUrl(nextPageToken);
                                            }
                                        }, 2000);
                                    } else {
                                        updateNearbyPlacesList(listUpdater);
                                    }
                                    break;

                                case "ZERO_RESULTS":
                                    // TODO: Do something for the case of no results
                                    Log.d(TAG, "onResponse: No restaurants are open right now in this area!!");
                                    break;

                                case "INVALID_REQUEST":
                                    // TODO: Handle INVALID_REQUEST error
                                    Log.d(TAG, "onResponse: Failed URL: " + nearbyUrl);
                                    Log.d(TAG, "onResponse: previousPageToken: " + previousPageToken);
                                    if (response.has("error_message")) {
                                        Log.d(TAG, "onResponse: errorMessage: " + response.get("error_message"));
                                    }
                                    break;

                                default:
                                    // TODO: Handle other type of error
                                    Log.d(TAG, "onResponse: Something went wrong.");
                                    Log.d(TAG, "onResponse: Status: " + response.getString("status"));
                                    if (response.has("error_message")) {
                                        Log.d(TAG, "onResponse: errorMessage: " + response.get("error_message"));
                                    }
                                    break;
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "onErrorResponse: An error occurred while requesting nearby place data");
                        error.printStackTrace();
                    }
                });

        requestQueue.add(nearbyPlacesRequest);
    }

    private void updateNearbyPlacesList(ArrayList<HashMap<String,String>> listUpdater) {
        nearbyPlacesList = listUpdater;
        setChanged();
        notifyObservers(nearbyPlacesList);
    }

    ArrayList<HashMap<String,String>> getNearbyPlacesList() {
        return nearbyPlacesList;
    }

    @Override
    public void update(Observable observedObject, Object newData) {
        // If new data is location data, update currentLocation
        // to match and create a new nearbyUrl
        if (newData instanceof Location) {
            Log.d(TAG, "update: new location data incoming");
            currentLocation = (Location) newData;
            createNearbyUrl(null);
        }

        // If new data is a new nearbyUrl string,
        // make a new HTTP request to Google's API
        if (newData instanceof String[]) {
            Log.d(TAG, "update: new nearby URL data incoming:");
            String[] nearbyUrlData = (String[]) newData;
            getNearbyPlacesFromUrl(nearbyUrlData);
        }
    }
}
