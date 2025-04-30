package com.example.phase3_cs3431;

public class Business {
    private final String id;
    private final String name;
    private final String address;
    private final String city;
    private final String state;
    private final int zipCode;
    private final double latitude;
    private final double longitude;
    private final int starRating;
    private final int numTip;
    private final int isOpen;

    public Business(String id, String name, String address, String city, String state, int zipCode,
                    double latitude, double longitude, int starRating, int numTip, int isOpen) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.starRating = starRating;
        this.numTip = numTip;
        this.isOpen = isOpen;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public int getZipCode() {
        return zipCode;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getStarRating() {
        return starRating;
    }

    public int getNumTip() {
        return numTip;
    }

    public int getIsOpen() {
        return isOpen;
    }
}
