package com.fleet.station.repository;

import com.fleet.station.domain.ActiveRide;
import com.fleet.station.domain.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActiveRideRepository extends JpaRepository<ActiveRide, String> {

    Optional<ActiveRide> findByScooterIdAndStatusIn(String scooterId, Collection<RideStatus> statuses);

    List<ActiveRide> findByStatusAndReservedAtBefore(RideStatus status, Instant threshold);

    List<ActiveRide> findByStatusAndPausedAtBefore(RideStatus status, Instant threshold);

    Optional<ActiveRide> findTopByScooterIdOrderByReservedAtDesc(String scooterId);
}
