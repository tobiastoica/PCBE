package com.fleet.station.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideActionRequest {

    @NotBlank(message = "rideId is required")
    private String rideId;
}
