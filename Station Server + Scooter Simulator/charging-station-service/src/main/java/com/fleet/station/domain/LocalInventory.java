package com.fleet.station.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "local_inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocalInventory {

    @Id
    @Column(name = "scooter_id", length = 32, nullable = false)
    private String scooterId;

    @Column(name = "model", length = 32, nullable = false)
    private String model;

    @Column(name = "battery_level", nullable = false)
    private Integer batteryLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private InventoryStatus status;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;
}
