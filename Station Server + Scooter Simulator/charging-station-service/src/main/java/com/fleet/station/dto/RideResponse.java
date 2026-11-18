package com.fleet.station.dto;

import com.fleet.station.domain.RideStatus;
import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideResponse {

    private String rideId;
    private String scooterId;
    private String userId;
    private RideStatus status;
    private Instant reservedAt;
    private Instant startedAt;
    private Instant pausedAt;
    private Instant completedAt;
    private Long totalPauseSeconds;
    private Integer fareCents;
    private Boolean isCompliantParking;
    private String violationReason;
    private Long durationSeconds;
    private String message;
}
