package com.fleet.central.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleet.central.domain.*;
import com.fleet.central.repository.ChargingStationRepository;
import com.fleet.central.repository.RideRepository;
import com.fleet.central.repository.ScooterRepository;
import com.fleet.central.service.GeoSpatialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class FleetEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(FleetEventConsumer.class);

    private final ScooterRepository scooterRepository;
    private final ChargingStationRepository stationRepository;
    private final RideRepository rideRepository;
    private final GeoSpatialService geoSpatialService;
    private final ObjectMapper objectMapper;

    public FleetEventConsumer(ScooterRepository scooterRepository,
                              ChargingStationRepository stationRepository,
                              RideRepository rideRepository,
                              GeoSpatialService geoSpatialService,
                              ObjectMapper objectMapper) {
        this.scooterRepository = scooterRepository;
        this.stationRepository = stationRepository;
        this.rideRepository = rideRepository;
        this.geoSpatialService = geoSpatialService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_RIDE_LIFECYCLE, groupId = "central-server-fleet-group")
    @Transactional
    public void handleRideLifecycleEvent(String message) {
        try {
            log.info("Received ride lifecycle event from Kafka: {}", message);
            JsonNode root = objectMapper.readTree(message);

            String eventType = root.path("eventType").asText();
            JsonNode payload = root.path("payload");

            if ("RIDE_STARTED".equalsIgnoreCase(eventType)) {
                handleRideStarted(payload);
            } else if ("RIDE_ENDED".equalsIgnoreCase(eventType)) {
                handleRideEnded(payload);
            }
        } catch (Exception e) {
            log.error("Error processing ride lifecycle event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_STATION_HANDOVER, groupId = "central-server-handover-group")
    @Transactional
    public void handleStationHandoverEvent(String message) {
        try {
            log.info("Received station handover event from Kafka: {}", message);
            JsonNode root = objectMapper.readTree(message);

            String scooterId = root.path("scooterId").asText();
            String targetStationId = root.path("targetStationId").asText();

            if (scooterId != null && !scooterId.isBlank() && targetStationId != null && !targetStationId.isBlank()) {
                Optional<Scooter> scooterOpt = scooterRepository.findById(scooterId);
                Optional<ChargingStation> targetStationOpt = stationRepository.findById(targetStationId);

                if (scooterOpt.isPresent() && targetStationOpt.isPresent()) {
                    Scooter scooter = scooterOpt.get();
                    scooter.setAssignedStation(targetStationOpt.get());
                    scooterRepository.save(scooter);
                    geoSpatialService.indexScooter(scooter);
                    log.info("Handover applied: Scooter {} reassigned to Station {}", scooterId, targetStationId);
                }
            }
        } catch (Exception e) {
            log.error("Error processing station handover event: {}", e.getMessage(), e);
        }
    }

    private void handleRideStarted(JsonNode payload) {
        String rideId = payload.path("rideId").asText();
        String scooterId = payload.path("scooterId").asText();
        String userId = payload.path("userId").asText();
        double startLat = payload.path("startLatitude").asDouble();
        double startLon = payload.path("startLongitude").asDouble();
        int batteryLevel = payload.path("batteryLevel").asInt(100);

        Optional<Scooter> scooterOpt = scooterRepository.findById(scooterId);
        if (scooterOpt.isPresent()) {
            Scooter scooter = scooterOpt.get();
            scooter.setCurrentStatus(ScooterStatus.MOVING);
            scooter.setBatteryLevel(batteryLevel);
            scooter.setCurrentLat(startLat);
            scooter.setCurrentLon(startLon);
            scooterRepository.save(scooter);

            // Remove from Redis stationary geospatial index
            geoSpatialService.removeScooter(scooterId);

            // Create initial active ride record in PostgreSQL
            Ride ride = new Ride(rideId, scooterId, userId, Instant.now(), startLat, startLon);
            rideRepository.save(ride);

            log.info("Ride {} started for scooter {}. Scooter marked MOVING and removed from Redis GEO.", rideId, scooterId);
        }
    }

    private void handleRideEnded(JsonNode payload) {
        String rideId = payload.path("rideId").asText();
        String scooterId = payload.path("scooterId").asText();
        double endLat = payload.path("endLatitude").asDouble();
        double endLon = payload.path("endLongitude").asDouble();
        int batteryLevel = payload.path("batteryLevel").asInt(80);
        int fareCents = payload.path("fareCents").asInt(0);
        String dockedStationId = payload.path("dockedStationId").asText(null);
        String photoUrl = payload.path("photoUrl").asText(null);
        boolean isCompliant = payload.path("isCompliantParking").asBoolean(true);
        String violationReason = payload.path("parkingViolationReason").asText(null);
        String routeTrace = payload.has("routeTrace") ? payload.path("routeTrace").toString() : null;

        // 1. Update Ride in PostgreSQL
        Optional<Ride> rideOpt = rideRepository.findById(rideId);
        Ride ride = rideOpt.orElseGet(() -> new Ride(rideId, scooterId, "unknown", Instant.now(), endLat, endLon));
        ride.setEndTime(Instant.now());
        ride.setEndLat(endLat);
        ride.setEndLon(endLon);
        ride.setFareCents(fareCents);
        ride.setPhotoUrl(photoUrl);
        ride.setIsCompliantParking(isCompliant);
        ride.setParkingViolationReason(violationReason);
        ride.setRouteTrace(routeTrace);
        rideRepository.save(ride);

        // 2. Update Scooter in PostgreSQL & Redis
        Optional<Scooter> scooterOpt = scooterRepository.findById(scooterId);
        if (scooterOpt.isPresent()) {
            Scooter scooter = scooterOpt.get();
            scooter.setCurrentStatus(ScooterStatus.AVAILABLE);
            scooter.setBatteryLevel(batteryLevel);
            scooter.setCurrentLat(endLat);
            scooter.setCurrentLon(endLon);

            if (dockedStationId != null && !dockedStationId.isBlank()) {
                stationRepository.findById(dockedStationId).ifPresent(scooter::setAssignedStation);
            }

            scooterRepository.save(scooter);

            // Re-index into Redis stationary geospatial index
            geoSpatialService.indexScooter(scooter);

            log.info("Ride {} ended for scooter {}. Scooter marked AVAILABLE and re-indexed into Redis GEO at ({}, {}).",
                    rideId, scooterId, endLat, endLon);
        }
    }
}
