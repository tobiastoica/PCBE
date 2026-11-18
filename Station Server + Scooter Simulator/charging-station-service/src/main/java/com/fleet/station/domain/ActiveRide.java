package com.fleet.station.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "active_rides")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActiveRide {

    @Id
    @Column(name = "ride_id", length = 36, nullable = false)
    private String rideId;

    @Column(name = "scooter_id", length = 32, nullable = false)
    private String scooterId;

    @Column(name = "user_id", length = 32, nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private RideStatus status;

    @Column(name = "reserved_at", nullable = false)
    private Instant reservedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "paused_at")
    private Instant pausedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "total_pause_seconds")
    @Builder.Default
    private Long totalPauseSeconds = 0L;

    @Column(name = "fare_cents")
    private Integer fareCents;

    @Column(name = "is_compliant_parking")
    private Boolean isCompliantParking;

    @Column(name = "violation_reason", length = 255)
    private String violationReason;
}
