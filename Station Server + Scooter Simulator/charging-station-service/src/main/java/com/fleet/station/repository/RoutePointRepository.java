package com.fleet.station.repository;

import com.fleet.station.domain.RoutePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoutePointRepository extends JpaRepository<RoutePoint, Long> {

    List<RoutePoint> findByRideIdOrderByTimestampAsc(String rideId);

    long countByRideId(String rideId);
}
