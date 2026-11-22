package com.example.assignment10;

import java.io.Serializable;

public class GasStation implements Serializable {
    private String name;
    private double latitude;
    private double longitude;
    private String locationType; // "START", "END", "MID"

    public GasStation() {}

    public GasStation(String name, double latitude, double longitude, String locationType) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationType = locationType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }
}
