package com.fleet.central.controller;

import com.fleet.central.dto.*;
import com.fleet.central.service.FleetCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin Fleet Operations", description = "Fleet overview, active scooter tracking, and station spawning")
public class AdminFleetController {

    private final FleetCommandService fleetCommandService;

    public AdminFleetController(FleetCommandService fleetCommandService) {
        this.fleetCommandService = fleetCommandService;
    }

    @GetMapping("/fleet-overview")
    @Operation(summary = "Get high-level fleet overview metrics and station status")
    public ResponseEntity<FleetOverviewDto> getFleetOverview() {
        return ResponseEntity.ok(fleetCommandService.getFleetOverview());
    }

    @GetMapping("/scooters/active")
    @Operation(summary = "List all currently active, moving, or rented scooters")
    public ResponseEntity<List<ActiveScooterDto>> getActiveScooters() {
        return ResponseEntity.ok(fleetCommandService.getActiveScooters());
    }

    @GetMapping("/scooters/{id}/live-location")
    @Operation(summary = "On-demand query for live telemetry of a moving scooter")
    public ResponseEntity<LiveLocationDto> getLiveLocation(@PathVariable String id) {
        return ResponseEntity.ok(fleetCommandService.getLiveLocation(id));
    }

    @PostMapping("/stations/spawn")
    @Operation(summary = "Spawn and register a new Docker charging station container")
    public ResponseEntity<SpawnStationResponse> spawnStation(@RequestBody SpawnStationRequest request) {
        SpawnStationResponse response = fleetCommandService.spawnStation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/scooters/transfer")
    @Operation(summary = "Execute administrative scooter transfer between stations")
    public ResponseEntity<?> transferScooter(@RequestBody ScooterTransferRequest request) {
        fleetCommandService.transferScooter(request);
        return ResponseEntity.ok().body("{\"status\":\"TRANSFER_INITIATED\",\"scooterId\":\"" + request.getScooterId() + "\"}");
    }
}
