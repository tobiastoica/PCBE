package com.fleet.station.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelemetryEventDto {

    private String rideId;
    private String scooterId;
    private Double lat;
    private Double lon;
    private Double speed;
    private Integer batteryLevel;
    private Instant timestamp;
}
