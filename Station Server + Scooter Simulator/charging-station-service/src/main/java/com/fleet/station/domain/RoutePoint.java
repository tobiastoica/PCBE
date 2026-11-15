package com.fleet.station.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "route_points", indexes = {
    @Index(name = "idx_route_ride_id", columnList = "ride_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutePoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ride_id", length = 36, nullable = false)
    private String rideId;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "lat", nullable = false)
    private Double lat;

    @Column(name = "lon", nullable = false)
    private Double lon;

    @Column(name = "speed_kmh")
    private Double speedKmh;

    @Column(name = "battery_level")
    private Integer batteryLevel;
}
