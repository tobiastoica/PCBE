package com.fleet.central.repository;

import com.fleet.central.domain.ChargingStation;
import com.fleet.central.domain.StationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChargingStationRepository extends JpaRepository<ChargingStation, String> {
    List<ChargingStation> findByStatus(StationStatus status);
}
