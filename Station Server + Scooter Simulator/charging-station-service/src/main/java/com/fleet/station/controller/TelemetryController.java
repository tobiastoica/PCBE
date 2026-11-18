package com.fleet.station.controller;

import com.fleet.station.dto.TelemetryIngestResponse;
import com.fleet.station.dto.TelemetryRequest;
import com.fleet.station.service.TelemetryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/scooter")
@RequiredArgsConstructor
public class TelemetryController {

    private final TelemetryService telemetryService;

    @PostMapping("/telemetry")
    public ResponseEntity<TelemetryIngestResponse> ingestTelemetry(@Valid @RequestBody TelemetryRequest request) {
        TelemetryIngestResponse response = telemetryService.ingestTelemetry(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
