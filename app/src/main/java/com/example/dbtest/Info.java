package com.example.dbtest;

public class Info {
    String name;
    Integer id;
    double latitude;
    double longitude;
    String nighttime;
    double distance;

    public Info(String name, Integer id, double latitude, double longitude, String nighttime, double distance){
        this.name = name;
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.nighttime = nighttime;
        this.distance = distance;
    }
}
