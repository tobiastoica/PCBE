package com.fleet.central.dto;

public class ActiveScooterDto {
    private String scooterId;
    private String model;
    private String status;
    private int batteryLevel;
    private String activeRideId;
    private String userId;
    private String managingStationId;
    private String managingStationUrl;
    private double currentLat;
    private double currentLon;
    private String startTime;

    public ActiveScooterDto() {}

    public ActiveScooterDto(String scooterId, String model, String status, int batteryLevel, String activeRideId,
                            String userId, String managingStationId, String managingStationUrl, double currentLat,
                            double currentLon, String startTime) {
        this.scooterId = scooterId;
        this.model = model;
        this.status = status;
        this.batteryLevel = batteryLevel;
        this.activeRideId = activeRideId;
        this.userId = userId;
        this.managingStationId = managingStationId;
        this.managingStationUrl = managingStationUrl;
        this.currentLat = currentLat;
        this.currentLon = currentLon;
        this.startTime = startTime;
    }

    public String getScooterId() { return scooterId; }
    public void setScooterId(String scooterId) { this.scooterId = scooterId; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(int batteryLevel) { this.batteryLevel = batteryLevel; }

    public String getActiveRideId() { return activeRideId; }
    public void setActiveRideId(String activeRideId) { this.activeRideId = activeRideId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getManagingStationId() { return managingStationId; }
    public void setManagingStationId(String managingStationId) { this.managingStationId = managingStationId; }

    public String getManagingStationUrl() { return managingStationUrl; }
    public void setManagingStationUrl(String managingStationUrl) { this.managingStationUrl = managingStationUrl; }

    public double getCurrentLat() { return currentLat; }
    public void setCurrentLat(double currentLat) { this.currentLat = currentLat; }

    public double getCurrentLon() { return currentLon; }
    public void setCurrentLon(double currentLon) { this.currentLon = currentLon; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
}
