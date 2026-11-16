package com.fleet.central.dto;

public class SpawnStationResponse {
    private String stationId;
    private String dockerContainerId;
    private String status;
    private String message;

    public SpawnStationResponse() {}

    public SpawnStationResponse(String stationId, String dockerContainerId, String status, String message) {
        this.stationId = stationId;
        this.dockerContainerId = dockerContainerId;
        this.status = status;
        this.message = message;
    }

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }

    public String getDockerContainerId() { return dockerContainerId; }
    public void setDockerContainerId(String dockerContainerId) { this.dockerContainerId = dockerContainerId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
