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
@Tag(name = "Fleet Discovery", description = "Proximity search for stationary available scooters")
public class ScooterDiscoveryController {

    private final GeoSpatialService geoSpatialService;
    private final ScooterRepository scooterRepository;

    public ScooterDiscoveryController(GeoSpatialService geoSpatialService, ScooterRepository scooterRepository) {
        this.geoSpatialService = geoSpatialService;
        this.scooterRepository = scooterRepository;
    }

    @GetMapping("/nearby")
    @Operation(summary = "Find nearby available stationary scooters within radius")
    public ResponseEntity<List<ScooterNearbyDto>> getNearbyScooters(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "1000") double radiusMeters) {

        // 1. Primary fast path: Query Redis GEO index
        List<ScooterNearbyDto> nearby = geoSpatialService.findNearby(lat, lon, radiusMeters);

        // 2. Fallback path if Redis is warming or temporarily unavailable
        if (nearby.isEmpty()) {
            List<Scooter> available = scooterRepository.findByCurrentStatus(ScooterStatus.AVAILABLE);
            List<ScooterNearbyDto> fallbackList = new ArrayList<>();

            for (Scooter s : available) {
                double dist = calculateHaversineDistanceMeters(lat, lon, s.getCurrentLat(), s.getCurrentLon());
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
