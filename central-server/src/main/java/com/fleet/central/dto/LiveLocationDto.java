package com.fleet.central.dto;

public class LiveLocationDto {
    private String scooterId;
    private String status;
    private String managingStation;
    private double lat;
    private double lon;
    private int batteryLevel;
    private double speedKmh;
    private String lastPingTimestamp;

    public LiveLocationDto() {}

    public LiveLocationDto(String scooterId, String status, String managingStation, double lat, double lon,
                           int batteryLevel, double speedKmh, String lastPingTimestamp) {
        this.scooterId = scooterId;
        this.status = status;
        this.managingStation = managingStation;
        this.lat = lat;
        this.lon = lon;
        this.batteryLevel = batteryLevel;
        this.speedKmh = speedKmh;
        this.lastPingTimestamp = lastPingTimestamp;
    }

    public String getScooterId() { return scooterId; }
    public void setScooterId(String scooterId) { this.scooterId = scooterId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getManagingStation() { return managingStation; }
    public void setManagingStation(String managingStation) { this.managingStation = managingStation; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLon() { return lon; }
    public void setLon(double lon) { this.lon = lon; }

    public int getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(int batteryLevel) { this.batteryLevel = batteryLevel; }

    public double getSpeedKmh() { return speedKmh; }
    public void setSpeedKmh(double speedKmh) { this.speedKmh = speedKmh; }

    public String getLastPingTimestamp() { return lastPingTimestamp; }
    public void setLastPingTimestamp(String lastPingTimestamp) { this.lastPingTimestamp = lastPingTimestamp; }
}
