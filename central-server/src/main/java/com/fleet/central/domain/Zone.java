package com.fleet.central.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "zones")
public class Zone {

    @Id
    @Column(name = "zone_id", length = 32)
    private String zoneId;

    @Column(name = "name", length = 64, nullable = false)
    private String name;

    @Column(name = "min_capacity", nullable = false)
    private int minCapacity = 5;

    @Column(name = "target_capacity", nullable = false)
    private int targetCapacity = 20;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Zone() {}

    public Zone(String zoneId, String name, int minCapacity, int targetCapacity) {
        this.zoneId = zoneId;
        this.name = name;
        this.minCapacity = minCapacity;
        this.targetCapacity = targetCapacity;
        this.createdAt = Instant.now();
    }

    public String getZoneId() { return zoneId; }
    public void setZoneId(String zoneId) { this.zoneId = zoneId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getMinCapacity() { return minCapacity; }
    public void setMinCapacity(int minCapacity) { this.minCapacity = minCapacity; }

    public int getTargetCapacity() { return targetCapacity; }
    public void setTargetCapacity(int targetCapacity) { this.targetCapacity = targetCapacity; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
