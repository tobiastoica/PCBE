package com.fleet.central.controller;

import com.fleet.central.dto.McpFleetStatusDto;
import com.fleet.central.dto.ReservationRequest;
import com.fleet.central.dto.ScooterReservationDto;
import com.fleet.central.service.FleetCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Model Context Protocol & Edge Handover", description = "Endpoints serving the Remote MCP Server and Edge Station contracts")
public class McpController {

    private final FleetCommandService fleetCommandService;

    public McpController(FleetCommandService fleetCommandService) {
        this.fleetCommandService = fleetCommandService;
    }

    /**
     * MCP Tool 3: get_fleet_and_stations_status
     */
    @GetMapping("/mcp/fleet-status")
    @Operation(summary = "Get global fleet inventory and station occupancy for MCP server and AI agents")
    public ResponseEntity<McpFleetStatusDto> getMcpFleetStatus() {
        return ResponseEntity.ok(fleetCommandService.getMcpFleetStatus());
    }

    /**
     * MCP Tool 2: book_scooter_for_user
     */
    @PostMapping("/mcp/book-scooter")
    @Operation(summary = "MCP Tool endpoint to book and lock a scooter for a specific user")
    public ResponseEntity<ScooterReservationDto> bookScooterForUser(@RequestBody ReservationRequest request) {
        String userId = request.getUserId() != null ? request.getUserId() : "usr_mcp";
        return ResponseEntity.ok(fleetCommandService.reserveScooter(request.getScooterId(), userId));
    }

    /**
     * Edge Endpoint: POST /api/v1/rides/reserve (Contract matching teammate specification Section 4.2 B)
     */
    @PostMapping("/rides/reserve")
    @Operation(summary = "Reserve selected scooter for up to 60 minutes prior to starting ride")
    public ResponseEntity<ScooterReservationDto> reserveRide(@RequestBody ReservationRequest request) {
        String userId = request.getUserId() != null ? request.getUserId() : "usr_rider";
        return ResponseEntity.ok(fleetCommandService.reserveScooter(request.getScooterId(), userId));
    }

    /**
     * Edge Endpoint: POST /api/v1/rides/reserve/cancel (Contract matching teammate specification Section 4.2 B)
     */
    @PostMapping("/rides/reserve/cancel")
    @Operation(summary = "Cancel active reservation, unlocking scooter and restoring it to available inventory")
    public ResponseEntity<ScooterReservationDto> cancelRideReservation(@RequestBody ReservationRequest request) {
        String userId = request.getUserId() != null ? request.getUserId() : "usr_rider";
        return ResponseEntity.ok(fleetCommandService.cancelReservation(request.getScooterId(), userId));
    }
}
