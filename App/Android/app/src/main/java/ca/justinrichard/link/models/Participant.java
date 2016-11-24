package ca.justinrichard.link.models;

import android.location.Location;

/**
 * Created by Justin on 11/1/2016.
 */

public class Participant {
    public String userId;
    public String displayName;
    public String imageUrl;
    public Double altitude;
    public Double latitude;
    public Double longitude;
    public Double lastUpdate;

    public Participant(String userId, Double lastUpdate, Double altitude, Double latitude, Double longitude) {
        this.userId = userId;
        this.altitude = altitude;
        this.latitude = latitude;
        this.longitude = longitude;
        this.lastUpdate = lastUpdate;
    }

    public String getUserId(){
        return this.userId;
    }
    public Double getAltitude(){
        return this.altitude;
    }
    public Double getLatitude(){
        return this.latitude;
    }
    public Double getLongitude(){
        return this.longitude;
    }
    public Double getLastUpdate(){
        return this.lastUpdate;
    }
    public String getDisplayName() { return this.displayName; }
    public String getImageUrl() { return this.imageUrl; }

    public void setDisplayName(String s){
        this.displayName = s;
    }
    public void setImageUrl(String s){
        this.imageUrl = s;
    }


    @Override
    public String toString(){
        return "Participant object for userId;"+userId;
    }
}
