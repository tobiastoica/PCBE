package com.fleet.central.controller;

import com.fleet.central.domain.Scooter;
import com.fleet.central.domain.ScooterStatus;
import com.fleet.central.dto.ScooterNearbyDto;
import com.fleet.central.repository.ScooterRepository;
import com.fleet.central.service.GeoSpatialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/v1/scooters")
@Tag(name = "Fleet Discovery & Reservations", description = "Proximity search and reservations for stationary scooters")
public class ScooterDiscoveryController {

    private final GeoSpatialService geoSpatialService;
    private final ScooterRepository scooterRepository;
    private final com.fleet.central.service.FleetCommandService fleetCommandService;

    public ScooterDiscoveryController(GeoSpatialService geoSpatialService,
                                      ScooterRepository scooterRepository,
                                      com.fleet.central.service.FleetCommandService fleetCommandService) {
        this.geoSpatialService = geoSpatialService;
        this.scooterRepository = scooterRepository;
        this.fleetCommandService = fleetCommandService;
    }

    @GetMapping("/nearby")
    @Operation(summary = "Find nearby available stationary scooters within radius")
    public ResponseEntity<List<ScooterNearbyDto>> getNearbyScooters(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false) Double longitude,
            @RequestParam(defaultValue = "1000") double radiusMeters) {

        double actualLat = lat != null ? lat : (latitude != null ? latitude : 45.7538);
        double actualLon = lon != null ? lon : (longitude != null ? longitude : 21.2257);

        // 1. Primary fast path: Query Redis GEO index
        List<ScooterNearbyDto> nearby = geoSpatialService.findNearby(actualLat, actualLon, radiusMeters);

        // 2. Fallback path if Redis is warming or temporarily unavailable
        if (nearby.isEmpty()) {
            List<Scooter> available = scooterRepository.findByCurrentStatus(ScooterStatus.AVAILABLE);
            List<ScooterNearbyDto> fallbackList = new ArrayList<>();

            for (Scooter s : available) {
                double dist = calculateHaversineDistanceMeters(actualLat, actualLon, s.getCurrentLat(), s.getCurrentLon());
                if (dist <= radiusMeters) {
                    String stationId = s.getAssignedStation() != null ? s.getAssignedStation().getStationId() : "station_a";
                    String stationName = s.getAssignedStation() != null ? s.getAssignedStation().getName() : "Station";
                    int stationPort = s.getAssignedStation() != null ? s.getAssignedStation().getPort() : 8084;
                    String stationUrl = "http://" + stationId + ":" + stationPort;

                    fallbackList.add(new ScooterNearbyDto(
                            s.getScooterId(),
                            s.getModel(),
                            s.getBatteryLevel(),
                            s.getCurrentStatus().name(),
                            s.getCurrentLat(),
                            s.getCurrentLon(),
                            stationId,
                            stationName,
                            stationUrl,
                            stationPort,
                            (int) Math.round(dist)
                    ));
                }
            }
            fallbackList.sort(Comparator.comparingInt(ScooterNearbyDto::getDistanceMeters));
            return ResponseEntity.ok(fallbackList);
        }

        return ResponseEntity.ok(nearby);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get specific scooter details and connected station connection information")
    public ResponseEntity<ScooterNearbyDto> getScooterDetails(@PathVariable String id) {
        return ResponseEntity.ok(fleetCommandService.getScooterDetails(id));
    }

    @PostMapping("/{id}/reserve")
    @Operation(summary = "Reserve and lock a scooter for a user for up to 60 minutes")
    public ResponseEntity<com.fleet.central.dto.ScooterReservationDto> reserveScooter(
            @PathVariable String id,
            @RequestBody(required = false) com.fleet.central.dto.ReservationRequest request) {
        String userId = (request != null && request.getUserId() != null) ? request.getUserId() : "usr_rider";
        return ResponseEntity.ok(fleetCommandService.reserveScooter(id, userId));
    }

    @PostMapping("/{id}/cancel-reserve")
    @Operation(summary = "Cancel reservation and unlock scooter back to available inventory")
    public ResponseEntity<com.fleet.central.dto.ScooterReservationDto> cancelReservation(
            @PathVariable String id,
            @RequestBody(required = false) com.fleet.central.dto.ReservationRequest request) {
        String userId = (request != null && request.getUserId() != null) ? request.getUserId() : "usr_rider";
        return ResponseEntity.ok(fleetCommandService.cancelReservation(id, userId));
    }

    private double calculateHaversineDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Radius of the Earth in meters
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
