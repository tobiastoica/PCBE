package com.fleet.station.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelemetryIngestResponse {

    private String status;
    private String rideId;
    private String scooterId;
    private Long pointId;
}
