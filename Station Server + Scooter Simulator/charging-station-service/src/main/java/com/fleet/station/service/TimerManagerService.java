package com.fleet.station.service;

import com.fleet.station.domain.ActiveRide;
import com.fleet.station.domain.InventoryStatus;
import com.fleet.station.domain.RideStatus;
import com.fleet.station.repository.ActiveRideRepository;
import com.fleet.station.repository.LocalInventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class TimerManagerService {

    private static final Logger log = LoggerFactory.getLogger(TimerManagerService.class);

    private final ActiveRideRepository activeRideRepository;
    private final LocalInventoryRepository localInventoryRepository;
    private final FareCalculatorService fareCalculatorService;

    public TimerManagerService(ActiveRideRepository activeRideRepository,
                               LocalInventoryRepository localInventoryRepository,
                               FareCalculatorService fareCalculatorService) {
        this.activeRideRepository = activeRideRepository;
        this.localInventoryRepository = localInventoryRepository;
        this.fareCalculatorService = fareCalculatorService;
    }

    /**
     * Periodic background sweep running every 60 seconds.
     * Automatically purges 1-hour expired reservations and 60-minute expired pauses.
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void runPeriodicTimerSweep() {
        Instant sixtyMinutesAgo = Instant.now().minus(60, ChronoUnit.MINUTES);
        sweepExpiredReservations(sixtyMinutesAgo);
        sweepExpiredPauses(sixtyMinutesAgo);
    }

    @Transactional
    public int sweepExpiredReservations(Instant threshold) {
        List<ActiveRide> expiredReservations = activeRideRepository
                .findByStatusAndReservedAtBefore(RideStatus.RESERVED, threshold);

        for (ActiveRide ride : expiredReservations) {
            ride.setStatus(RideStatus.CANCELLED);
            ride.setCompletedAt(Instant.now());
            activeRideRepository.save(ride);

            localInventoryRepository.findById(ride.getScooterId()).ifPresent(inv -> {
                inv.setStatus(InventoryStatus.DOCKED);
                inv.setLastUpdated(Instant.now());
                localInventoryRepository.save(inv);
            });

            log.warn("Auto-cancelled expired reservation: rideId={}, scooterId={}", ride.getRideId(), ride.getScooterId());
        }

        return expiredReservations.size();
    }

    @Transactional
    public int sweepExpiredPauses(Instant threshold) {
        List<ActiveRide> expiredPauses = activeRideRepository
                .findByStatusAndPausedAtBefore(RideStatus.PAUSED, threshold);

        for (ActiveRide ride : expiredPauses) {
            Instant now = Instant.now();
            ride.setStatus(RideStatus.COMPLETED);
            ride.setCompletedAt(now);
            ride.setIsCompliantParking(false);
            ride.setViolationReason("PAUSE_TIMEOUT_EXCEEDED");

            // Calculate final fare including 60 min pause
            long totalPause = (ride.getTotalPauseSeconds() != null ? ride.getTotalPauseSeconds() : 0L)
                    + Duration.between(ride.getPausedAt(), now).toSeconds();
            long totalDuration = Duration.between(ride.getStartedAt(), now).toSeconds();
            long activeSeconds = Math.max(0, totalDuration - totalPause);
            ride.setFareCents(fareCalculatorService.calculateFare(activeSeconds, totalPause));

            activeRideRepository.save(ride);
            log.warn("Auto-completed expired pause: rideId={}, scooterId={}", ride.getRideId(), ride.getScooterId());
        }

        return expiredPauses.size();
    }
}
