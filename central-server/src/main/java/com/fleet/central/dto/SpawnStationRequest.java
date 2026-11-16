package com.fleet.central.dto;

public class SpawnStationRequest {
    private String stationId;
    private String name;
    private double lat;
    private double lon;
    private int capacity = 15;
    private int port = 8086;

    public SpawnStationRequest() {}

    public SpawnStationRequest(String stationId, String name, double lat, double lon, int capacity, int port) {
        this.stationId = stationId;
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.capacity = capacity;
        this.port = port;
    }

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLon() { return lon; }
    public void setLon(double lon) { this.lon = lon; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
}
