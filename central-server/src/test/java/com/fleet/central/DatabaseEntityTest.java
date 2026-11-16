package com.fleet.central;

import com.fleet.central.domain.*;
import com.fleet.central.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class DatabaseEntityTest {

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private org.springframework.kafka.core.KafkaAdmin kafkaAdmin;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ZoneRepository zoneRepository;

    @Autowired
    private ChargingStationRepository stationRepository;

    @Autowired
    private ScooterRepository scooterRepository;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private RebalanceTaskRepository rebalanceTaskRepository;

    @Test
    @DisplayName("Verify User creation and lookup")
    @Transactional
    void testUserPersistence() {
        User user = new User("usr_test_1", "testuser", "hashedpwd", "ROLE_USER", "Test User");
        userRepository.save(user);

        Optional<User> found = userRepository.findByUsername("testuser");
        assertTrue(found.isPresent());
        assertEquals("Test User", found.get().getFullName());
    }

    @Test
    @DisplayName("Verify ChargingStation and Scooter relationship with Optimistic Locking")
    @Transactional
    void testScooterStationRelationshipAndOptimisticLock() {
        Zone zone = zoneRepository.save(new Zone("zone_t1", "Test Zone", 2, 10));
        ChargingStation station = stationRepository.save(
                new ChargingStation("st_t1", "Test Station", 45.75, 21.22, 10, 8084, "dock_t1", StationStatus.ACTIVE)
        );

        Scooter scooter = new Scooter("sc_t1", "Ninebot Max", 95, ScooterStatus.AVAILABLE, 45.751, 21.221, station, zone);
        scooter = scooterRepository.save(scooter);

        assertNotNull(scooter.getVersion());
        assertEquals("st_t1", scooter.getAssignedStation().getStationId());

        // Update status and verify version increment
        scooter.setCurrentStatus(ScooterStatus.MOVING);
        Scooter updated = scooterRepository.saveAndFlush(scooter);
        assertEquals(ScooterStatus.MOVING, updated.getCurrentStatus());
    }

    @Test
    @DisplayName("Verify Ride and RebalanceTask persistence")
    @Transactional
    void testRideAndRebalancePersistence() {
        Ride ride = new Ride("rd_t1", "sc_101", "usr_rider_01", Instant.now(), 45.75, 21.22);
        ride.setFareCents(1500);
        ride.setIsCompliantParking(true);
        rideRepository.save(ride);

        Optional<Ride> foundRide = rideRepository.findById("rd_t1");
        assertTrue(foundRide.isPresent());
        assertTrue(foundRide.get().getIsCompliantParking());

        RebalanceTask task = new RebalanceTask("reb_t1", "st_1", "st_2", List.of("sc_101", "sc_102"), "AI_AGENT", "Night balancing");
        rebalanceTaskRepository.save(task);

        Optional<RebalanceTask> foundTask = rebalanceTaskRepository.findById("reb_t1");
        assertTrue(foundTask.isPresent());
        assertEquals(2, foundTask.get().getScooterIds().size());
        assertEquals(RebalanceStatus.PROPOSED, foundTask.get().getStatus());
    }
}
