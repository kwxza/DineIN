package com.dinein.dinein;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import java.util.HashMap;

public class NearbyListViewAdapter extends RecyclerView.Adapter<NearbyListViewAdapter.ListItemViewHolder> {

    private ArrayList<HashMap<String,String>> nearbyPlacesList;

    NearbyListViewAdapter(@Nullable ArrayList<HashMap<String, String>> nearbyPlacesList) {
        this.nearbyPlacesList = nearbyPlacesList;
    }

    public boolean updateNearbyList(ArrayList<HashMap<String,String>> nearbyPlacesListUpdate) {
        boolean changed = true;
        if (this.nearbyPlacesList != null) {

        }

        return changed;
    }

    @NonNull
    @Override
    public ListItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View listItemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.nearby_list_item, parent, false);
        return new ListItemViewHolder(listItemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ListItemViewHolder listItemViewHolder, int position) {
            listItemViewHolder.listItemName.setText(nearbyPlacesList.get(position).get("name"));
    }

    @Override
    public int getItemCount() {
        if (nearbyPlacesList == null) {return 0;}
        return nearbyPlacesList.size();
    }

    class ListItemViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout listItemLayout;
        TextView listItemName;

        ListItemViewHolder(@NonNull View itemView) {
            super(itemView);

            listItemLayout = itemView.findViewById(R.id.list_item);
            listItemName = itemView.findViewById(R.id.list_item_name);
        }
    }
}
