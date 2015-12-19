package com.exsoft.weatharium.model;

import java.util.Comparator;

/**
 * Created by M.A. on 03.12.2015.
 */
public class City implements Comparable<City>{
    private int id;
    private String name, country, lon, lat;

    public City() {}

    public City(int id, String name, String country, String lon, String lat) {
        this.id = id;
        this.name = name;
        this.country = country;
        this.lon = lon;
        this.lat = lat;
    }

    public Integer ID() {
        return  id;
    }

    public String Name() {
        return name;
    }

    public String Country() {
        return country;
    }

    public String Longitude() {
        return lon;
    }

    public String Latitude() {
        return lat;
    }

    @Override
    public String toString() {
        return name + ", " + country;
    }

    @Override
    public int compareTo(City another) {
        // Сортировка по стране RU, BY, UA, UK, FR, BR, etc
        return this.Country().compareTo(another.Country());
    }



    private class CustomComparator implements Comparator<City> {
        @Override
        public int compare(City o1, City o2) {
            // Сортировка по стране RU, BY, UA, UK, FR, BR, etc
            return o1.Country().compareTo(o2.Country());
        }
    }

}
