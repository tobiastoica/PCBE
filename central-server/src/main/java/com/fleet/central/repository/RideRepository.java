package com.fleet.central.repository;

import com.fleet.central.domain.Ride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RideRepository extends JpaRepository<Ride, String> {
    List<Ride> findByUserIdOrderByStartTimeDesc(String userId);
    List<Ride> findByScooterIdOrderByStartTimeDesc(String scooterId);
    Optional<Ride> findByScooterIdAndEndTimeIsNull(String scooterId);
    List<Ride> findAllByOrderByStartTimeDesc();
    long countByEndTimeIsNull();
    long countByEndTimeIsNotNull();
    List<Ride> findByEndTimeIsNotNull();
}
