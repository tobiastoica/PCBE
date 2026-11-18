package com.fleet.station;

import com.fleet.station.domain.ActiveRide;
import com.fleet.station.domain.InventoryStatus;
import com.fleet.station.domain.LocalInventory;
import com.fleet.station.domain.RideStatus;
import com.fleet.station.domain.RoutePoint;
import com.fleet.station.dto.TelemetryIngestResponse;
import com.fleet.station.dto.TelemetryRequest;
import com.fleet.station.exception.BadRequestException;
import com.fleet.station.repository.ActiveRideRepository;
import com.fleet.station.repository.LocalInventoryRepository;
import com.fleet.station.repository.RoutePointRepository;
import com.fleet.station.service.SseHubService;
import com.fleet.station.service.TelemetryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class Stage4TelemetryAndSseTest {

    @Autowired
    private TelemetryService telemetryService;

    @Autowired
    private SseHubService sseHubService;

    @Autowired
    private ActiveRideRepository activeRideRepository;

    @Autowired
    private LocalInventoryRepository localInventoryRepository;

    @Autowired
    private RoutePointRepository routePointRepository;

    private static final String TEST_RIDE_ID = "rd_test_stage4_01";
    private static final String TEST_SCOOTER_ID = "sc_test_stage4_01";

    @BeforeEach
    void setupTestData() {
        routePointRepository.deleteAll();
        activeRideRepository.deleteAll();
        localInventoryRepository.deleteAll();

        LocalInventory inventory = LocalInventory.builder()
                .scooterId(TEST_SCOOTER_ID)
                .model("Ninebot Max")
                .batteryLevel(95)
                .status(InventoryStatus.IN_USE)
                .lastUpdated(Instant.now())
                .build();
        localInventoryRepository.save(inventory);

        ActiveRide ride = ActiveRide.builder()
                .rideId(TEST_RIDE_ID)
                .scooterId(TEST_SCOOTER_ID)
                .userId("usr_tester_01")
                .status(RideStatus.MOVING)
                .reservedAt(Instant.now().minusSeconds(120))
                .startedAt(Instant.now().minusSeconds(60))
                .build();
        activeRideRepository.save(ride);
    }

    @Test
    @DisplayName("Stage 4 - Ingest 10 telemetry points and verify route persistence")
    void testTelemetryIngestionAndPersistence() {
        double baseLat = 45.7500;
        double baseLon = 21.2200;

        for (int i = 0; i < 10; i++) {
            TelemetryRequest req = TelemetryRequest.builder()
                    .rideId(TEST_RIDE_ID)
                    .scooterId(TEST_SCOOTER_ID)
                    .lat(baseLat + (i * 0.0001))
                    .lon(baseLon + (i * 0.0001))
                    .speedKmh(15.0 + i)
                    .batteryLevel(95 - i)
                    .timestamp(Instant.now().plusSeconds(i * 3))
                    .build();

            TelemetryIngestResponse response = telemetryService.ingestTelemetry(req);
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("INGESTED");
            assertThat(response.getPointId()).isNotNull();
        }

        List<RoutePoint> recorded = routePointRepository.findByRideIdOrderByTimestampAsc(TEST_RIDE_ID);
        assertThat(recorded).hasSize(10);
        assertThat(recorded.get(0).getSpeedKmh()).isEqualTo(15.0);
        assertThat(recorded.get(9).getSpeedKmh()).isEqualTo(24.0);

        LocalInventory updatedScooter = localInventoryRepository.findById(TEST_SCOOTER_ID).orElseThrow();
        assertThat(updatedScooter.getBatteryLevel()).isEqualTo(86);
    }

    @Test
    @DisplayName("Stage 4 - SSE Hub delivers telemetry to active listener")
    void testSseEmitterDispatchesCoordinates() {
        AtomicBoolean receivedData = new AtomicBoolean(false);

        SseEmitter mockEmitter = new SseEmitter(10_000L) {
            @Override
            public void send(SseEventBuilder builder) throws IOException {
                receivedData.set(true);
            }
        };

        sseHubService.registerEmitter(TEST_RIDE_ID, mockEmitter);
        assertThat(sseHubService.getActiveSubscriberCount(TEST_RIDE_ID)).isGreaterThanOrEqualTo(1);

        TelemetryRequest req = TelemetryRequest.builder()
                .scooterId(TEST_SCOOTER_ID)
                .lat(45.7538)
                .lon(21.2260)
                .speedKmh(18.5)
                .batteryLevel(90)
                .build();

        telemetryService.ingestTelemetry(req);

        assertThat(receivedData.get()).isTrue();
        sseHubService.removeEmitter(TEST_RIDE_ID, mockEmitter);
    }

    @Test
    @DisplayName("Stage 4 - Disconnected SSE emitter is removed from tracking")
    void testSseClientDisconnectGracefulCleanup() {
        SseEmitter emitter = sseHubService.subscribe(TEST_RIDE_ID);
        int initialCount = sseHubService.getActiveSubscriberCount(TEST_RIDE_ID);
        assertThat(initialCount).isGreaterThan(0);

        sseHubService.removeEmitter(TEST_RIDE_ID, emitter);
        int finalCount = sseHubService.getActiveSubscriberCount(TEST_RIDE_ID);
        assertThat(finalCount).isEqualTo(initialCount - 1);
    }

    @Test
    @DisplayName("Stage 4 - Ingest without active ride throws BadRequestException")
    void testTelemetryWithoutActiveRideThrowsBadRequest() {
        TelemetryRequest req = TelemetryRequest.builder()
                .scooterId("sc_unknown_999")
                .lat(45.0)
                .lon(21.0)
                .build();

        assertThatThrownBy(() -> telemetryService.ingestTelemetry(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("No active ride found");
    }
}
