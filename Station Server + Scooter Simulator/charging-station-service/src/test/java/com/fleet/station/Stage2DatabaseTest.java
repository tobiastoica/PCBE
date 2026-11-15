package com.fleet.station;

import com.fleet.station.domain.ActiveRide;
import com.fleet.station.domain.InventoryStatus;
import com.fleet.station.domain.LocalInventory;
import com.fleet.station.domain.RideStatus;
import com.fleet.station.domain.RoutePoint;
import com.fleet.station.repository.ActiveRideRepository;
import com.fleet.station.repository.LocalInventoryRepository;
import com.fleet.station.repository.RoutePointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class Stage2DatabaseTest {

    @Autowired
    private ActiveRideRepository activeRideRepository;

    @Autowired
    private RoutePointRepository routePointRepository;

    @Autowired
    private LocalInventoryRepository localInventoryRepository;

    @BeforeEach
    void cleanUp() {
        routePointRepository.deleteAll();
        activeRideRepository.deleteAll();
        localInventoryRepository.deleteAll();
    }

    @Test
    @DisplayName("Stage 2 - ActiveRide CRUD: should persist, query by status, and update ride state")
    void testSaveAndFindActiveRide() {
        String rideId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        ActiveRide ride = ActiveRide.builder()
                .rideId(rideId)
                .scooterId("sc_101")
                .userId("usr_42")
                .status(RideStatus.RESERVED)
                .reservedAt(now)
                .fareCents(0)
                .build();

        activeRideRepository.save(ride);

        // Verify retrieval
        Optional<ActiveRide> fetchedOpt = activeRideRepository.findById(rideId);
        assertThat(fetchedOpt).isPresent();
        ActiveRide fetched = fetchedOpt.get();
        assertThat(fetched.getScooterId()).isEqualTo("sc_101");
        assertThat(fetched.getUserId()).isEqualTo("usr_42");
        assertThat(fetched.getStatus()).isEqualTo(RideStatus.RESERVED);

        // Verify active status lookup
        Optional<ActiveRide> activeOpt = activeRideRepository.findByScooterIdAndStatusIn(
                "sc_101", Set.of(RideStatus.RESERVED, RideStatus.MOVING));
        assertThat(activeOpt).isPresent();

        // Update state to MOVING
        fetched.setStatus(RideStatus.MOVING);
        fetched.setStartedAt(now.plusSeconds(30));
        activeRideRepository.save(fetched);

        ActiveRide updated = activeRideRepository.findById(rideId).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(RideStatus.MOVING);
        assertThat(updated.getStartedAt()).isNotNull();

        // Test timeout query: query reservations before threshold
        Instant threshold = now.plus(2, ChronoUnit.HOURS);
        List<ActiveRide> expired = activeRideRepository.findByStatusAndReservedAtBefore(RideStatus.RESERVED, threshold);
        assertThat(expired).isEmpty(); // because status is MOVING now
    }

    @Test
    @DisplayName("Stage 2 - SQLite WAL Concurrency: 10 concurrent threads inserting 200 route points with zero locks")
    void testConcurrentRoutePointsInsert() throws InterruptedException {
        String rideId = "rd_concurrent_test";
        int threadCount = 10;
        int pointsPerThread = 20;
        int totalExpectedPoints = threadCount * pointsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        Instant baseTime = Instant.now();

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int p = 0; p < pointsPerThread; p++) {
                        RoutePoint point = RoutePoint.builder()
                                .rideId(rideId)
                                .timestamp(baseTime.plusMillis((long) (threadId * 1000 + p * 10)))
                                .lat(45.7500 + (threadId * 0.001) + (p * 0.0001))
                                .lon(21.2200 + (threadId * 0.001) + (p * 0.0001))
                                .speedKmh(18.5)
                                .batteryLevel(85 - p)
                                .build();
                        routePointRepository.save(point);
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(errorCount.get()).isZero();
        assertThat(successCount.get()).isEqualTo(totalExpectedPoints);

        // Verify count and chronological order
        long count = routePointRepository.countByRideId(rideId);
        assertThat(count).isEqualTo(totalExpectedPoints);

        List<RoutePoint> orderedPoints = routePointRepository.findByRideIdOrderByTimestampAsc(rideId);
        assertThat(orderedPoints).hasSize(totalExpectedPoints);

        for (int i = 1; i < orderedPoints.size(); i++) {
            assertThat(orderedPoints.get(i).getTimestamp())
                    .isAfterOrEqualTo(orderedPoints.get(i - 1).getTimestamp());
        }
    }

    @Test
    @DisplayName("Stage 2 - LocalInventory: should filter docked scooters and update status")
    void testInventoryLookupAndStatusUpdate() {
        Instant now = Instant.now();

        LocalInventory sc1 = LocalInventory.builder()
                .scooterId("sc_101")
                .model("Ninebot Max")
                .batteryLevel(95)
                .status(InventoryStatus.DOCKED)
                .lastUpdated(now)
                .build();

        LocalInventory sc2 = LocalInventory.builder()
                .scooterId("sc_102")
                .model("Ninebot Max")
                .batteryLevel(70)
                .status(InventoryStatus.DOCKED)
                .lastUpdated(now)
                .build();

        LocalInventory sc3 = LocalInventory.builder()
                .scooterId("sc_103")
                .model("Ninebot Max")
                .batteryLevel(40)
                .status(InventoryStatus.IN_USE)
                .lastUpdated(now)
                .build();

        localInventoryRepository.saveAll(List.of(sc1, sc2, sc3));

        List<LocalInventory> docked = localInventoryRepository.findByStatus(InventoryStatus.DOCKED);
        assertThat(docked).hasSize(2);
        assertThat(docked).extracting(LocalInventory::getScooterId).containsExactlyInAnyOrder("sc_101", "sc_102");

        // Reassign sc_101 to RESERVED
        sc1.setStatus(InventoryStatus.RESERVED);
        localInventoryRepository.save(sc1);

        List<LocalInventory> dockedAfterUpdate = localInventoryRepository.findByStatus(InventoryStatus.DOCKED);
        assertThat(dockedAfterUpdate).hasSize(1);
        assertThat(dockedAfterUpdate.get(0).getScooterId()).isEqualTo("sc_102");
    }
}
