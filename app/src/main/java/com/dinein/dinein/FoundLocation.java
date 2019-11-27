package com.dinein.dinein;

import android.location.Location;

import androidx.annotation.NonNull;

import java.util.Observable;

class FoundLocation extends Observable {
    private Location location;

    FoundLocation() {
        this.location = null;
    }

    Location getLocation() {
        return location;
    }

    void setLocation(Location location) {
        if (!this.isSameLocationAs(location)) {
            this.location = location;
            setChanged();
            notifyObservers(this.location);
        }
    }

    private boolean isSameLocationAs(@NonNull Object location) {
        boolean equivalent = false;
        if (location instanceof Location
                && this.location != null
                && this.location.getLatitude() == ((Location) location).getLatitude()
                && this.location.getLongitude() == ((Location) location).getLongitude()) {
            equivalent = true;
        }
        return equivalent;
    }
}
