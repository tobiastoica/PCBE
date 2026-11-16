package com.fleet.central.dto;

public class ScooterTransferRequest {
    private String scooterId;
    private String fromStationId;
    private String toStationId;
    private String reason;

    public ScooterTransferRequest() {}

    public ScooterTransferRequest(String scooterId, String fromStationId, String toStationId, String reason) {
        this.scooterId = scooterId;
        this.fromStationId = fromStationId;
        this.toStationId = toStationId;
        this.reason = reason;
    }

    public String getScooterId() { return scooterId; }
    public void setScooterId(String scooterId) { this.scooterId = scooterId; }

    public String getFromStationId() { return fromStationId; }
    public void setFromStationId(String fromStationId) { this.fromStationId = fromStationId; }

    public String getToStationId() { return toStationId; }
    public void setToStationId(String toStationId) { this.toStationId = toStationId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
