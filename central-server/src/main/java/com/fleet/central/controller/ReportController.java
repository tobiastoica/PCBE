package com.fleet.central.controller;

import com.fleet.central.dto.FleetPerformanceReportDto;
import com.fleet.central.dto.RedisGeospatialReportDto;
import com.fleet.central.dto.StationOccupancyReportDto;
import com.fleet.central.service.FleetCommandService;
import com.fleet.central.service.GeoSpatialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Analytics & Redis Database Reports", description = "Query and aggregate reports from PostgreSQL SSoT and the Redis In-Memory Geospatial Cache")
public class ReportController {

    private final GeoSpatialService geoSpatialService;
    private final FleetCommandService fleetCommandService;

    public ReportController(GeoSpatialService geoSpatialService, FleetCommandService fleetCommandService) {
        this.geoSpatialService = geoSpatialService;
        this.fleetCommandService = fleetCommandService;
    }

    @GetMapping("/redis-geospatial")
    @Operation(summary = "Direct Redis DB query reporting indexed stationary scooters, coordinate members, and metadata")
    public ResponseEntity<RedisGeospatialReportDto> getRedisGeospatialReport() {
        return ResponseEntity.ok(geoSpatialService.getRedisGeospatialReport());
    }

    @GetMapping("/fleet-summary")
    @Operation(summary = "Aggregated fleet health report: status counts, battery distribution histogram, and revenue overview")
    public ResponseEntity<FleetPerformanceReportDto> getFleetPerformanceReport() {
        return ResponseEntity.ok(fleetCommandService.getFleetPerformanceReport());
    }

    @GetMapping("/station-occupancy")
    @Operation(summary = "Station capacity, current docked scooters, occupancy percentage, and surplus/deficit metrics")
    public ResponseEntity<StationOccupancyReportDto> getStationOccupancyReport() {
        return ResponseEntity.ok(fleetCommandService.getStationOccupancyReport());
    }
}
