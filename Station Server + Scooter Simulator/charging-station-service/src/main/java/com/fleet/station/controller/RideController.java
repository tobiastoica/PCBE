package com.fleet.station.controller;

import com.fleet.station.dto.ReservationResponse;
import com.fleet.station.dto.ReserveRideRequest;
import com.fleet.station.dto.RideActionRequest;
import com.fleet.station.dto.RideResponse;
import com.fleet.station.service.RideService;
import com.fleet.station.service.SseHubService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/rides")
public class RideController {

    private final RideService rideService;
    private final SseHubService sseHubService;

    public RideController(RideService rideService, SseHubService sseHubService) {
        this.rideService = rideService;
        this.sseHubService = sseHubService;
    }

    @PostMapping("/reserve")
    public ResponseEntity<ReservationResponse> reserve(@Valid @RequestBody ReserveRideRequest request) {
        ReservationResponse response = rideService.reserveScooter(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/reserve/cancel")
    public ResponseEntity<ReservationResponse> cancelReservation(@Valid @RequestBody ReserveRideRequest request) {
        ReservationResponse response = rideService.cancelReservation(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/start")
    public ResponseEntity<RideResponse> startRide(@Valid @RequestBody RideActionRequest request) {
        RideResponse response = rideService.startRide(request.getRideId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/pause")
    public ResponseEntity<RideResponse> pauseRide(@Valid @RequestBody RideActionRequest request) {
        RideResponse response = rideService.pauseRide(request.getRideId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resume")
    public ResponseEntity<RideResponse> resumeRide(@Valid @RequestBody RideActionRequest request) {
        RideResponse response = rideService.resumeRide(request.getRideId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{rideId}")
    public ResponseEntity<RideResponse> getRide(@PathVariable String rideId) {
        RideResponse response = rideService.getRide(rideId);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{rideId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamRide(@PathVariable String rideId) {
        // Validate ride existence
        rideService.getRide(rideId);
        return sseHubService.subscribe(rideId);
    }
}
