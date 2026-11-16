package com.fleet.central.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "rides")
public class Ride {

    @Id
    @Column(name = "ride_id", length = 36)
    private String rideId;

    @Column(name = "scooter_id", length = 32, nullable = false)
    private String scooterId;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "start_lat", nullable = false)
    private double startLat;

    @Column(name = "start_lon", nullable = false)
    private double startLon;

    @Column(name = "end_lat")
    private Double endLat;

    @Column(name = "end_lon")
    private Double endLon;

    @Column(name = "photo_url", length = 255)
    private String photoUrl;

    @Column(name = "is_compliant_parking")
    private Boolean isCompliantParking;

    @Column(name = "parking_violation_reason", length = 255)
    private String parkingViolationReason;

    @Column(name = "fare_cents")
    private Integer fareCents;

    @Lob
    @Column(name = "route_trace")
    private String routeTrace;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Ride() {}

    public Ride(String rideId, String scooterId, String userId, Instant startTime, double startLat, double startLon) {
        this.rideId = rideId;
        this.scooterId = scooterId;
        this.userId = userId;
        this.startTime = startTime;
        this.startLat = startLat;
        this.startLon = startLon;
        this.createdAt = Instant.now();
    }

    public String getRideId() { return rideId; }
    public void setRideId(String rideId) { this.rideId = rideId; }

    public String getScooterId() { return scooterId; }
    public void setScooterId(String scooterId) { this.scooterId = scooterId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public double getStartLat() { return startLat; }
    public void setStartLat(double startLat) { this.startLat = startLat; }

    public double getStartLon() { return startLon; }
    public void setStartLon(double startLon) { this.startLon = startLon; }

    public Double getEndLat() { return endLat; }
    public void setEndLat(Double endLat) { this.endLat = endLat; }

    public Double getEndLon() { return endLon; }
    public void setEndLon(Double endLon) { this.endLon = endLon; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public Boolean getIsCompliantParking() { return isCompliantParking; }
    public void setIsCompliantParking(Boolean isCompliantParking) { this.isCompliantParking = isCompliantParking; }

    public String getParkingViolationReason() { return parkingViolationReason; }
    public void setParkingViolationReason(String parkingViolationReason) { this.parkingViolationReason = parkingViolationReason; }

    public Integer getFareCents() { return fareCents; }
    public void setFareCents(Integer fareCents) { this.fareCents = fareCents; }

    public String getRouteTrace() { return routeTrace; }
    public void setRouteTrace(String routeTrace) { this.routeTrace = routeTrace; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
