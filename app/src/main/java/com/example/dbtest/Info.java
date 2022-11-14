package com.example.dbtest;

public class Info {
    String name;
    Integer id;
    double latitude;
    double longitude;
    String nighttime;
    String distance;

    public Info(String name, Integer id, double latitude, double longitude, String nighttime, String distance){
        this.name = name;
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.nighttime = nighttime;
        this.distance = distance;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setDistance(String distance){
        this.distance = distance;
    }
}
