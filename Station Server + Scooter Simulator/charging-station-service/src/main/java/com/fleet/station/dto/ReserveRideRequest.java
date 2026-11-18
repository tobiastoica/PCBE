package com.fleet.station.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReserveRideRequest {

    @NotBlank(message = "scooterId is required")
    private String scooterId;

    private String userId;
}
