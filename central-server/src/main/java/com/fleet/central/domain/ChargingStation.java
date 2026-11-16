package com.fleet.central.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "charging_stations")
public class ChargingStation {

    @Id
    @Column(name = "station_id", length = 32)
    private String stationId;

    @Column(name = "name", length = 64, nullable = false)
    private String name;

    @Column(name = "lat", nullable = false)
    private double lat;

    @Column(name = "lon", nullable = false)
    private double lon;

    @Column(name = "capacity", nullable = false)
    private int capacity = 15;

    @Column(name = "port", nullable = false)
    private int port = 8084;

    @Column(name = "docker_container_id", length = 64)
    private String dockerContainerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 24, nullable = false)
    private StationStatus status = StationStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ChargingStation() {}

    public ChargingStation(String stationId, String name, double lat, double lon, int capacity, int port, String dockerContainerId, StationStatus status) {
        this.stationId = stationId;
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.capacity = capacity;
        this.port = port;
        this.dockerContainerId = dockerContainerId;
        this.status = status != null ? status : StationStatus.ACTIVE;
        this.createdAt = Instant.now();
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

    public String getDockerContainerId() { return dockerContainerId; }
    public void setDockerContainerId(String dockerContainerId) { this.dockerContainerId = dockerContainerId; }

    public StationStatus getStatus() { return status; }
    public void setStatus(StationStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
