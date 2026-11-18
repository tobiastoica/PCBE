package com.fleet.station.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponse {

    private String rideId;
    private String scooterId;
    private String reservationStatus;
    private String reservedForUserId;
    private String stationId;
    private String stationUrl;
    private int expiresInSeconds;
    private String message;
}
