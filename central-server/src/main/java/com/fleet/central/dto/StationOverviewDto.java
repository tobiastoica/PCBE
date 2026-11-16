package com.fleet.central.dto;

public class StationOverviewDto {
    private String stationId;
    private String name;
    private double lat;
    private double lon;
    private int capacity;
    private long dockedCount;
    private int port;
    private String status;
    private String dockerContainerId;

    public StationOverviewDto() {}

    public StationOverviewDto(String stationId, String name, double lat, double lon, int capacity,
                              long dockedCount, int port, String status, String dockerContainerId) {
        this.stationId = stationId;
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.capacity = capacity;
        this.dockedCount = dockedCount;
        this.port = port;
        this.status = status;
        this.dockerContainerId = dockerContainerId;
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

    public long getDockedCount() { return dockedCount; }
    public void setDockedCount(long dockedCount) { this.dockedCount = dockedCount; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDockerContainerId() { return dockerContainerId; }
    public void setDockerContainerId(String dockerContainerId) { this.dockerContainerId = dockerContainerId; }
}
