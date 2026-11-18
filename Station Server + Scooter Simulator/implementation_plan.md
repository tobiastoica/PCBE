# Implementation Plan: Distributed Electric Scooter Fleet System
## Components: Charging Station Edge Server & Scooter Fleet Simulator

This document serves as the master implementation plan and living reference for the **Charging Station Edge Server** and the **Scooter Fleet Simulator**. Any architectural or functional modifications must be reflected here.

---

## 1. Development & Testing Methodology

To maintain system integrity and prevent regressions across distributed components, implementation follows a strict **Staged Test-Driven Strategy**:
1. **Incremental Stages:** Work is divided into 8 distinct, self-contained stages moving from foundational layers to end-to-end distributed integration.
2. **Dedicated Test Suite per Stage:** Each stage defines an explicit automated test suite (Unit, MockMvc Slice, Concurrency, or Embedded Kafka tests).
3. **Strict Regression Policy:** Before marking any stage complete, **all previous test suites must be re-run** to verify zero regressions.
4. **No Premature Implementation:** Modules must be implemented strictly within their assigned stage.

---

## 2. Component Project Structure

```
Station Server + Scooter Simulator/
├── charging-station-service/      # Edge Station Node (Spring Boot 3.x, Java 21)
│   ├── src/main/java/             # SQLite DB, REST Controllers, SSE Hub, Gemini Client, Kafka
│   ├── src/test/java/             # Unit, Integration, and MockMvc test suites
│   ├── Dockerfile
│   └── pom.xml
├── scooter-simulator/             # Fleet Simulator (Java 21, Virtual Threads)
│   ├── src/main/java/             # Virtual Threads Engine, 2-Corner Grid Routing, Telemetry Client
│   ├── src/test/java/             # Movement Math & Concurrency test suites
│   ├── Dockerfile
│   └── pom.xml
├── docker-compose.yml             # Orchestration for Station A, Station B, Simulator, Kafka
├── implementation_plan.md         # Master Architecture & Implementation Roadmap
├── specificatii_detaliate_proiect.md
└── pom.xml                        # Multi-module Root Parent POM
```

---

## 3. Staged Implementation Roadmap

```
+-----------------------------------------------------------------------------------------+
| STAGE 1: Project Scaffolding & Build Setup                                              |
| -> Maven POMs, Java 21 toolchains, Dockerfiles, basic Spring Boot applications         |
+-----------------------------------------------------------------------------------------+
                                           |
                                           v
+-----------------------------------------------------------------------------------------+
| STAGE 2: Station Edge - Database Layer & Local Persistence (SQLite WAL)                 |
| -> Entities: ActiveRide, RoutePoint, LocalInventory; Spring Data JPA; WAL Mode           |
+-----------------------------------------------------------------------------------------+
                                           |
                                           v
+-----------------------------------------------------------------------------------------+
| STAGE 3: Station Edge - Ride Lifecycle REST API & Local Timers                          |
| -> Reserve (1h), Cancel, Start, Pause (1h), Resume, Fare Calculation Service             |
+-----------------------------------------------------------------------------------------+
                                           |
                                           v
+-----------------------------------------------------------------------------------------+
| STAGE 4: Station Edge - 3s Telemetry Ingestion & Real-Time SSE Hub                      |
| -> POST /telemetry, SseEmitter dispatching live GPS and completion events               |
+-----------------------------------------------------------------------------------------+
                                           |
                                           v
+-----------------------------------------------------------------------------------------+
| STAGE 5: Scooter Simulator - Virtual Threads & 2-Corner Grid Routing                    |
| -> 1 Thread per Scooter, 50/50 Trip Decision, Manhattan Grid Math, Telemetry Emitter   |
+-----------------------------------------------------------------------------------------+
                                           |
                                           v
+-----------------------------------------------------------------------------------------+
| STAGE 6: Station Edge - Multimodal AI Parking Compliance (Gemini Vision)               |
| -> POST /rides/stop (Multipart Photo), Gemini Flash Vision Client, Mock Mode Fallback   |
+-----------------------------------------------------------------------------------------+
                                           |
                                           v
+-----------------------------------------------------------------------------------------+
| STAGE 7: Station Edge - Kafka Messaging & Inter-Station Handover Choreography           |
| -> ride-lifecycle-events, station-handover-events (Docking at Station B ends ride on A)  |
+-----------------------------------------------------------------------------------------+
                                           |
                                           v
+-----------------------------------------------------------------------------------------+
| STAGE 8: Multi-Container Orchestration & End-to-End System Verification                 |
| -> Docker Compose, Multi-node integration, Full automated regression runs               |
+-----------------------------------------------------------------------------------------+
```

---

### Stage 1: Project Scaffolding & Build Setup

#### Goal
Establish build configurations, dependency trees, and executable base skeletons for both standalone services.

#### Deliverables
* `charging-station-service/pom.xml`: Spring Boot 3.3+, Java 21, Spring Web, Spring Data JPA, SQLite JDBC, Spring Kafka, Validation, Lombok.
* `scooter-simulator/pom.xml`: Spring Boot 3.3+, Java 21, Spring Web (lightweight control API), WebClient/HttpClient, Lombok.
* Application entry points: `ChargingStationApplication.java` and `ScooterSimulatorApplication.java`.
* Baseline `application.yml` configs for both services.

#### Test Suite 1 (`Stage1ScaffoldingTest`)
* `contextLoads()`: Verifies Spring context initializes cleanly for both apps.
* `healthEndpointTest()`: Verifies HTTP 200 on Actuator/Health endpoints.
* **Exit Criteria:** Clean build via `mvn clean install` for both modules.

---

### Stage 2: Station Edge - Database Layer & Local Persistence (SQLite WAL)

#### Goal
Configure embedded SQLite database with Write-Ahead Logging (WAL) and implement JPA entity persistence for local station operations.

#### Deliverables
* SQLite DataSource configuration with `PRAGMA journal_mode=WAL;` and `busy_timeout=5000`.
* JPA Entities:
  * `ActiveRide`: `rideId` (UUID PK), `scooterId`, `userId`, `status` (`RESERVED`, `MOVING`, `PAUSED`, `COMPLETED`, `CANCELLED`), `reservedAt`, `startedAt`, `pausedAt`, `completedAt`, `fareCents`, `isCompliantParking`, `violationReason`.
  * `RoutePoint`: `id` (Auto PK), `rideId` (Indexed FK), `timestamp`, `lat`, `lon`, `speedKmh`, `batteryLevel`.
  * `LocalInventory`: `scooterId` (PK), `model`, `batteryLevel`, `status` (`DOCKED`, `RESERVED`, `IN_USE`).
* Spring Data Repositories: `ActiveRideRepository`, `RoutePointRepository`, `LocalInventoryRepository`.

#### Test Suite 2 (`Stage2DatabaseTest`)
* `testSaveAndFindActiveRide()`: Verifies ride lifecycle state persistence.
* `testConcurrentRoutePointsInsert()`: Spawns 10 concurrent threads inserting `RoutePoint` records to verify SQLite WAL mode handles concurrent writes without `SQLITE_BUSY` or table lock errors.
* `testInventoryStatusLookup()`: Verifies localized scooter assignment and query performance.
* **Regression Check:** Rerun `Stage1ScaffoldingTest`.

---

### Stage 3: Station Edge - Ride Lifecycle REST API & Local Timers

#### Goal
Implement the user-facing REST control API, fare calculation engine, and background expiration timers.

#### Deliverables
* `RideService`: Business logic for reservation, starting, pausing, resuming, and fare estimation.
* `FareCalculator`: Computes fare (base unlock fee + per-minute active rate + per-minute pause rate).
* `TimerManagerService`:
  * `@Scheduled` background worker checking for expired reservations (`> 60 minutes`). Automatically cancels and frees the scooter.
  * Sweep for pause durations exceeding 60 minutes.
* `RideController` Endpoints:
  * `POST /api/v1/rides/reserve`
  * `POST /api/v1/rides/reserve/cancel`
  * `POST /api/v1/rides/start`
  * `POST /api/v1/rides/pause`
  * `POST /api/v1/rides/resume`

#### Test Suite 3 (`Stage3RideLifecycleTest`)
* `testReserveSuccessAndConflict()`: Reserving scooter succeeds; attempting to reserve the same scooter twice returns HTTP 409 Conflict.
* `testStartRideStateTransition()`: Validates `RESERVED -> MOVING`.
* `testPauseAndResumeTimers()`: Validates state toggles and pause timestamp tracking.
* `testReservationTimeoutAutoCancel()`: Mocks clock advance; verifies reservation cancels and scooter is freed after 60 minutes.
* `testFareCalculation()`: Asserts correct cents calculation for combinations of active and paused durations.
* **Regression Check:** Rerun `Stage1ScaffoldingTest` and `Stage2DatabaseTest`.

---

### Stage 4: Station Edge - 3s Telemetry Ingestion & Real-Time SSE Hub

#### Goal
Implement high-frequency GPS telemetry ingestion and live client streaming over Server-Sent Events (SSE).

#### Deliverables
* `TelemetryController`:
  * `POST /api/v1/scooter/telemetry`: Ingests `{scooterId, lat, lon, speedKmh, batteryLevel}`, writes to `RoutePointRepository`, forwards to SSE Hub.
* `SseHubService`:
  * Manages active `SseEmitter` instances mapped by `rideId`.
  * Handles client reconnects, heartbeats, and timeouts.
* `GET /api/v1/rides/{rideId}/stream`: Produces `text/event-stream`. Streams live coordinates to the user.

#### Test Suite 4 (`Stage4TelemetryAndSseTest`)
* `testTelemetryIngestionAndPersistence()`: Ingests 10 telemetry points; asserts they are saved in SQLite `route_points`.
* `testSseEmitterDispatchesCoordinates()`: Connects a mock SSE client to `/rides/{id}/stream`; posts telemetry; asserts client receives formatted `event: telemetry` payload.
* `testSseClientDisconnectGracefulCleanup()`: Asserts dead emitter memory is cleaned up when a client disconnects.
* **Regression Check:** Rerun Stages 1, 2, and 3 test suites.

---

### Stage 5: Scooter Simulator - Virtual Threads & 2-Corner Grid Routing

#### Goal
Implement the autonomous fleet simulator using Java 21 Virtual Threads and realistic urban grid navigation.

#### Deliverables
* `VirtualThreadManager`: Manages one virtual thread per scooter (`Thread.ofVirtual().start(...)`).
* `GridMovementEngine`:
  * 2-Corner Manhattan routing algorithm (splits trip into 3 legs: Lat $\to$ Lon $\to$ Lat).
  * 3-second tick step displacement ($\approx 15\text{ m}$ per step).
  * Battery drainage math ($\approx 0.05\%$ per tick).
* `TripDecisionEngine`:
  * 50% coin flip on start:
    * **50% Docking Trip:** Targets Station B coordinates.
    * **50% Street Trip:** Targets random coordinate $\approx 500\text{m}$ away.
* `TelemetryDispatcher`: Sends HTTP `POST /api/v1/scooter/telemetry` to `assignedStationUrl` every 3 seconds.
* `SimulatorController` Endpoints:
  * `POST /api/v1/scooter/{id}/unlock`: Initiates 3s thread loop for that scooter.
  * `POST /api/v1/scooter/{id}/pause`: Freezes movement.
  * `POST /api/v1/scooter/{id}/reassign`: Updates `assignedStationUrl`.
  * `GET /api/v1/scooter/{id}/status`: Returns current coordinates, battery, and state.

#### Test Suite 5 (`Stage5SimulatorMovementTest`)
* `testManhattanRoutingCalculation()`: Asserts that generated waypoints produce two $90^\circ$ turns and arrive accurately at target coordinates.
* `test5050TripDecisionDistribution()`: Runs 100 simulated trip initiations; verifies statistically balanced distribution between Docking and Street trips.
* `testVirtualThreadConcurrency()`: Spawns 50 virtual thread scooters running simultaneously; verifies memory footprint is $< 50\text{ MB}$ RAM.
* `testScooterPauseAndResume()`: Asserts coordinate advancement freezes when paused.
* **Regression Check:** Rerun Stages 1, 2, 3, and 4 test suites.

---

### Stage 6: Station Edge - Multimodal AI Parking Compliance (Gemini Vision)

#### Goal
Integrate Google Gemini Flash Vision API to inspect parking compliance photos on street ride termination.

#### Deliverables
* `GeminiVisionClient`:
  * Sends user photo as Base64/binary to Gemini Flash Vision REST endpoint.
  * Enforces JSON Schema output: `{compliant: bool, violationType: str, confidence: float, reasoning: str}`.
  * Configurable offline toggle: `gemini.mock-enabled: true/false`.
* `POST /api/v1/rides/stop`:
  * Accepts `multipart/form-data` (`rideId`, `lat`, `lon`, `photoFile`).
  * Calls `GeminiVisionClient`.
  * Flags violation if non-compliant with confidence $> 0.80$ and assesses penalty fare.
  * Marks ride `COMPLETED` in SQLite.
  * Tells Simulator to transition scooter to `AVAILABLE`.
  * Returns final receipt with compliance verdict to user.

#### Test Suite 6 (`Stage6GeminiVisionTest`)
* `testGeminiCompliantPhoto()`: Mocks clean parking photo; asserts `is_compliant_parking = true` and base fare applied.
* `testGeminiObstructionViolation()`: Mocks wheelchair ramp obstruction photo; asserts `is_compliant_parking = false`, `violationReason = "WHEELCHAIR_RAMP_BLOCKED"`, and penalty fee added.
* `testGeminiOfflineFallback()`: Disables network/enables mock toggle; asserts graceful completion without blocking user.
* **Regression Check:** Rerun Stages 1, 2, 3, 4, and 5 test suites.

---

### Stage 7: Station Edge - Kafka Messaging & Inter-Station Handover Choreography

#### Goal
Integrate Apache Kafka for lifecycle domain events and inter-station handover choreography when a scooter docks at another station.

#### Deliverables
* Kafka Producers:
  * Topic `ride-lifecycle-events`: Emits `fleet.ride-started` and `fleet.ride-ended` (containing full `route_points` array, fare, compliance).
* Docking & Handover Flow:
  * `POST /api/v1/stations/dock` on Station B:
    * Publishes `SCOOTER_DOCKED` to topic `station-handover-events` with key = `"station_a"`.
    * Calls Simulator: reassigns `scooter.assignedStationUrl = station_b`.
  * `@KafkaListener` on Station A:
    * Consumes `SCOOTER_DOCKED`.
    * Detects active ride for `sc_101` in local SQLite.
    * Auto-completes ride with `is_compliant_parking = true` (official dock = 100% compliant, 0 photos required).
    * Pushes `ride-completed` event down the open SSE stream to the user's phone.
    * Publishes `fleet.ride-ended` to Kafka for Central Server persistence.

#### Test Suite 7 (`Stage7KafkaHandoverTest`)
* Uses `@EmbeddedKafka`:
* `testRideStartedLifecycleEventPublished()`: Asserts `fleet.ride-started` is emitted when ride starts.
* `testInterStationDockingHandover()`:
  * Simulates active ride on Station A.
  * Fires dock event on Station B.
  * Asserts Station A receives Kafka event, finalizes SQLite ride, emits SSE `ride-completed`, and emits `fleet.ride-ended`.
* **Regression Check:** Rerun Stages 1, 2, 3, 4, 5, and 6 test suites.

---

### Stage 8: Multi-Container Orchestration & End-to-End System Verification

#### Goal
Package services into production Docker containers, orchestrate with Docker Compose, and verify complete system flows end-to-end.

#### Deliverables
* Multi-stage `Dockerfile` for `charging-station-service` and `scooter-simulator`.
* `docker-compose.yml` integrating:
  * `kafka-broker` (Confluent Kafka KRaft)
  * `charging-station-a` (Port 8084, Volume `station_a_data:/data`)
  * `charging-station-b` (Port 8085, Volume `station_b_data:/data`)
  * `scooter-simulator` (Port 8088)
* End-to-end automated test runner script (`verify-system.ps1` / shell equivalent).

#### Test Suite 8 (`Stage8EndToEndSystemTest`)
* **E2E Scenario 1 (Street Parking Flow):**
  * Reserve on Station A $\to$ Start $\to$ Stream 3s telemetry $\to$ Reach street target $\to$ Call `POST /rides/stop` with photo $\to$ Validate Gemini verdict & receipt $\to$ Check SQLite record.
* **E2E Scenario 2 (Station Handover Flow):**
  * Reserve on Station A $\to$ Start $\to$ Stream 3s telemetry $\to$ Arrive at Station B $\to$ Station B dock triggered $\to$ Verify Station A auto-completes & pushes receipt over SSE $\to$ Verify scooter reassigned to Station B.
* **E2E Scenario 3 (Reservation Expiry):**
  * Reserve $\to$ Wait past expiry threshold $\to$ Verify auto-cancel and unlock.
* **Full Regression Run:** Run all test suites from Stage 1 through Stage 7.

---

## 4. Test Suite Summary & Regression Tracking

| Stage | Focus Area | Test Class / Suite | Regression Suites to Run | Status |
| :---: | :--- | :--- | :--- | :---: |
| **1** | Build & Skeletons | `Stage1ScaffoldingTest` | None | COMPLETED |
| **2** | SQLite Persistence & WAL | `Stage2DatabaseTest` | Stage 1 | COMPLETED |
| **3** | Ride Lifecycle & Timers | `Stage3RideLifecycleTest` | Stages 1, 2 | COMPLETED |
| **4** | Telemetry & SSE Streaming | `Stage4TelemetryAndSseTest` | Stages 1, 2, 3 | COMPLETED |
| **5** | Virtual Threads & Grid Movement| `Stage5SimulatorMovementTest`| Stages 1, 2, 3, 4 | PENDING |
| **6** | Gemini Vision AI Inspection | `Stage6GeminiVisionTest` | Stages 1, 2, 3, 4, 5 | PENDING |
| **7** | Kafka Handover Choreography | `Stage7KafkaHandoverTest` | Stages 1, 2, 3, 4, 5, 6 | PENDING |
| **8** | Docker Compose & E2E Validation| `Stage8EndToEndSystemTest` | Stages 1 through 7 | PENDING |

---

## 5. Maintenance & Update Rules
1. Whenever API contracts, payload schemas, or architectural flows are modified during implementation, **update this document first**.
2. Never proceed to Stage $N+1$ until Stage $N$ passes all its tests plus all prior regression suites ($1$ to $N$).
3. Maintain test isolation: tests must not rely on external networks (use `@EmbeddedKafka`, mocked Gemini API responses, and temporary SQLite databases for automated unit/integration runs).
