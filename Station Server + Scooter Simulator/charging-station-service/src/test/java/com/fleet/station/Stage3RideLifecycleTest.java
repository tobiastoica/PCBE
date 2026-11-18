package com.fleet.station;

import com.fleet.station.domain.ActiveRide;
import com.fleet.station.domain.InventoryStatus;
import com.fleet.station.domain.LocalInventory;
import com.fleet.station.domain.RideStatus;
import com.fleet.station.dto.ReservationResponse;
import com.fleet.station.dto.ReserveRideRequest;
import com.fleet.station.dto.RideResponse;
import com.fleet.station.exception.ConflictException;
import com.fleet.station.repository.ActiveRideRepository;
import com.fleet.station.repository.LocalInventoryRepository;
import com.fleet.station.service.FareCalculatorService;
import com.fleet.station.service.RideService;
import com.fleet.station.service.TimerManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class Stage3RideLifecycleTest {

    @Autowired
    private RideService rideService;

    @Autowired
    private TimerManagerService timerManagerService;

    @Autowired
    private FareCalculatorService fareCalculatorService;

    @Autowired
    private ActiveRideRepository activeRideRepository;

    @Autowired
    private LocalInventoryRepository localInventoryRepository;

    @BeforeEach
    void resetDatabaseState() {
        activeRideRepository.deleteAll();
        localInventoryRepository.deleteAll();

        // Seed initial docked scooter
        LocalInventory scooter = LocalInventory.builder()
                .scooterId("sc_stage3_01")
                .model("Ninebot Max")
                .batteryLevel(90)
                .status(InventoryStatus.DOCKED)
                .lastUpdated(Instant.now())
                .build();
        localInventoryRepository.save(scooter);
    }

    @Test
    @DisplayName("Stage 3 - Reservation: successfully lock scooter and block conflicting concurrent bookings")
    void testReserveSuccessAndConflict() {
        ReserveRideRequest request = new ReserveRideRequest("sc_stage3_01", "usr_rider_1");

        ReservationResponse response = rideService.reserveScooter(request);

        assertThat(response).isNotNull();
        assertThat(response.getRideId()).startsWith("rd_");
        assertThat(response.getReservationStatus()).isEqualTo("LOCKED_FOR_USER");
        assertThat(response.getExpiresInSeconds()).isEqualTo(3600);

        // Verify inventory marked as RESERVED
        LocalInventory updatedInventory = localInventoryRepository.findById("sc_stage3_01").orElseThrow();
        assertThat(updatedInventory.getStatus()).isEqualTo(InventoryStatus.RESERVED);

        // Verify second reservation for same scooter throws ConflictException
        ReserveRideRequest duplicateRequest = new ReserveRideRequest("sc_stage3_01", "usr_rider_2");
        assertThatThrownBy(() -> rideService.reserveScooter(duplicateRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already reserved or in use");
    }

    @Test
    @DisplayName("Stage 3 - Start Ride: verify state changes from RESERVED to MOVING")
    void testStartRideStateTransition() {
        ReserveRideRequest reserveReq = new ReserveRideRequest("sc_stage3_01", "usr_rider_1");
        ReservationResponse reservation = rideService.reserveScooter(reserveReq);

        RideResponse startResponse = rideService.startRide(reservation.getRideId());

        assertThat(startResponse.getStatus()).isEqualTo(RideStatus.MOVING);
        assertThat(startResponse.getStartedAt()).isNotNull();

        // Verify inventory marked as IN_USE
        LocalInventory updatedInventory = localInventoryRepository.findById("sc_stage3_01").orElseThrow();
        assertThat(updatedInventory.getStatus()).isEqualTo(InventoryStatus.IN_USE);
    }

    @Test
    @DisplayName("Stage 3 - Pause and Resume: verify state toggling and pause duration accumulation")
    void testPauseAndResumeTimers() throws InterruptedException {
        ReserveRideRequest reserveReq = new ReserveRideRequest("sc_stage3_01", "usr_rider_1");
        ReservationResponse reservation = rideService.reserveScooter(reserveReq);
        rideService.startRide(reservation.getRideId());

        // Pause ride
        RideResponse pauseResponse = rideService.pauseRide(reservation.getRideId());
        assertThat(pauseResponse.getStatus()).isEqualTo(RideStatus.PAUSED);
        assertThat(pauseResponse.getPausedAt()).isNotNull();

        Thread.sleep(50); // slight pause to ensure non-zero timestamp delta

        // Resume ride
        RideResponse resumeResponse = rideService.resumeRide(reservation.getRideId());
        assertThat(resumeResponse.getStatus()).isEqualTo(RideStatus.MOVING);
        assertThat(resumeResponse.getPausedAt()).isNull();
        assertThat(resumeResponse.getTotalPauseSeconds()).isNotNull();
    }

    @Test
    @DisplayName("Stage 3 - Expiration Sweep: purge reservations older than 60 minutes and restore inventory")
    void testReservationTimeoutAutoCancel() {
        String rideId = "rd_expired_test_01";
        Instant pastReservationTime = Instant.now().minus(75, ChronoUnit.MINUTES);

        ActiveRide oldRide = ActiveRide.builder()
                .rideId(rideId)
                .scooterId("sc_stage3_01")
                .userId("usr_timeout")
                .status(RideStatus.RESERVED)
                .reservedAt(pastReservationTime)
                .totalPauseSeconds(0L)
                .fareCents(0)
                .build();
        activeRideRepository.save(oldRide);

        LocalInventory inventory = localInventoryRepository.findById("sc_stage3_01").orElseThrow();
        inventory.setStatus(InventoryStatus.RESERVED);
        localInventoryRepository.save(inventory);

        // Run sweep with 60-minute threshold
        Instant sweepThreshold = Instant.now().minus(60, ChronoUnit.MINUTES);
        int purgedCount = timerManagerService.sweepExpiredReservations(sweepThreshold);

        assertThat(purgedCount).isGreaterThanOrEqualTo(1);

        ActiveRide purgedRide = activeRideRepository.findById(rideId).orElseThrow();
        assertThat(purgedRide.getStatus()).isEqualTo(RideStatus.CANCELLED);
        assertThat(purgedRide.getCompletedAt()).isNotNull();

        LocalInventory restoredInventory = localInventoryRepository.findById("sc_stage3_01").orElseThrow();
        assertThat(restoredInventory.getStatus()).isEqualTo(InventoryStatus.DOCKED);
    }

    @Test
    @DisplayName("Stage 3 - Fare Calculator: verify unlock base fee, active minutes, and pause rate computation")
    void testFareCalculation() {
        // 10 minutes active (600s), 5 minutes paused (300s)
        // Expected: 100 (base) + (10 * 25) + (5 * 10) = 100 + 250 + 50 = 400 cents (4.00 EUR)
        int fare = fareCalculatorService.calculateFare(600, 300);
        assertThat(fare).isEqualTo(400);

        // Sub-minute ceiling rounding: 65 seconds active (rounds to 2 minutes)
        // Expected: 100 + (2 * 25) = 150 cents
        int shortTripFare = fareCalculatorService.calculateFare(65, 0);
        assertThat(shortTripFare).isEqualTo(150);

        // Zero seconds: 0 cents
        int zeroFare = fareCalculatorService.calculateFare(0, 0);
        assertThat(zeroFare).isEqualTo(0);
    }
}
