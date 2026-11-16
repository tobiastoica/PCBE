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
}
