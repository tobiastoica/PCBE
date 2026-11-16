package com.fleet.central.service;

import com.fleet.central.domain.Scooter;
import com.fleet.central.domain.ScooterStatus;
import com.fleet.central.dto.ScooterNearbyDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GeoSpatialService {

    private static final Logger log = LoggerFactory.getLogger(GeoSpatialService.class);
    public static final String KEY_GEO = "fleet:stationary:geo";
    public static final String KEY_META_PREFIX = "fleet:scooter:";

    private final StringRedisTemplate redisTemplate;

    public GeoSpatialService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Indexes a stationary available scooter in Redis GEO and stores metadata.
     */
    public void indexScooter(Scooter scooter) {
        try {
            if (scooter.getCurrentStatus() != ScooterStatus.AVAILABLE) {
                removeScooter(scooter.getScooterId());
                return;
            }

            Point point = new Point(scooter.getCurrentLon(), scooter.getCurrentLat());
            redisTemplate.opsForGeo().add(KEY_GEO, point, scooter.getScooterId());

            String metaKey = KEY_META_PREFIX + scooter.getScooterId() + ":meta";
            Map<String, String> meta = new HashMap<>();
            meta.put("scooterId", scooter.getScooterId());
            meta.put("model", scooter.getModel());
            meta.put("batteryLevel", String.valueOf(scooter.getBatteryLevel()));
            meta.put("status", scooter.getCurrentStatus().name());
            meta.put("lat", String.valueOf(scooter.getCurrentLat()));
            meta.put("lon", String.valueOf(scooter.getCurrentLon()));

            if (scooter.getAssignedStation() != null) {
                meta.put("stationId", scooter.getAssignedStation().getStationId());
                meta.put("stationName", scooter.getAssignedStation().getName());
                meta.put("stationPort", String.valueOf(scooter.getAssignedStation().getPort()));
                meta.put("stationUrl", "http://" + scooter.getAssignedStation().getStationId() + ":" + scooter.getAssignedStation().getPort());
            }

            redisTemplate.opsForHash().putAll(metaKey, meta);
        } catch (Exception e) {
            log.warn("Failed to index scooter in Redis GEO: {}", e.getMessage());
        }
    }

    /**
     * Removes a scooter from the Redis geospatial index (e.g. when rented, moving, or offline).
     */
    public void removeScooter(String scooterId) {
        try {
            redisTemplate.opsForZSet().remove(KEY_GEO, scooterId);
            redisTemplate.delete(KEY_META_PREFIX + scooterId + ":meta");
        } catch (Exception e) {
            log.warn("Failed to remove scooter from Redis GEO: {}", e.getMessage());
        }
    }

    /**
     * Fast sub-millisecond query for nearby available scooters within a given radius in meters.
     */
    public List<ScooterNearbyDto> findNearby(double lat, double lon, double radiusMeters) {
        List<ScooterNearbyDto> results = new ArrayList<>();
        try {
            Point center = new Point(lon, lat);
            Distance distance = new Distance(radiusMeters, RedisGeoCommands.DistanceUnit.METERS);
            Circle circle = new Circle(center, distance);

            RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                    .includeDistance()
                    .includeCoordinates()
                    .sortAscending();

            GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults = redisTemplate.opsForGeo().radius(KEY_GEO, circle, args);

            if (geoResults != null) {
                for (var result : geoResults) {
                    String scooterId = result.getContent().getName();
                    double dist = result.getDistance().getValue();

                    String metaKey = KEY_META_PREFIX + scooterId + ":meta";
                    Map<Object, Object> entries = redisTemplate.opsForHash().entries(metaKey);

                    if (!entries.isEmpty()) {
                        String model = String.valueOf(entries.getOrDefault("model", "Ninebot"));
                        int battery = Integer.parseInt(String.valueOf(entries.getOrDefault("batteryLevel", "100")));
                        String status = String.valueOf(entries.getOrDefault("status", "AVAILABLE"));
                        double sLat = Double.parseDouble(String.valueOf(entries.getOrDefault("lat", String.valueOf(result.getContent().getPoint().getY()))));
                        double sLon = Double.parseDouble(String.valueOf(entries.getOrDefault("lon", String.valueOf(result.getContent().getPoint().getX()))));
                        String stationId = String.valueOf(entries.getOrDefault("stationId", ""));
                        String stationName = String.valueOf(entries.getOrDefault("stationName", ""));
                        String stationUrl = String.valueOf(entries.getOrDefault("stationUrl", ""));
                        int stationPort = Integer.parseInt(String.valueOf(entries.getOrDefault("stationPort", "8084")));

                        results.add(new ScooterNearbyDto(
                                scooterId, model, battery, status, sLat, sLon,
                                stationId, stationName, stationUrl, stationPort, (int) Math.round(dist)
                        ));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Redis GEO search failed: {}", e.getMessage());
        }
        return results;
    }

    /**
     * Stores high-frequency live telemetry in Redis with a 5-minute TTL without writing to PostgreSQL.
     */
    public void saveLiveTelemetry(String scooterId, double lat, double lon, double speedKmh, int batteryLevel, String status) {
        try {
            String key = "fleet:scooter:" + scooterId + ":live";
            Map<String, String> telemetry = new HashMap<>();
            telemetry.put("scooterId", scooterId);
            telemetry.put("lat", String.valueOf(lat));
            telemetry.put("lon", String.valueOf(lon));
            telemetry.put("speedKmh", String.valueOf(speedKmh));
            telemetry.put("batteryLevel", String.valueOf(batteryLevel));
            telemetry.put("status", status != null ? status : "MOVING");
            telemetry.put("timestamp", java.time.Instant.now().toString());

            redisTemplate.opsForHash().putAll(key, telemetry);
            redisTemplate.expire(key, java.time.Duration.ofMinutes(5));
        } catch (Exception e) {
            log.warn("Failed to store live telemetry in Redis: {}", e.getMessage());
        }
    }

    /**
     * Retrieves the latest cached live telemetry for a scooter from Redis.
     */
    public Map<Object, Object> getLiveTelemetry(String scooterId) {
        try {
            return redisTemplate.opsForHash().entries("fleet:scooter:" + scooterId + ":live");
        } catch (Exception e) {
            log.warn("Failed to read live telemetry from Redis: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Inspects Redis geospatial cache directly and returns an audit report of all indexed stationary scooters.
     */
    public com.fleet.central.dto.RedisGeospatialReportDto getRedisGeospatialReport() {
        try {
            Long count = redisTemplate.opsForZSet().zCard(KEY_GEO);
            long total = count != null ? count : 0L;

            Set<String> memberIds = redisTemplate.opsForZSet().range(KEY_GEO, 0, -1);
            List<ScooterNearbyDto> indexedList = new ArrayList<>();

            if (memberIds != null) {
                for (String scooterId : memberIds) {
                    Map<Object, Object> entries = redisTemplate.opsForHash().entries(KEY_META_PREFIX + scooterId + ":meta");
                    if (!entries.isEmpty()) {
                        String model = String.valueOf(entries.getOrDefault("model", "Ninebot Max"));
                        int battery = Integer.parseInt(String.valueOf(entries.getOrDefault("batteryLevel", "100")));
                        String status = String.valueOf(entries.getOrDefault("status", "AVAILABLE"));
                        double sLat = Double.parseDouble(String.valueOf(entries.getOrDefault("lat", "45.7538")));
                        double sLon = Double.parseDouble(String.valueOf(entries.getOrDefault("lon", "21.2257")));
                        String stationId = String.valueOf(entries.getOrDefault("stationId", "station_a"));
                        String stationName = String.valueOf(entries.getOrDefault("stationName", "Station"));
                        String stationUrl = String.valueOf(entries.getOrDefault("stationUrl", "http://localhost:8084"));
                        int stationPort = Integer.parseInt(String.valueOf(entries.getOrDefault("stationPort", "8084")));

                        indexedList.add(new ScooterNearbyDto(
                                scooterId, model, battery, status, sLat, sLon,
                                stationId, stationName, stationUrl, stationPort, 0
                        ));
                    }
                }
            }

            return new com.fleet.central.dto.RedisGeospatialReportDto(
                    KEY_GEO,
                    total,
                    indexedList,
                    total,
                    "HEALTHY",
                    java.time.Instant.now().toString()
            );
        } catch (Exception e) {
            log.error("Failed to generate Redis geospatial report: {}", e.getMessage());
            return new com.fleet.central.dto.RedisGeospatialReportDto(
                    KEY_GEO, 0L, Collections.emptyList(), 0L, "DEGRADED: " + e.getMessage(), java.time.Instant.now().toString()
            );
        }
    }
}
