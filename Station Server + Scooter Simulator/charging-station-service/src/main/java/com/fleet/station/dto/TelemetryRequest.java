package com.fleet.station.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelemetryRequest {

    private String rideId;

    @NotBlank(message = "scooterId is required")
    private String scooterId;

    @NotNull(message = "lat is required")
    @JsonAlias({"latitude"})
    private Double lat;

    @NotNull(message = "lon is required")
    @JsonAlias({"longitude"})
    private Double lon;

    @JsonAlias({"speed"})
    private Double speedKmh;

    private Integer batteryLevel;

    private Instant timestamp;
}
