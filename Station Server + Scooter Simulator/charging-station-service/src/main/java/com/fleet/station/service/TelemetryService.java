package com.fleet.station.service;

import com.fleet.station.domain.ActiveRide;
import com.fleet.station.domain.RideStatus;
import com.fleet.station.domain.RoutePoint;
import com.fleet.station.dto.TelemetryEventDto;
import com.fleet.station.dto.TelemetryIngestResponse;
import com.fleet.station.dto.TelemetryRequest;
import com.fleet.station.exception.BadRequestException;
import com.fleet.station.exception.ResourceNotFoundException;
import com.fleet.station.repository.ActiveRideRepository;
import com.fleet.station.repository.LocalInventoryRepository;
import com.fleet.station.repository.RoutePointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelemetryService {

    private final RoutePointRepository routePointRepository;
    private final ActiveRideRepository activeRideRepository;
    private final LocalInventoryRepository localInventoryRepository;
    private final SseHubService sseHubService;

    @Transactional
    public TelemetryIngestResponse ingestTelemetry(TelemetryRequest request) {
        String targetRideId = request.getRideId();

        // 1. Resolve active ride if rideId not explicitly provided
        if (targetRideId == null || targetRideId.isBlank()) {
            Optional<ActiveRide> activeRideOpt = activeRideRepository.findByScooterIdAndStatusIn(
                    request.getScooterId(),
                    List.of(RideStatus.MOVING, RideStatus.PAUSED, RideStatus.RESERVED)
            );
            if (activeRideOpt.isPresent()) {
                targetRideId = activeRideOpt.get().getRideId();
            } else {
                throw new BadRequestException("No active ride found for scooter: " + request.getScooterId());
            }
        } else {
            if (!activeRideRepository.existsById(targetRideId)) {
                throw new ResourceNotFoundException("Active ride not found with ID: " + targetRideId);
            }
        }

        // 2. Persist route point
        Instant timestamp = request.getTimestamp() != null ? request.getTimestamp() : Instant.now();
        RoutePoint point = RoutePoint.builder()
                .rideId(targetRideId)
                .timestamp(timestamp)
                .lat(request.getLat())
                .lon(request.getLon())
                .speedKmh(request.getSpeedKmh() != null ? request.getSpeedKmh() : 0.0)
                .batteryLevel(request.getBatteryLevel() != null ? request.getBatteryLevel() : 100)
                .build();

        RoutePoint saved = routePointRepository.save(point);

        // 3. Update local inventory if scooter is managed locally
        localInventoryRepository.findById(request.getScooterId()).ifPresent(scooter -> {
            if (request.getBatteryLevel() != null) {
                scooter.setBatteryLevel(request.getBatteryLevel());
            }
            scooter.setLastUpdated(timestamp);
            localInventoryRepository.save(scooter);
        });

        // 4. Forward live event to SSE subscribers
        TelemetryEventDto sseDto = TelemetryEventDto.builder()
                .rideId(targetRideId)
                .scooterId(request.getScooterId())
                .lat(request.getLat())
                .lon(request.getLon())
                .speed(request.getSpeedKmh())
                .batteryLevel(request.getBatteryLevel())
                .timestamp(timestamp)
                .build();

        sseHubService.broadcastTelemetry(targetRideId, sseDto);

        return TelemetryIngestResponse.builder()
                .status("INGESTED")
                .rideId(targetRideId)
                .scooterId(request.getScooterId())
                .pointId(saved.getId())
                .build();
    }
}
