package com.dinein.dinein;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;

public class NearbyListViewAdapter extends RecyclerView.Adapter<NearbyListViewAdapter.ListItemViewHolder> {

    private ArrayList<HashMap<String,String>> nearbyPlacesList;

    public NearbyListViewAdapter(ArrayList<HashMap<String,String>> nearbyPlacesList) {
        this.nearbyPlacesList = nearbyPlacesList;
    }

    @NonNull
    @Override
    public ListItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View listItemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.nearby_list_item, parent, false);
        ListItemViewHolder listItemViewHolder = new ListItemViewHolder(listItemView);
        return listItemViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ListItemViewHolder listItemViewHolder, int position) {
            listItemViewHolder.listItemName.setText(nearbyPlacesList.get(position).get("name"));
    }

    @Override
    public int getItemCount() {
        return nearbyPlacesList.size();
    }

    public class ListItemViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout listItemLayout;
        TextView listItemName;

        public ListItemViewHolder(@NonNull View itemView) {
            super(itemView);

            listItemLayout = itemView.findViewById(R.id.list_item);
            listItemName = itemView.findViewById(R.id.list_item_name);
        }
    }
}
