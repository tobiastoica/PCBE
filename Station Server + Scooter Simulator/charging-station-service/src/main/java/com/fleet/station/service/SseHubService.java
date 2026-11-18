package com.fleet.station.service;

import com.fleet.station.dto.TelemetryEventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class SseHubService {

    // Default timeout: 60 minutes
    private static final long DEFAULT_SSE_TIMEOUT_MS = 3_600_000L;

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emittersByRide = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String rideId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_SSE_TIMEOUT_MS);
        registerEmitter(rideId, emitter);

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of(
                            "rideId", rideId,
                            "status", "CONNECTED"
                    )));
        } catch (IOException e) {
            log.warn("Failed to dispatch initial SSE handshake for ride {}: {}", rideId, e.getMessage());
            removeEmitter(rideId, emitter);
        }

        return emitter;
    }

    public void registerEmitter(String rideId, SseEmitter emitter) {
        emittersByRide.computeIfAbsent(rideId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(rideId, emitter));
        emitter.onTimeout(() -> removeEmitter(rideId, emitter));
        emitter.onError(e -> removeEmitter(rideId, emitter));
    }

    public void broadcastTelemetry(String rideId, TelemetryEventDto telemetry) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByRide.get(rideId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("telemetry")
                        .data(telemetry));
            } catch (Exception ex) {
                log.debug("SSE delivery failure for ride {}: {}", rideId, ex.getMessage());
                deadEmitters.add(emitter);
            }
        }

        if (!deadEmitters.isEmpty()) {
            emitters.removeAll(deadEmitters);
            if (emitters.isEmpty()) {
                emittersByRide.remove(rideId, emitters);
            }
        }
    }

    public void broadcastRideCompleted(String rideId, Object receipt) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByRide.get(rideId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("ride_completed")
                        .data(receipt));
                emitter.complete();
            } catch (Exception ex) {
                log.debug("SSE complete dispatch failed for ride {}: {}", rideId, ex.getMessage());
            }
        }
        emittersByRide.remove(rideId);
    }

    public void removeEmitter(String rideId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByRide.get(rideId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                emittersByRide.remove(rideId, emitters);
            }
        }
    }

    public int getActiveSubscriberCount(String rideId) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByRide.get(rideId);
        return emitters == null ? 0 : emitters.size();
    }

    public int getTotalActiveSubscribers() {
        return emittersByRide.values().stream().mapToInt(List::size).sum();
    }

    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        if (emittersByRide.isEmpty()) {
            return;
        }

        emittersByRide.forEach((rideId, emitters) -> {
            List<SseEmitter> deadEmitters = new ArrayList<>();
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (Exception e) {
                    deadEmitters.add(emitter);
                }
            }
            if (!deadEmitters.isEmpty()) {
                emitters.removeAll(deadEmitters);
                if (emitters.isEmpty()) {
                    emittersByRide.remove(rideId, emitters);
                }
            }
        });
    }
}
