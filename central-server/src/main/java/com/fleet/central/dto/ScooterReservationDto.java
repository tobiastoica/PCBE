package com.fleet.central.dto;

public class ScooterReservationDto {

    private String scooterId;
    private String userId;
    private String reservationStatus;
    private int expiresInSeconds;
    private String stationId;
    private String stationName;
    private String stationUrl;
    private int stationPort;
    private String message;

    public ScooterReservationDto() {
    }

    public ScooterReservationDto(String scooterId, String userId, String reservationStatus, int expiresInSeconds,
                                 String stationId, String stationName, String stationUrl, int stationPort, String message) {
        this.scooterId = scooterId;
        this.userId = userId;
        this.reservationStatus = reservationStatus;
        this.expiresInSeconds = expiresInSeconds;
        this.stationId = stationId;
        this.stationName = stationName;
        this.stationUrl = stationUrl;
        this.stationPort = stationPort;
        this.message = message;
    }

    public String getScooterId() {
        return scooterId;
    }

    public void setScooterId(String scooterId) {
        this.scooterId = scooterId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getReservationStatus() {
        return reservationStatus;
    }

    public void setReservationStatus(String reservationStatus) {
        this.reservationStatus = reservationStatus;
    }

    public int getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(int expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public String getStationUrl() {
        return stationUrl;
    }

    public void setStationUrl(String stationUrl) {
        this.stationUrl = stationUrl;
    }

    public int getStationPort() {
        return stationPort;
    }

    public void setStationPort(int stationPort) {
        this.stationPort = stationPort;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
