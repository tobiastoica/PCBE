package com.fleet.central.service;

import com.fleet.central.domain.*;
import com.fleet.central.dto.*;
import com.fleet.central.kafka.FleetEventProducer;
import com.fleet.central.repository.ChargingStationRepository;
import com.fleet.central.repository.RideRepository;
import com.fleet.central.repository.ScooterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class FleetCommandService {

    private static final Logger log = LoggerFactory.getLogger(FleetCommandService.class);

    private final ScooterRepository scooterRepository;
    private final ChargingStationRepository stationRepository;
    private final RideRepository rideRepository;
    private final GeoSpatialService geoSpatialService;
    private final DockerStationManager dockerStationManager;
    private final FleetEventProducer fleetEventProducer;

    public FleetCommandService(ScooterRepository scooterRepository,
                               ChargingStationRepository stationRepository,
                               RideRepository rideRepository,
                               GeoSpatialService geoSpatialService,
                               DockerStationManager dockerStationManager,
                               FleetEventProducer fleetEventProducer) {
        this.scooterRepository = scooterRepository;
        this.stationRepository = stationRepository;
        this.rideRepository = rideRepository;
        this.geoSpatialService = geoSpatialService;
        this.dockerStationManager = dockerStationManager;
        this.fleetEventProducer = fleetEventProducer;
    }

    public FleetOverviewDto getFleetOverview() {
        List<ChargingStation> stations = stationRepository.findAll();
        List<StationOverviewDto> stationDtos = new ArrayList<>();

        for (ChargingStation s : stations) {
            long docked = scooterRepository.countByAssignedStation_StationIdAndCurrentStatus(s.getStationId(), ScooterStatus.AVAILABLE);
            stationDtos.add(new StationOverviewDto(
                    s.getStationId(),
                    s.getName(),
                    s.getLat(),
                    s.getLon(),
                    s.getCapacity(),
                    docked,
                    s.getPort(),
                    s.getStatus().name(),
                    s.getDockerContainerId()
            ));
        }

        long totalScooters = scooterRepository.count();
        long available = scooterRepository.countByCurrentStatus(ScooterStatus.AVAILABLE);
        long moving = scooterRepository.countByCurrentStatus(ScooterStatus.MOVING);
        long inUse = scooterRepository.countByCurrentStatus(ScooterStatus.RESERVED);
        long activeRides = moving + inUse;
        long maintenance = scooterRepository.countByCurrentStatus(ScooterStatus.MAINTENANCE) +
                scooterRepository.countByCurrentStatus(ScooterStatus.LOW_BATTERY);

        return new FleetOverviewDto(stations.size(), totalScooters, available, activeRides, maintenance, stationDtos);
    }

    public List<ActiveScooterDto> getActiveScooters() {
        List<Scooter> allScooters = scooterRepository.findAll();
        List<ActiveScooterDto> activeList = new ArrayList<>();

        for (Scooter s : allScooters) {
            if (s.getCurrentStatus() == ScooterStatus.MOVING || s.getCurrentStatus() == ScooterStatus.RESERVED || s.getCurrentStatus() == ScooterStatus.PAUSED) {
                Optional<Ride> activeRide = rideRepository.findByScooterIdAndEndTimeIsNull(s.getScooterId());
                String rideId = activeRide.map(Ride::getRideId).orElse("rd_sim_" + s.getScooterId());
                String userId = activeRide.map(Ride::getUserId).orElse("usr_active");
                String startTime = activeRide.map(r -> r.getStartTime().toString()).orElse(Instant.now().toString());

                String stationId = s.getAssignedStation() != null ? s.getAssignedStation().getStationId() : "station_a";
                String stationUrl = s.getAssignedStation() != null ?
                        "http://" + s.getAssignedStation().getStationId() + ":" + s.getAssignedStation().getPort() : "http://localhost:8084";

                activeList.add(new ActiveScooterDto(
                        s.getScooterId(),
                        s.getModel(),
                        s.getCurrentStatus().name(),
                        s.getBatteryLevel(),
                        rideId,
                        userId,
                        stationId,
                        stationUrl,
                        s.getCurrentLat(),
                        s.getCurrentLon(),
                        startTime
                ));
            }
        }
        return activeList;
    }

    public LiveLocationDto getLiveLocation(String scooterId) {
        Scooter scooter = scooterRepository.findById(scooterId)
                .orElseThrow(() -> new IllegalArgumentException("Scooter not found: " + scooterId));

        String stationId = scooter.getAssignedStation() != null ? scooter.getAssignedStation().getStationId() : "station_a";
        double speed = scooter.getCurrentStatus() == ScooterStatus.MOVING ? 17.5 : 0.0;

        return new LiveLocationDto(
                scooter.getScooterId(),
                scooter.getCurrentStatus().name(),
                stationId,
                scooter.getCurrentLat(),
                scooter.getCurrentLon(),
                scooter.getBatteryLevel(),
                speed,
                Instant.now().toString()
        );
    }

    @Transactional
    public void transferScooter(ScooterTransferRequest request) {
        Scooter scooter = scooterRepository.findById(request.getScooterId())
                .orElseThrow(() -> new IllegalArgumentException("Scooter not found: " + request.getScooterId()));

        ChargingStation targetStation = stationRepository.findById(request.getToStationId())
                .orElseThrow(() -> new IllegalArgumentException("Target station not found: " + request.getToStationId()));

        scooter.setAssignedStation(targetStation);
        scooter.setCurrentLat(targetStation.getLat());
        scooter.setCurrentLon(targetStation.getLon());
        scooterRepository.save(scooter);

        // Update in Redis
        geoSpatialService.indexScooter(scooter);

        // Publish administrative transfer command to Kafka
        fleetEventProducer.publishAdminTransfer(
                request.getFromStationId(),
                request.getToStationId(),
                List.of(request.getScooterId()),
                "manual_transfer",
                request.getReason()
        );

        log.info("Administrative transfer executed for scooter {} from {} to {}",
                request.getScooterId(), request.getFromStationId(), request.getToStationId());
    }

    @Transactional
    public SpawnStationResponse spawnStation(SpawnStationRequest request) {
        if (stationRepository.existsById(request.getStationId())) {
            throw new IllegalArgumentException("Station ID already exists: " + request.getStationId());
        }

        // Spawn container via Docker
        String containerId = dockerStationManager.spawnStationContainer(
                request.getStationId(),
                request.getName(),
                request.getLat(),
                request.getLon(),
                request.getPort()
        );

        ChargingStation station = new ChargingStation(
                request.getStationId(),
                request.getName(),
                request.getLat(),
                request.getLon(),
                request.getCapacity(),
                request.getPort(),
                containerId,
                StationStatus.ACTIVE
        );
        stationRepository.save(station);

        log.info("Station {} spawned with Docker container {} and registered in database.", request.getStationId(), containerId);

        return new SpawnStationResponse(
                station.getStationId(),
                containerId,
                "ACTIVE",
                "Station container spawned and registered successfully."
        );
    }
}
