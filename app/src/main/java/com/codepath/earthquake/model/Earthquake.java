package com.codepath.earthquake.model;

import com.google.gson.Gson;

import java.util.ArrayList;

public class Earthquake {
    public static final Gson GSON = new Gson();
    public Metadata metadata;
    public static class Metadata {
        public long generated;
        public String title;
        public int status;
        public String api;
        public int count;
    }

    public ArrayList<Feature> features;
    public static class Feature {
        public String type;
        public String id;
        public Properties properties;
        public static class Properties {
            public String type;
            public double mag;
            public String title;
            public String place;
            public long time;
            public String url;
            public String detail;
        }
        public Geometry geometry;
        public static class Geometry {
            public String type;
            public ArrayList<Double> coordinates;
        }
    }
}
