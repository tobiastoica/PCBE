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

        // Check if recent real-time telemetry is cached in Redis
        var liveData = geoSpatialService.getLiveTelemetry(scooterId);
        if (liveData != null && !liveData.isEmpty()) {
            double lat = Double.parseDouble(String.valueOf(liveData.getOrDefault("lat", scooter.getCurrentLat())));
            double lon = Double.parseDouble(String.valueOf(liveData.getOrDefault("lon", scooter.getCurrentLon())));
            double speed = Double.parseDouble(String.valueOf(liveData.getOrDefault("speedKmh", "0.0")));
            int battery = Integer.parseInt(String.valueOf(liveData.getOrDefault("batteryLevel", scooter.getBatteryLevel())));
            String status = String.valueOf(liveData.getOrDefault("status", scooter.getCurrentStatus().name()));
            String ts = String.valueOf(liveData.getOrDefault("timestamp", Instant.now().toString()));

            return new LiveLocationDto(scooterId, status, stationId, lat, lon, battery, speed, ts);
        }

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

    /**
     * Retrieves full details for a scooter including its assigned edge station connection.
     */
    public ScooterNearbyDto getScooterDetails(String scooterId) {
        Scooter scooter = scooterRepository.findById(scooterId)
                .orElseThrow(() -> new IllegalArgumentException("Scooter not found: " + scooterId));

        String stationId = scooter.getAssignedStation() != null ? scooter.getAssignedStation().getStationId() : "station_a";
        String stationName = scooter.getAssignedStation() != null ? scooter.getAssignedStation().getName() : "Station";
        int stationPort = scooter.getAssignedStation() != null ? scooter.getAssignedStation().getPort() : 8084;
        String stationUrl = "http://" + stationId + ":" + stationPort;

        return new ScooterNearbyDto(
                scooter.getScooterId(),
                scooter.getModel(),
                scooter.getBatteryLevel(),
                scooter.getCurrentStatus().name(),
                scooter.getCurrentLat(),
                scooter.getCurrentLon(),
                stationId,
                stationName,
                stationUrl,
                stationPort,
                0
        );
    }

    /**
     * Reserves and locks a scooter for a user for up to 60 minutes.
     * Updates PostgreSQL status to RESERVED and removes from Redis stationary GEO index.
     */
    @Transactional
    public ScooterReservationDto reserveScooter(String scooterId, String userId) {
        Scooter scooter = scooterRepository.findById(scooterId)
                .orElseThrow(() -> new IllegalArgumentException("Scooter not found: " + scooterId));

        if (scooter.getCurrentStatus() != ScooterStatus.AVAILABLE) {
            throw new IllegalStateException("Scooter " + scooterId + " cannot be reserved. Current status: " + scooter.getCurrentStatus());
        }

        scooter.setCurrentStatus(ScooterStatus.RESERVED);
        scooterRepository.save(scooter);

        // Remove from Redis stationary geospatial index so other riders cannot see or book it
        geoSpatialService.removeScooter(scooterId);

        String stationId = scooter.getAssignedStation() != null ? scooter.getAssignedStation().getStationId() : "station_a";
        String stationName = scooter.getAssignedStation() != null ? scooter.getAssignedStation().getName() : "Station";
        int stationPort = scooter.getAssignedStation() != null ? scooter.getAssignedStation().getPort() : 8084;
        String stationUrl = "http://" + stationId + ":" + stationPort;

        log.info("Scooter {} reserved by user {}. Status: RESERVED, removed from Redis GEO.", scooterId, userId);

        return new ScooterReservationDto(
                scooterId,
                userId,
                "LOCKED_FOR_USER",
                3600,
                stationId,
                stationName,
                stationUrl,
                stationPort,
                "Scooter successfully locked for user. Connect to station edge service to unlock/start ride."
        );
    }

    /**
     * Cancels an active reservation, returning the scooter to AVAILABLE and re-indexing in Redis GEO.
     */
    @Transactional
    public ScooterReservationDto cancelReservation(String scooterId, String userId) {
        Scooter scooter = scooterRepository.findById(scooterId)
                .orElseThrow(() -> new IllegalArgumentException("Scooter not found: " + scooterId));

        if (scooter.getCurrentStatus() == ScooterStatus.RESERVED) {
            scooter.setCurrentStatus(ScooterStatus.AVAILABLE);
            scooterRepository.save(scooter);
            geoSpatialService.indexScooter(scooter);
            log.info("Reservation cancelled for scooter {}. Status returned to AVAILABLE and re-indexed in Redis GEO.", scooterId);
        }

        String stationId = scooter.getAssignedStation() != null ? scooter.getAssignedStation().getStationId() : "station_a";
        String stationName = scooter.getAssignedStation() != null ? scooter.getAssignedStation().getName() : "Station";
        int stationPort = scooter.getAssignedStation() != null ? scooter.getAssignedStation().getPort() : 8084;
        String stationUrl = "http://" + stationId + ":" + stationPort;

        return new ScooterReservationDto(
                scooterId,
                userId,
                "CANCELLED_AND_UNLOCKED",
                0,
                stationId,
                stationName,
                stationUrl,
                stationPort,
                "Reservation cancelled. Scooter returned to available inventory."
        );
    }

    /**
     * Serves MCP Tool 3: get_fleet_and_stations_status for the remote MCP server and autonomous AI agents.
     */
    public McpFleetStatusDto getMcpFleetStatus() {
        List<ChargingStation> stations = stationRepository.findAll();
        List<McpFleetStatusDto.McpStationStatusDto> stationDtos = new ArrayList<>();

        for (ChargingStation s : stations) {
            List<Scooter> docked = scooterRepository.findByAssignedStation_StationIdAndCurrentStatus(s.getStationId(), ScooterStatus.AVAILABLE);
            List<String> scooterIds = docked.stream().map(Scooter::getScooterId).toList();
            int deficit = s.getCapacity() - docked.size();

            stationDtos.add(new McpFleetStatusDto.McpStationStatusDto(
                    s.getStationId(),
                    s.getName(),
                    s.getCapacity(),
                    docked.size(),
                    scooterIds,
                    deficit
            ));
        }

        List<Scooter> allScooters = scooterRepository.findAll();
        List<McpFleetStatusDto.McpScooterSummaryDto> scooterDtos = allScooters.stream().map(s -> new McpFleetStatusDto.McpScooterSummaryDto(
                s.getScooterId(),
                s.getModel(),
                s.getBatteryLevel(),
                s.getCurrentStatus().name(),
                s.getAssignedStation() != null ? s.getAssignedStation().getStationId() : "unassigned",
                s.getCurrentLat(),
                s.getCurrentLon()
        )).toList();

        return new McpFleetStatusDto(stationDtos, scooterDtos);
    }

    /**
     * Computes detailed station occupancy and utilization metrics.
     */
    public StationOccupancyReportDto getStationOccupancyReport() {
        List<ChargingStation> stations = stationRepository.findAll();
        int totalCapacity = 0;
        long totalDocked = 0;
        List<StationOccupancyReportDto.StationOccupancyItemDto> items = new ArrayList<>();

        for (ChargingStation s : stations) {
            List<Scooter> docked = scooterRepository.findByAssignedStation_StationIdAndCurrentStatus(s.getStationId(), ScooterStatus.AVAILABLE);
            long count = docked.size();
            totalCapacity += s.getCapacity();
            totalDocked += count;

            double occPercent = s.getCapacity() > 0 ? ((double) count / s.getCapacity()) * 100.0 : 0.0;
            boolean isFull = count >= s.getCapacity();
            boolean isEmpty = count == 0;
            int target = s.getCapacity() / 2;
            int surplusDeficit = (int) count - target;

            items.add(new StationOccupancyReportDto.StationOccupancyItemDto(
                    s.getStationId(),
                    s.getName(),
                    s.getCapacity(),
                    count,
                    Math.round(occPercent * 10.0) / 10.0,
                    s.getStatus().name(),
                    isFull,
                    isEmpty,
                    surplusDeficit,
                    docked.stream().map(Scooter::getScooterId).toList()
            ));
        }

        double avgOccupancy = totalCapacity > 0 ? ((double) totalDocked / totalCapacity) * 100.0 : 0.0;
        return new StationOccupancyReportDto(
                stations.size(),
                totalCapacity,
                totalDocked,
                Math.round(avgOccupancy * 10.0) / 10.0,
                items
        );
    }

    /**
     * Aggregates comprehensive fleet and trip performance analytics.
     */
    public FleetPerformanceReportDto getFleetPerformanceReport() {
        List<Scooter> allScooters = scooterRepository.findAll();
        long totalScooters = allScooters.size();
        long available = 0;
        long reserved = 0;
        long moving = 0;
        long maintenance = 0;

        int minBattery = 100;
        int maxBattery = 0;
        int sumBattery = 0;
        long lowBatteryCount = 0;

        long b80plus = 0;
        long b50to79 = 0;
        long b20to49 = 0;
        long bless20 = 0;

        for (Scooter s : allScooters) {
            int bat = s.getBatteryLevel();
            sumBattery += bat;
            if (bat < minBattery) minBattery = bat;
            if (bat > maxBattery) maxBattery = bat;
            if (bat < 20) lowBatteryCount++;

            if (bat >= 80) b80plus++;
            else if (bat >= 50) b50to79++;
            else if (bat >= 20) b20to49++;
            else bless20++;

            switch (s.getCurrentStatus()) {
                case AVAILABLE -> available++;
                case RESERVED -> reserved++;
                case MOVING -> moving++;
                case LOW_BATTERY, MAINTENANCE -> maintenance++;
            }
        }

        double avgBattery = totalScooters > 0 ? (double) sumBattery / totalScooters : 0.0;
        var batteryMap = new java.util.LinkedHashMap<String, Long>();
        batteryMap.put("80-100%", b80plus);
        batteryMap.put("50-79%", b50to79);
        batteryMap.put("20-49%", b20to49);
        batteryMap.put("<20%", bless20);

        long activeRides = rideRepository.countByEndTimeIsNull();
        long completedRides = rideRepository.countByEndTimeIsNotNull();
        long totalRides = activeRides + completedRides;

        List<Ride> completedList = rideRepository.findByEndTimeIsNotNull();
        long totalRevenueCents = 0;
        long compliantParkingCount = 0;

        for (Ride r : completedList) {
            totalRevenueCents += r.getFareCents();
            if (Boolean.TRUE.equals(r.getIsCompliantParking())) {
                compliantParkingCount++;
            }
        }

        double revenueEur = (double) totalRevenueCents / 100.0;
        double compliancePercent = completedRides > 0 ? ((double) compliantParkingCount / completedRides) * 100.0 : 100.0;

        return new FleetPerformanceReportDto(
                totalScooters,
                available,
                reserved,
                moving,
                maintenance,
                Math.round(avgBattery * 10.0) / 10.0,
                minBattery,
                maxBattery,
                lowBatteryCount,
                batteryMap,
                totalRides,
                activeRides,
                completedRides,
                totalRevenueCents,
                Math.round(revenueEur * 100.0) / 100.0,
                Math.round(compliancePercent * 10.0) / 10.0,
                Instant.now().toString()
        );
    }
}
