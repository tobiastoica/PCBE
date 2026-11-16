package com.fleet.central.controller;

import com.fleet.central.domain.Ride;
import com.fleet.central.repository.RideRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Ride History", description = "Ride auditing, traces, fares, and parking compliance")
public class RideHistoryController {

    private final RideRepository rideRepository;

    public RideHistoryController(RideRepository rideRepository) {
        this.rideRepository = rideRepository;
    }

    @GetMapping("/rides/history")
    @Operation(summary = "Get global historical ride audit log")
    public ResponseEntity<List<Ride>> getAllRides() {
        return ResponseEntity.ok(rideRepository.findAllByOrderByStartTimeDesc());
    }

    @GetMapping("/users/{id}/rides")
    @Operation(summary = "Get ride history for a specific user")
    public ResponseEntity<List<Ride>> getUserRides(@PathVariable String id) {
        return ResponseEntity.ok(rideRepository.findByUserIdOrderByStartTimeDesc(id));
    }
}
