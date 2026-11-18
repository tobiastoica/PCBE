package com.fleet.station.service;

import com.fleet.station.domain.ActiveRide;
import com.fleet.station.domain.InventoryStatus;
import com.fleet.station.domain.RideStatus;
import com.fleet.station.dto.ReservationResponse;
import com.fleet.station.dto.ReserveRideRequest;
import com.fleet.station.dto.RideResponse;
import com.fleet.station.exception.BadRequestException;
import com.fleet.station.exception.ConflictException;
import com.fleet.station.exception.ResourceNotFoundException;
import com.fleet.station.repository.ActiveRideRepository;
import com.fleet.station.repository.LocalInventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class RideService {

    private static final Logger log = LoggerFactory.getLogger(RideService.class);

    private final ActiveRideRepository activeRideRepository;
    private final LocalInventoryRepository localInventoryRepository;
    private final FareCalculatorService fareCalculatorService;

    @Value("${station.id:station_a}")
    private String stationId;

    @Value("${station.url:http://localhost:8084}")
    private String stationUrl;

    public RideService(ActiveRideRepository activeRideRepository,
                       LocalInventoryRepository localInventoryRepository,
                       FareCalculatorService fareCalculatorService) {
        this.activeRideRepository = activeRideRepository;
        this.localInventoryRepository = localInventoryRepository;
        this.fareCalculatorService = fareCalculatorService;
    }

    @Transactional
    public ReservationResponse reserveScooter(ReserveRideRequest request) {
        String scooterId = request.getScooterId();
        String userId = (request.getUserId() != null && !request.getUserId().isBlank())
                ? request.getUserId() : "usr_demo";

        // 1. Conflict Check: verify scooter is not already in an active session
        Optional<ActiveRide> existingActive = activeRideRepository.findByScooterIdAndStatusIn(
                scooterId, Set.of(RideStatus.RESERVED, RideStatus.MOVING, RideStatus.PAUSED));

        if (existingActive.isPresent()) {
            throw new ConflictException("Scooter " + scooterId + " is already reserved or in use.");
        }

        // 2. Create ActiveRide entity
        String rideId = "rd_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Instant now = Instant.now();

        ActiveRide ride = ActiveRide.builder()
                .rideId(rideId)
                .scooterId(scooterId)
                .userId(userId)
                .status(RideStatus.RESERVED)
                .reservedAt(now)
                .totalPauseSeconds(0L)
                .fareCents(0)
                .build();

        activeRideRepository.save(ride);

        // 3. Update local inventory if scooter is docked
        localInventoryRepository.findById(scooterId).ifPresent(inventory -> {
            inventory.setStatus(InventoryStatus.RESERVED);
            inventory.setLastUpdated(now);
            localInventoryRepository.save(inventory);
        });

        log.info("Scooter {} successfully reserved for user {} under ride {}", scooterId, userId, rideId);

        return ReservationResponse.builder()
                .rideId(rideId)
                .scooterId(scooterId)
                .reservationStatus("LOCKED_FOR_USER")
                .reservedForUserId(userId)
                .stationId(stationId)
                .stationUrl(stationUrl)
                .expiresInSeconds(3600)
                .message("Scooter " + scooterId + " reserved successfully. Expires in 60 minutes.")
                .build();
    }

    @Transactional
    public ReservationResponse cancelReservation(ReserveRideRequest request) {
        String scooterId = request.getScooterId();

        Optional<ActiveRide> rideOpt = activeRideRepository.findByScooterIdAndStatusIn(
                scooterId, Set.of(RideStatus.RESERVED));

        if (rideOpt.isEmpty()) {
            throw new BadRequestException("No active reservation found to cancel for scooter " + scooterId);
        }

        ActiveRide ride = rideOpt.get();
        ride.setStatus(RideStatus.CANCELLED);
        ride.setCompletedAt(Instant.now());
        activeRideRepository.save(ride);

        // Free local inventory
        localInventoryRepository.findById(scooterId).ifPresent(inv -> {
            inv.setStatus(InventoryStatus.DOCKED);
            inv.setLastUpdated(Instant.now());
            localInventoryRepository.save(inv);
        });

        log.info("Reservation for scooter {} cancelled. Ride {} marked CANCELLED.", scooterId, ride.getRideId());

        return ReservationResponse.builder()
                .rideId(ride.getRideId())
                .scooterId(scooterId)
                .reservationStatus("CANCELLED_AND_UNLOCKED")
                .stationId(stationId)
                .stationUrl(stationUrl)
                .expiresInSeconds(0)
                .message("Reservation for scooter " + scooterId + " has been cancelled.")
                .build();
    }

    @Transactional
    public RideResponse startRide(String rideId) {
        ActiveRide ride = activeRideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found: " + rideId));

        if (ride.getStatus() != RideStatus.RESERVED) {
            throw new BadRequestException("Cannot start ride in status: " + ride.getStatus() + ". Ride must be RESERVED.");
        }

        Instant now = Instant.now();
        ride.setStatus(RideStatus.MOVING);
        ride.setStartedAt(now);
        activeRideRepository.save(ride);

        // Mark local inventory as IN_USE
        localInventoryRepository.findById(ride.getScooterId()).ifPresent(inv -> {
            inv.setStatus(InventoryStatus.IN_USE);
            inv.setLastUpdated(now);
            localInventoryRepository.save(inv);
        });

        log.info("Ride {} started successfully. Scooter {} marked MOVING.", rideId, ride.getScooterId());
        return mapToResponse(ride, "Ride started successfully. Scooter unlocked.");
    }

    @Transactional
    public RideResponse pauseRide(String rideId) {
        ActiveRide ride = activeRideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found: " + rideId));

        if (ride.getStatus() != RideStatus.MOVING) {
            throw new BadRequestException("Cannot pause ride in status: " + ride.getStatus() + ". Ride must be MOVING.");
        }

        Instant now = Instant.now();
        ride.setStatus(RideStatus.PAUSED);
        ride.setPausedAt(now);
        activeRideRepository.save(ride);

        log.info("Ride {} paused. 60-minute pause timer initiated.", rideId);
        return mapToResponse(ride, "Ride paused. Scooter locked. 60-minute pause timer active.");
    }

    @Transactional
    public RideResponse resumeRide(String rideId) {
        ActiveRide ride = activeRideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found: " + rideId));

        if (ride.getStatus() != RideStatus.PAUSED) {
            throw new BadRequestException("Cannot resume ride in status: " + ride.getStatus() + ". Ride must be PAUSED.");
        }

        Instant now = Instant.now();
        long pausedSeconds = Duration.between(ride.getPausedAt(), now).toSeconds();
        long currentTotalPause = ride.getTotalPauseSeconds() != null ? ride.getTotalPauseSeconds() : 0L;
        ride.setTotalPauseSeconds(currentTotalPause + pausedSeconds);
        ride.setPausedAt(null);
        ride.setStatus(RideStatus.MOVING);
        activeRideRepository.save(ride);

        log.info("Ride {} resumed after {}s in pause. Total pause time: {}s.", rideId, pausedSeconds, ride.getTotalPauseSeconds());
        return mapToResponse(ride, "Ride resumed successfully.");
    }

    public RideResponse getRide(String rideId) {
        ActiveRide ride = activeRideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found: " + rideId));
        return mapToResponse(ride, null);
    }

    public RideResponse mapToResponse(ActiveRide ride, String message) {
        Long durationSeconds = null;
        if (ride.getStartedAt() != null) {
            Instant end = ride.getCompletedAt() != null ? ride.getCompletedAt() : Instant.now();
            durationSeconds = Duration.between(ride.getStartedAt(), end).toSeconds();
        }

        Integer estimatedFare = ride.getFareCents();
        if (estimatedFare == null || estimatedFare == 0) {
            if (ride.getStartedAt() != null) {
                long totalPause = ride.getTotalPauseSeconds() != null ? ride.getTotalPauseSeconds() : 0L;
                if (ride.getPausedAt() != null) {
                    totalPause += Duration.between(ride.getPausedAt(), Instant.now()).toSeconds();
                }
                long totalDuration = durationSeconds != null ? durationSeconds : 0L;
                long activeSeconds = Math.max(0, totalDuration - totalPause);
                estimatedFare = fareCalculatorService.calculateFare(activeSeconds, totalPause);
            } else {
                estimatedFare = 0;
            }
        }

        return RideResponse.builder()
                .rideId(ride.getRideId())
                .scooterId(ride.getScooterId())
                .userId(ride.getUserId())
                .status(ride.getStatus())
                .reservedAt(ride.getReservedAt())
                .startedAt(ride.getStartedAt())
                .pausedAt(ride.getPausedAt())
                .completedAt(ride.getCompletedAt())
                .totalPauseSeconds(ride.getTotalPauseSeconds())
                .fareCents(estimatedFare)
                .isCompliantParking(ride.getIsCompliantParking())
                .violationReason(ride.getViolationReason())
                .durationSeconds(durationSeconds)
                .message(message)
                .build();
    }
}
