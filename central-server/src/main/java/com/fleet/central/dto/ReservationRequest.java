package com.fleet.central.dto;

public class ReservationRequest {

    private String userId;
    private String scooterId;

    public ReservationRequest() {
    }

    public ReservationRequest(String userId, String scooterId) {
        this.userId = userId;
        this.scooterId = scooterId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getScooterId() {
        return scooterId;
    }

    public void setScooterId(String scooterId) {
        this.scooterId = scooterId;
    }
}
