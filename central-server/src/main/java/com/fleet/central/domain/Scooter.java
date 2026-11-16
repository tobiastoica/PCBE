package com.fleet.central.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "scooters")
public class Scooter {

    @Id
    @Column(name = "scooter_id", length = 32)
    private String scooterId;

    @Column(name = "model", length = 32, nullable = false)
    private String model;

    @Column(name = "battery_level", nullable = false)
    private int batteryLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", length = 24, nullable = false)
    private ScooterStatus currentStatus = ScooterStatus.AVAILABLE;

    @Column(name = "current_lat", nullable = false)
    private double currentLat;

    @Column(name = "current_lon", nullable = false)
    private double currentLon;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_station_id")
    private ChargingStation assignedStation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_zone_id")
    private Zone assignedZone;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "last_maintenance")
    private Instant lastMaintenance;

    public Scooter() {}

    public Scooter(String scooterId, String model, int batteryLevel, ScooterStatus currentStatus,
                   double currentLat, double currentLon, ChargingStation assignedStation, Zone assignedZone) {
        this.scooterId = scooterId;
        this.model = model;
        this.batteryLevel = batteryLevel;
        this.currentStatus = currentStatus != null ? currentStatus : ScooterStatus.AVAILABLE;
        this.currentLat = currentLat;
        this.currentLon = currentLon;
        this.assignedStation = assignedStation;
        this.assignedZone = assignedZone;
        this.version = 0L;
    }

    public String getScooterId() { return scooterId; }
    public void setScooterId(String scooterId) { this.scooterId = scooterId; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(int batteryLevel) { this.batteryLevel = batteryLevel; }

    public ScooterStatus getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(ScooterStatus currentStatus) { this.currentStatus = currentStatus; }

    public double getCurrentLat() { return currentLat; }
    public void setCurrentLat(double currentLat) { this.currentLat = currentLat; }

    public double getCurrentLon() { return currentLon; }
    public void setCurrentLon(double currentLon) { this.currentLon = currentLon; }

    public ChargingStation getAssignedStation() { return assignedStation; }
    public void setAssignedStation(ChargingStation assignedStation) { this.assignedStation = assignedStation; }

    public Zone getAssignedZone() { return assignedZone; }
    public void setAssignedZone(Zone assignedZone) { this.assignedZone = assignedZone; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public Instant getLastMaintenance() { return lastMaintenance; }
    public void setLastMaintenance(Instant lastMaintenance) { this.lastMaintenance = lastMaintenance; }
}
