package com.dinein.dinein;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class NearbyListViewAdapter extends RecyclerView.Adapter<NearbyListViewAdapter.ListItemViewHolder> {
    private static final String TAG = "NearbyListViewAdapter";
    private LinkedHashMap<String, HashMap<String,String>> nearbyPlacesList;
    private ArrayList<String> positionIndexes;

    NearbyListViewAdapter(LinkedHashMap<String, HashMap<String, String>> nearbyPlacesList) {
        this.nearbyPlacesList = nearbyPlacesList;
        this.positionIndexes = new ArrayList<>();
    }

    public void updateNearbyPlacesList(LinkedHashMap<String, HashMap<String,String>> nearbyPlacesListUpdate) {
        this.nearbyPlacesList = nearbyPlacesListUpdate;
        positionIndexes.clear();
        positionIndexes.addAll(nearbyPlacesList.keySet());
    }

    @NonNull
    @Override
    public ListItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View listItemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.nearby_list_item, parent, false);
        return new ListItemViewHolder(listItemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ListItemViewHolder listItemViewHolder, int position) {
            String placeId = positionIndexes.get(position);
            listItemViewHolder.listItemName.setText(nearbyPlacesList.get(placeId).get("name"));
            listItemViewHolder.listItemAddress.setText(nearbyPlacesList.get(placeId).get("address"));
            listItemViewHolder.listItemDistance.setText(nearbyPlacesList.get(placeId).get("distance"));
            listItemViewHolder.listItemDuration.setText(nearbyPlacesList.get(placeId).get("duration"));
    }

    @Override
    public int getItemCount() {
        if (nearbyPlacesList == null) {return 0;}
        return nearbyPlacesList.size();
    }

    class ListItemViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout listItemLayout;
        TextView listItemName;
        TextView listItemAddress;
        TextView listItemDistance;
        TextView listItemDuration;

        ListItemViewHolder(@NonNull View itemView) {
            super(itemView);

            listItemLayout = itemView.findViewById(R.id.list_item);
            listItemName = itemView.findViewById(R.id.list_item_name);
            listItemAddress = itemView.findViewById(R.id.list_item_address);
            listItemDistance = itemView.findViewById(R.id.list_item_distance);
            listItemDuration = itemView.findViewById(R.id.list_item_duration);
        }
    }
}
