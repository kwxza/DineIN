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
    private Location currentLocation;
    private ArrayList<HashMap<String,String>> nearbyPlacesList, listUpdater;
    private RequestQueue requestQueue;

    // Constructor
    NearbyPlaces(Context currentContext) {
        this.GOOGLE_API_KEY = currentContext.getResources().getString(R.string.api_key);
        this.currentLocation = null;
        this.nearbyPlacesList = new ArrayList<>();
        this.listUpdater = new ArrayList<>();
        this.addObserver(this);
        requestQueue = Volley.newRequestQueue(currentContext);

        Log.d(TAG, "NearbyPlaces: nearbyList size @ construction: " + nearbyPlacesList.size());
        Log.d(TAG, "NearbyPlaces: listUpdate size @ construction: " + listUpdater.size());
    }

    // Method for constructing query url to get nearby restaurants
    private void createNearbyUrl(@Nullable String pageToken) {
        StringBuilder nearbyUrlBuilder = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");

        if (pageToken == null) {
            nearbyUrlBuilder
                    .append("location=")
                    .append(currentLocation.getLatitude())
                    .append(",")
                    .append(currentLocation.getLongitude())
                    .append("&rankby=distance&type=restaurant&opennow");
        } else {
            nearbyUrlBuilder.append("pagetoken=").append(pageToken);
        }
        nearbyUrlBuilder.append("&key=").append(GOOGLE_API_KEY);

        this.setNearbyUrlData(nearbyUrlBuilder.toString(), pageToken);
    }

    private String createDistanceUrl(ArrayList<HashMap<String,String>> listUpdater) {
        StringBuilder distanceUrlBuilder = new StringBuilder("https://maps.googleapis.com/maps/api/distancematrix/json?");

        distanceUrlBuilder
                .append("origins=")
                .append(currentLocation.getLatitude())
                .append(",")
                .append(currentLocation.getLongitude())
                .append("&mode=walking")
                .append("&destinations=");

        for (int i = 0; i < listUpdater.size(); i++) {
            distanceUrlBuilder
                    .append("place_id:")
                    .append(listUpdater.get(i).get("place_id"));
            if (i < listUpdater.size() - 1) {
                distanceUrlBuilder.append("|");
            }
        }

        distanceUrlBuilder
                .append("&key=")
                .append(GOOGLE_API_KEY);

        return distanceUrlBuilder.toString();
    }

    // Method that requests nearby places from Google and parses the JSON response
    private void getNearbyPlacesDataFromUrl(String[] nearbyUrlData) {
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
                                        if (results.getJSONObject(i).has("rating")) {
                                            place.put("rating", results.getJSONObject(i).getString("rating"));
                                        }

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
                                        getDistanceDataFromUrl(createDistanceUrl(listUpdater));
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

    private void getDistanceDataFromUrl(final String distanceUrl) {
        JsonObjectRequest distanceRequest = new JsonObjectRequest
                (Request.Method.GET, distanceUrl, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        try {
                            String status = response.getString("status");

                            if (status.equals("OK")) {
                                JSONArray addresses = response.getJSONArray("destination_addresses");
                                JSONArray distanceInfo = response.getJSONArray("rows").getJSONObject(0).getJSONArray("elements");

                                for (int i = 0; i < addresses.length(); i++) {
                                    if (distanceInfo.getJSONObject(i).getString("status").equals("OK")) {
                                        listUpdater.get(i).put("address", addresses.getString(i).split(",")[0]);
                                        listUpdater.get(i).put("distance", distanceInfo.getJSONObject(i).getJSONObject("distance").getString("text"));
                                        listUpdater.get(i).put("duration", distanceInfo.getJSONObject(i).getJSONObject("duration").getString("text"));
                                    }
                                }
                                updateNearbyPlacesList(listUpdater);
                            } else {
                                // TODO: Handle request error
                                Log.d(TAG, "onResponse: Something went wrong.");
                                Log.d(TAG, "onResponse: Status: " + response.getString("status"));
                                if (response.has("error_message")) {
                                    Log.d(TAG, "onResponse: errorMessage: " + response.get("error_message"));
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "onErrorResponse: Something went wrong with the distance request");
                        error.printStackTrace();
                    }
                });

        requestQueue.add(distanceRequest);
    }

    // Method that sets nearby url data and notifies observers of changes
    private void setNearbyUrlData(String newUrl, @Nullable String pageToken) {
        String[] nearbyUrlData = new String[] {newUrl, pageToken};
        setChanged();
        notifyObservers(nearbyUrlData);
    }

    private void updateNearbyPlacesList(ArrayList<HashMap<String,String>> listUpdater) {
        boolean newListIsDifferent = false;
        
        if (nearbyPlacesList.size() == listUpdater.size()) {
            int index = 0;
            while (!newListIsDifferent && index < nearbyPlacesList.size()) {
                newListIsDifferent = !nearbyPlacesList.get(index).get("place_id").equals(listUpdater.get(index).get("place_id"));
                if (newListIsDifferent) {
                    Log.d(TAG, "updateNearbyPlacesList: newListIsDifferent @ index: " + index);
                    Log.d(TAG, "updateNearbyPlacesList: nearbyPlace @ index: " + nearbyPlacesList.get(index).get("name"));
                    Log.d(TAG, "updateNearbyPlacesList: listUpdate @ index: " + listUpdater.get(index).get("name"));
                    Log.d(TAG, "updateNearbyPlacesList: nearbyList: " + nearbyPlacesList.toString());
                    Log.d(TAG, "updateNearbyPlacesList: listUpdate: " + listUpdater.toString());
                }
                index++;
            }
        } else { newListIsDifferent = true; }
        
        if (newListIsDifferent || nearbyPlacesList.size() == 0) {
            Log.d(TAG, "updateNearbyPlacesList: nearbyList size: " + nearbyPlacesList.size());
            Log.d(TAG, "updateNearbyPlacesList: listUpdate size: " + listUpdater.size());
            nearbyPlacesList.clear();
            nearbyPlacesList.addAll(listUpdater);
            setChanged();
            notifyObservers(nearbyPlacesList);
            Log.d(TAG, "updateNearbyPlacesList: list updated");
        }
    }

    ArrayList<HashMap<String,String>> getNearbyPlacesList() {
        return nearbyPlacesList;
    }

    @Override
    public void update(Observable observedObject, Object newData) {
        // If new data is location data, update currentLocation
        // to match and create a new nearbyUrl
        if (newData instanceof Location) {
            currentLocation = (Location) newData;
            createNearbyUrl(null);
        }

        // If new data is a new nearbyUrl string,
        // make a new HTTP request to Google's API
        if (newData instanceof String[]) {
            String[] nearbyUrlData = (String[]) newData;
            getNearbyPlacesDataFromUrl(nearbyUrlData);
        }
    }
}
