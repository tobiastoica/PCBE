package com.fleet.central.repository;

import com.fleet.central.domain.Scooter;
import com.fleet.central.domain.ScooterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScooterRepository extends JpaRepository<Scooter, String> {
    List<Scooter> findByCurrentStatus(ScooterStatus status);
    List<Scooter> findByAssignedStation_StationId(String stationId);
    List<Scooter> findByAssignedStation_StationIdAndCurrentStatus(String stationId, ScooterStatus status);
    long countByCurrentStatus(ScooterStatus status);
    long countByAssignedStation_StationIdAndCurrentStatus(String stationId, ScooterStatus status);
}
