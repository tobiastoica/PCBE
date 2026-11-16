package com.fleet.central.dto;

public class ScooterNearbyDto {
    private String scooterId;
    private String model;
    private int batteryLevel;
    private String status;
    private double lat;
    private double lon;
    private String stationId;
    private String stationName;
    private String stationUrl;
    private int stationPort;
    private int distanceMeters;

    public ScooterNearbyDto() {}

    public ScooterNearbyDto(String scooterId, String model, int batteryLevel, String status, double lat, double lon,
                            String stationId, String stationName, String stationUrl, int stationPort, int distanceMeters) {
        this.scooterId = scooterId;
        this.model = model;
        this.batteryLevel = batteryLevel;
        this.status = status;
        this.lat = lat;
        this.lon = lon;
        this.stationId = stationId;
        this.stationName = stationName;
        this.stationUrl = stationUrl;
        this.stationPort = stationPort;
        this.distanceMeters = distanceMeters;
    }

    public String getScooterId() { return scooterId; }
    public void setScooterId(String scooterId) { this.scooterId = scooterId; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(int batteryLevel) { this.batteryLevel = batteryLevel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLon() { return lon; }
    public void setLon(double lon) { this.lon = lon; }

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }

    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }

    public String getStationUrl() { return stationUrl; }
    public void setStationUrl(String stationUrl) { this.stationUrl = stationUrl; }

    public int getStationPort() { return stationPort; }
    public void setStationPort(int stationPort) { this.stationPort = stationPort; }

    public int getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(int distanceMeters) { this.distanceMeters = distanceMeters; }
}
