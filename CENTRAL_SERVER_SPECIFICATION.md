# Central Server & Authoritative Database Specification
## Urban Micro-Mobility & Autonomous Fleet Rebalancing Platform
### Component: Central Management Service & Database Backbone (Port 8081)

---

## 1. Executive Summary & Distributed Role

The **Central Management Server** is the authoritative master and Single Source of Truth (SSoT) for the entire distributed micro-mobility platform. While edge computing nodes (Charging Station servers) handle high-frequency 3-second GPS telemetry and active user ride sessions, the Central Server is responsible for:

1. **Authoritative Persistence (PostgreSQL 16):** Managing persistent domain entities (Users, Zones, Charging Stations, Scooters, Completed Rides, and Rebalance Tasks).
2. **In-Memory Proximity Discovery (Redis 7):** Indexing all **stationary, available** e-scooters using Redis `GEO` commands to serve sub-millisecond proximity queries to mobile clients and external agents.
3. **Kafka Event Backbone Coordination:** Consuming lifecycle and handover events from edge stations to update the fleet state, and broadcasting administrative or rebalancing dispatch commands.
4. **Active Fleet Directory:** Maintaining an inventory of currently rented/moving scooters without storing every 3s GPS ping, allowing administrators to query live locations on-demand directly from managing edge nodes.
5. **Dynamic Infrastructure Provisioning:** Managing and spawning new Dockerized charging station containers on-demand via the host Docker daemon (`/var/run/docker.sock`).
6. **Web Administration Dashboard:** Providing a responsive, professional management UI for operators to authenticate, visualize available fleet pins on an interactive map, inspect active rentals, spawn stations, and approve AI rebalancing proposals.

---

## 2. Security & Authentication Contract

The Central Server employs a dual authentication model:

### 2.1. Human Users & Operators (JWT Authentication)
- Users and administrators authenticate via `POST /api/v1/auth/login` with `username` and `password`.
- Upon successful login, the server returns a signed HMAC-SHA256 JWT containing `sub` (username), `role` (`ROLE_ADMIN` or `ROLE_USER`), and an expiration timestamp (default: 24 hours).
- Protected endpoints require the HTTP header:
  ```http
  Authorization: Bearer <jwt_token>
  ```
- **Pre-seeded Accounts:**
  - Admin: `admin` / `admin123` (`ROLE_ADMIN`)
  - Rider: `rider` / `rider123` (`ROLE_USER`)

### 2.2. Inter-Service & Machine-to-Machine (Demo API Key)
- Edge station nodes, simulator workers, or external scripts can bypass user login by supplying the configured demo API key header:
  ```http
  X-API-KEY: fleet-secret-key-2026
  ```
- If a valid `X-API-KEY` is present, the request is authenticated with `ROLE_SYSTEM`.

---

## 3. REST API Contract (OpenAPI 3.0)

Base URL: `http://<central-server-host>:8081`  
Swagger UI: `http://<central-server-host>:8081/swagger-ui.html`  
OpenAPI JSON: `http://<central-server-host>:8081/v3/api-docs`

---

### 3.1. Authentication Endpoints

#### `POST /api/v1/auth/login`
Authenticates a user or administrator.
- **Access:** Public
- **Request Body:**
  ```json
  {
    "username": "admin",
    "password": "admin123"
  }
  ```
- **Response:** `200 OK`
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "type": "Bearer",
    "username": "admin",
    "role": "ROLE_ADMIN",
    "fullName": "Fleet Administrator",
    "expiresIn": 86400000
  }
  ```

#### `POST /api/v1/auth/register`
Registers a new customer account.
- **Access:** Public
- **Request Body:**
  ```json
  {
    "username": "john_doe",
    "password": "SecurePassword123",
    "fullName": "John Doe"
  }
  ```
- **Response:** `201 Created`

---

### 3.2. Fleet Discovery & Public Map Endpoints

#### `GET /api/v1/scooters/nearby`
Primary discovery endpoint used by the mobile app and UI map tab. Interrogates **Redis 7** using `GEORADIUS` to return stationary scooters ready for rental.
- **Access:** Public / Authenticated
- **Query Parameters:**
  - `lat` or `latitude` (double, required): Latitude of user or map center (e.g. `45.7537`)
  - `lon` or `longitude` (double, required): Longitude of user or map center (e.g. `21.2257`)
  - `radiusMeters` (integer, optional, default: `1000`): Search radius in meters
- **Response:** `200 OK`
  ```json
  [
    {
      "scooterId": "sc_101",
      "model": "Ninebot Max G2",
      "batteryLevel": 88,
      "status": "AVAILABLE",
      "lat": 45.7538,
      "lon": 21.2257,
      "stationId": "station_a",
      "stationName": "Victory Square Station",
      "stationUrl": "http://station_a:8084",
      "stationPort": 8084,
      "distanceMeters": 42
    }
  ]
  ```

#### `GET /api/v1/scooters/{id}`
Inspects a specific scooter by ID, returning its hardware status and managing edge station network connection so the mobile app or client can communicate directly with the edge station.
- **Access:** Public / Authenticated
- **Path Variable:** `id` (e.g. `sc_101`)
- **Response:** `200 OK`
  ```json
  {
    "scooterId": "sc_101",
    "model": "Ninebot Max G2",
    "batteryLevel": 92,
    "status": "AVAILABLE",
    "lat": 45.7540,
    "lon": 21.2259,
    "stationId": "station_a",
    "stationName": "Victory Square Station",
    "stationUrl": "http://station_a:8084",
    "stationPort": 8084,
    "distanceMeters": 0
  }
  ```

#### `POST /api/v1/scooters/{id}/reserve`
Reserves and blocks a scooter for a user prior to unlocking. Sets status to `RESERVED` in PostgreSQL and temporarily evicts the scooter from Redis `fleet:stationary:geo` to prevent double-booking.
- **Access:** Public / Authenticated
- **Path Variable:** `id` (e.g. `sc_101`)
- **Query Parameters:** `userId` (string, optional, default: `usr_demo`)
- **Response:** `200 OK`
  ```json
  {
    "scooterId": "sc_101",
    "status": "LOCKED_FOR_USER",
    "reservedForUserId": "usr_demo",
    "stationId": "station_a",
    "stationUrl": "http://station_a:8084",
    "stationPort": 8084,
    "reservationExpiresInSeconds": 3600,
    "message": "Scooter sc_101 successfully reserved and locked. Connect to station for ride initiation."
  }
  ```

#### `POST /api/v1/scooters/{id}/cancel-reserve`
Cancels an active reservation, returning status to `AVAILABLE` in PostgreSQL and re-indexing the scooter into Redis `fleet:stationary:geo`.
- **Access:** Public / Authenticated
- **Path Variable:** `id` (e.g. `sc_101`)
- **Response:** `200 OK`
  ```json
  {
    "scooterId": "sc_101",
    "status": "CANCELLED_AND_UNLOCKED",
    "stationId": "station_a",
    "stationUrl": "http://station_a:8084",
    "stationPort": 8084,
    "reservationExpiresInSeconds": 0,
    "message": "Reservation cancelled. Scooter sc_101 is now AVAILABLE for fleet rental."
  }
  ```

#### `POST /api/v1/rides/reserve` & `POST /api/v1/rides/reserve/cancel`
Direct inter-service endpoints matching teammate Edge Station and Mobile App JSON request body contracts.
- **Access:** Public / Authenticated
- **Request Body:**
  ```json
  {
    "userId": "usr_44",
    "scooterId": "sc_101"
  }
  ```
- **Response:** `200 OK` (`ScooterReservationDto`)

---

### 3.3. Administration & Fleet Management Endpoints

#### `GET /api/v1/admin/fleet-overview`
Returns global platform metrics and status of all charging stations.
- **Access:** `ROLE_ADMIN`
- **Response:** `200 OK`
  ```json
  {
    "totalStations": 3,
    "totalScooters": 50,
    "availableScooters": 42,
    "activeRides": 5,
    "maintenanceScooters": 3,
    "stations": [
      {
        "stationId": "station_a",
        "name": "Victory Square Station",
        "lat": 45.7538,
        "lon": 21.2257,
        "capacity": 15,
        "dockedCount": 8,
        "port": 8084,
        "status": "ACTIVE",
        "dockerContainerId": "d8b3c9f1a234"
      }
    ]
  }
  ```

#### `GET /api/v1/admin/scooters/active`
Lists all scooters currently in `IN_USE` / `MOVING` / `PAUSED` state.
- **Access:** `ROLE_ADMIN`
- **Response:** `200 OK`
  ```json
  [
    {
      "scooterId": "sc_101",
      "model": "Ninebot Max G2",
      "status": "MOVING",
      "batteryLevel": 78,
      "activeRideId": "rd_7712",
      "userId": "usr_44",
      "managingStationId": "station_a",
      "managingStationUrl": "http://station-a:8084",
      "startTime": "2026-11-16T11:20:00Z"
    }
  ]
  ```

#### `GET /api/v1/admin/scooters/{id}/live-location`
On-demand query for an active scooter's live telemetry, proxied from its managing edge station.
- **Access:** `ROLE_ADMIN`
- **Path Variable:** `id` (e.g. `sc_101`)
- **Response:** `200 OK`
  ```json
  {
    "scooterId": "sc_101",
    "status": "MOVING",
    "managingStation": "station_a",
    "lat": 45.7512,
    "lon": 21.2280,
    "batteryLevel": 77,
    "speedKmh": 18.4,
    "lastPingTimestamp": "2026-11-16T11:22:45Z"
  }
  ```

#### `POST /api/v1/admin/stations/spawn`
Dynamically provisions and boots a new edge station container via Docker.
- **Access:** `ROLE_ADMIN`
- **Request Body:**
  ```json
  {
    "stationId": "station_c",
    "name": "Union Square Station",
    "lat": 45.7580,
    "lon": 21.2310,
    "capacity": 20,
    "port": 8086
  }
  ```
- **Response:** `201 Created`
  ```json
  {
    "stationId": "station_c",
    "dockerContainerId": "a1f9450bc321e847",
    "status": "ACTIVE",
    "message": "Station container spawned and registered successfully."
  }
  ```

#### `POST /api/v1/admin/scooters/transfer`
Manually relocates stationary scooters between stations. Emits Kafka event on `admin-transfer-commands`.
- **Access:** `ROLE_ADMIN`
- **Request Body:**
  ```json
  {
    "scooterId": "sc_101",
    "fromStationId": "station_a",
    "toStationId": "station_b",
    "reason": "Battery charging consolidation"
  }
  ```
- **Response:** `200 OK`
  ```json
  {
    "status": "TRANSFER_INITIATED",
    "scooterId": "sc_101",
    "targetStationId": "station_b"
  }
  ```

---

### 3.4. AI Rebalancing & Analytics Endpoints

#### `GET /api/v1/rebalance/pending`
Retrieves pending fleet rebalancing proposals submitted by the autonomous AI agent or operators.
- **Access:** `ROLE_ADMIN` / `ROLE_SYSTEM`
- **Response:** `200 OK`
  ```json
  [
    {
      "taskId": "reb_9921",
      "sourceStationId": "station_a",
      "targetStationId": "station_b",
      "scooterIds": ["sc_101", "sc_104"],
      "status": "PROPOSED",
      "proposedBy": "AUTONOMOUS_AGENT",
      "justification": "Station A has 14/15 scooters; Station B projected deficit of 6 for 08:00 AM rush.",
      "createdAt": "2026-11-16T03:00:00Z"
    }
  ]
  ```

#### `POST /api/v1/rebalance/{id}/approve`
Approves a pending rebalance proposal and dispatches relocation commands via Kafka.
- **Access:** `ROLE_ADMIN`
- **Path Variable:** `id` (e.g. `reb_9921`)
- **Response:** `200 OK`
  ```json
  {
    "taskId": "reb_9921",
    "status": "APPROVED",
    "approvedAt": "2026-11-16T11:25:00Z"
  }
  ```

#### `POST /api/v1/rebalance/propose`
Used by the MCP Gateway or AI Agent to submit a new rebalance proposal.
- **Access:** `ROLE_ADMIN` / `ROLE_SYSTEM`
- **Request Body:**
  ```json
  {
    "sourceStationId": "station_a",
    "targetStationId": "station_b",
    "scooterIds": ["sc_101"],
    "justification": "Predicted morning deficit"
  }
  ```
- **Response:** `201 Created`
  ```json
  {
    "taskId": "reb_9922",
    "status": "PROPOSED"
  }
  ```

#### `GET /api/v1/analytics/demand`
Provides historical average and projected scooter demand for a station.
- **Access:** `ROLE_ADMIN` / `ROLE_SYSTEM`
- **Query Parameters:** `stationId` (string), `targetHour` (0-23)
- **Response:** `200 OK`
  ```json
  {
    "stationId": "station_a",
    "targetHour": 8,
    "predictedDemand": 25,
    "historicalAvg": 22
  }
  ```

---

### 3.5. Ride History & Audit Endpoints

#### `GET /api/v1/rides/history`
Returns completed rides with full details, fare breakdown, and parking compliance flags.
- **Access:** `ROLE_ADMIN`
- **Response:** `200 OK` Array of Ride objects.

---

### 3.6. Remote MCP (Model Context Protocol) Server Gateway Endpoints

To enable autonomous AI agents (such as the team's `fleet-mcp-server` on port 8083 or Antigravity agents) to inspect and control the micro-mobility system, the Central Server exposes dedicated REST endpoints corresponding directly to the MCP tools defined in the project architecture:

#### Tool 1: `find_available_scooters` -> `GET /api/v1/scooters/nearby`
- Accepts `latitude`, `longitude`, `radiusMeters`.
- Queries Redis 7 geospatial index and returns nearest scooters with their assigned station connection details.

#### Tool 2: `book_scooter_for_user` -> `POST /api/v1/mcp/book-scooter`
Reserves a scooter for a specific user.
- **Access:** Public / Authenticated / MCP
- **Request Body:**
  ```json
  {
    "userId": "usr_mcp_42",
    "scooterId": "sc_101"
  }
  ```
- **Response:** `200 OK` (`ScooterReservationDto`)
  ```json
  {
    "scooterId": "sc_101",
    "status": "LOCKED_FOR_USER",
    "reservedForUserId": "usr_mcp_42",
    "stationId": "station_a",
    "stationUrl": "http://station_a:8084",
    "stationPort": 8084,
    "reservationExpiresInSeconds": 3600,
    "message": "Scooter sc_101 successfully reserved and locked. Connect to station for ride initiation."
  }
  ```

#### Tool 3: `get_fleet_and_stations_status` -> `GET /api/v1/mcp/fleet-status`
Returns high-level holistic intelligence combining Redis cache metrics and PostgreSQL state. Includes station dock occupancy, surplus/deficit vs target thresholds, docked scooter ID arrays, and overall fleet availability.
- **Access:** Public / Authenticated / MCP
- **Response:** `200 OK`
  ```json
  {
    "totalStations": 4,
    "totalScooters": 11,
    "availableScooters": 11,
    "activeRides": 0,
    "stations": [
      {
        "stationId": "station_a",
        "name": "Victory Square Station",
        "capacity": 15,
        "dockedCount": 4,
        "occupancyPercent": 26.7,
        "surplusOrDeficit": -3,
        "dockedScooterIds": ["sc_101", "sc_102", "sc_103", "sc_104"]
      }
    ],
    "scooters": [
      {
        "scooterId": "sc_101",
        "model": "Ninebot Max G2",
        "batteryLevel": 92,
        "status": "AVAILABLE",
        "stationId": "station_a"
      }
    ],
    "systemHealthy": true,
    "timestamp": "2026-11-16T19:20:00Z"
  }
  ```

#### Tool 4: `propose_fleet_rebalancing` -> `POST /api/v1/rebalance/propose`
Enables autonomous agents to submit formal rebalancing directives (`sourceStationId`, `targetStationId`, `scooterIds`, `justification`) for human operator review and automated dispatch.

---

### 3.7. Redis In-Memory Datastore & Statistical Reporting APIs

To allow operators, external MCP servers, and monitoring agents to directly query the state of the Redis in-memory cache and aggregate rich operational reports without manual SQL/CLI access:

#### `GET /api/v1/reports/redis-geospatial`
Direct inspection of Redis 7 database keys and geospatial sets.
- **Access:** Public / Authenticated / Operators
- **Response:** `200 OK`
  ```json
  {
    "keyName": "fleet:stationary:geo",
    "totalIndexedScooters": 11,
    "indexedScooters": [
      {
        "scooterId": "sc_101",
        "model": "Ninebot Max G2",
        "batteryLevel": 92,
        "status": "AVAILABLE",
        "lat": 45.7540,
        "lon": 21.2259,
        "stationId": "station_a",
        "stationName": "Victory Square Station",
        "stationUrl": "http://station_a:8084",
        "stationPort": 8084,
        "distanceMeters": 0
      }
    ],
    "metadataKeysCount": 11,
    "status": "HEALTHY",
    "queryTimestamp": "2026-11-16T19:20:26Z"
  }
  ```

#### `GET /api/v1/reports/station-occupancy`
Aggregates live charging dock utilization, total capacity, deficits, and docked scooter rosters across all operational edge nodes.
- **Access:** Public / Authenticated
- **Response:** `200 OK`
  ```json
  {
    "totalStations": 4,
    "totalCapacity": 65,
    "totalDocked": 11,
    "averageOccupancyPercent": 16.9,
    "stations": [
      {
        "stationId": "station_a",
        "name": "Victory Square Station",
        "capacity": 15,
        "dockedCount": 4,
        "occupancyPercent": 26.7,
        "status": "ACTIVE",
        "surplusOrDeficit": -3,
        "dockedScooterIds": ["sc_101", "sc_102", "sc_103", "sc_104"],
        "full": false,
        "empty": false
      }
    ]
  }
  ```

#### `GET /api/v1/reports/fleet-summary`
Comprehensive platform analytics combining PostgreSQL persistent SSoT records with live Redis cache telemetry.
- **Access:** Public / Authenticated
- **Response:** `200 OK`
  ```json
  {
    "totalScooters": 11,
    "availableScooters": 11,
    "reservedScooters": 0,
    "movingScooters": 0,
    "maintenanceScooters": 0,
    "averageBatteryPercent": 82.0,
    "minBatteryPercent": 54,
    "maxBatteryPercent": 96,
    "lowBatteryAlertCount": 0,
    "batteryDistribution": {
      "80-100%": 7,
      "50-79%": 4,
      "20-49%": 0,
      "<20%": 0
    },
    "totalRides": 0,
    "activeRides": 0,
    "completedRides": 0,
    "totalRevenueCents": 0,
    "totalRevenueEur": 0.0,
    "parkingCompliancePercent": 100.0,
    "generatedAt": "2026-11-16T19:20:27Z"
  }
  ```

---

## 4. Kafka Topics & Messaging Schemas

Broker Port: `9092`  
Internal Docker Listener: `kafka-broker:29092`  
External Host Listener: `localhost:9092`  
Serialization: Standard JSON UTF-8

### 4.1. Topics Consumed by Central Server

#### Topic: `ride-lifecycle-events` (3 partitions)
Edge station nodes emit events here when rides begin, end, or get cancelled. The consumer in `FleetEventConsumer.java` supports both nested (`{"eventType": "...", "payload": {...}}`) and flat JSON structures, as well as polymorphic field names:

1. **`fleet.ride-started` / `RIDE_STARTED`**
   - **Central Server Action:**
     - Sets scooter `current_status = 'IN_USE'` in PostgreSQL.
     - Removes scooter from Redis `fleet:stationary:geo` using `ZREM`.
     - Decrements station's docked counter.
   - **Schema:**
     ```json
     {
       "eventId": "c7a10f82-3d52-47bc-9b63-12c8a1438901",
       "eventType": "RIDE_STARTED",
       "timestamp": "2026-11-16T11:20:00Z",
       "payload": {
         "rideId": "rd_7712",
         "scooterId": "sc_101",
         "userId": "usr_44",
         "stationId": "station_a",
         "startLatitude": 45.7538,
         "startLongitude": 21.2257,
         "batteryLevel": 88
       }
     }
     ```

2. **`fleet.ride-ended` / `RIDE_ENDED`**
   - **Central Server Action:**
     - Persists completed ride in PostgreSQL `rides` (including fare, duration, route trace, parking compliance verdict).
     - Updates scooter in PostgreSQL (`current_lat`, `current_lon`, `battery_level`, `current_status = 'AVAILABLE'`, `assigned_station_id`).
     - Adds scooter back into Redis `fleet:stationary:geo` via `GEOADD`.
     - Supports `dockedStationId` or `destinationStationId`, `routeTrace` or `routePoints`, and `parkingCompliance.compliant`.
   - **Schema:**
     ```json
     {
       "eventId": "e9b21f93-4e63-58cd-ac74-23d9b2549012",
       "eventType": "RIDE_ENDED",
       "timestamp": "2026-11-16T11:45:00Z",
       "payload": {
         "rideId": "rd_7712",
         "scooterId": "sc_101",
         "userId": "usr_44",
         "endLatitude": 45.7489,
         "endLongitude": 21.2391,
         "batteryLevel": 74,
         "fareCents": 1850,
         "durationSeconds": 1500,
         "distanceMeters": 3200,
         "dockedStationId": "station_b",
         "photoUrl": "http://minio:9000/photos/rd_7712.jpg",
         "isCompliantParking": true,
         "parkingViolationReason": null,
         "routeTrace": [
           {"lat": 45.7538, "lon": 21.2257, "timestamp": 1731756000},
           {"lat": 45.7512, "lon": 21.2280, "timestamp": 1731756600},
           {"lat": 45.7489, "lon": 21.2391, "timestamp": 1731757500}
         ]
       }
     }
     ```

#### Topic: `station-handover-events` (3 partitions)
Edge nodes notify the system when a scooter is docked, undocked, or when a handover is requested.
- **Accepted Events:** `fleet.scooter-docked`, `SCOOTER_DOCKED`, `HANDOVER_REQUEST`, `station.handover-request`, `fleet.handover-completed`.
- **Central Server Action:**
  - Updates scooter's `assigned_station_id = targetStationId` in PostgreSQL.
  - Updates Redis metadata `fleet:scooter:{id}:meta`.

#### Topic: `scooter-telemetry-topic` (3 partitions)
High-frequency (3-second) GPS and IoT telemetry broadcast by in-transit scooters or edge station gateways.
- **Consumer Group:** `central-server-telemetry-group`
- **Central Server Action:**
  - Ingests telemetry directly into Redis Hash key `fleet:scooter:{id}:live` with a 5-minute (300s) TTL.
  - Completely bypasses PostgreSQL to prevent relational database write saturation during rush hour.
  - Serves live locations on-demand via `GET /api/v1/admin/scooters/{id}/live-location`.
- **Schema:**
  ```json
  {
    "scooterId": "sc_101",
    "latitude": 45.7525,
    "longitude": 21.2270,
    "speedKmh": 18.4,
    "batteryLevel": 85,
    "status": "MOVING",
    "timestamp": "2026-11-16T19:20:00Z"
  }
  ```

---

### 4.2. Topics Produced by Central Server

#### Topic: `admin-transfer-commands` (3 partitions)
Emitted when an operator or an approved AI rebalancing task executes.
- **Schema:**
  ```json
  {
    "commandId": "cmd_4412",
    "commandType": "DISPATCH_REBALANCE",
    "timestamp": "2026-11-16T11:30:00Z",
    "sourceStationId": "station_a",
    "targetStationId": "station_b",
    "scooterIds": ["sc_101", "sc_104"],
    "taskId": "reb_9921"
  }
  ```
    "scooterIds": ["sc_101", "sc_104"],
    "taskId": "reb_9921"
  }
  ```

---

## 5. PostgreSQL 16 Database Schema (SSoT)

```sql
-- Users and authentication
CREATE TABLE users (
    user_id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(64) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'ROLE_USER',
    full_name VARCHAR(128) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Operational Urban Zones
CREATE TABLE zones (
    zone_id VARCHAR(32) PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    min_capacity INT NOT NULL DEFAULT 5,
    target_capacity INT NOT NULL DEFAULT 20,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Charging Stations (Edge Nodes)
CREATE TABLE charging_stations (
    station_id VARCHAR(32) PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lon DOUBLE PRECISION NOT NULL,
    capacity INT NOT NULL DEFAULT 15,
    port INT NOT NULL DEFAULT 8084,
    docker_container_id VARCHAR(64),
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, MAINTENANCE, OFFLINE
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Scooter Fleet
CREATE TABLE scooters (
    scooter_id VARCHAR(32) PRIMARY KEY,
    model VARCHAR(32) NOT NULL,
    battery_level INT NOT NULL CHECK (battery_level BETWEEN 0 AND 100),
    current_status VARCHAR(24) NOT NULL, -- AVAILABLE, RESERVED, MOVING, PAUSED, LOW_BATTERY, MAINTENANCE
    current_lat DOUBLE PRECISION NOT NULL,
    current_lon DOUBLE PRECISION NOT NULL,
    assigned_station_id VARCHAR(32) REFERENCES charging_stations(station_id),
    assigned_zone_id VARCHAR(32) REFERENCES zones(zone_id),
    version BIGINT NOT NULL DEFAULT 0, -- Optimistic Locking
    last_maintenance TIMESTAMP WITH TIME ZONE
);

-- Completed and Historical Rides
CREATE TABLE rides (
    ride_id VARCHAR(32) PRIMARY KEY,
    scooter_id VARCHAR(32) NOT NULL REFERENCES scooters(scooter_id),
    user_id VARCHAR(36) NOT NULL REFERENCES users(user_id),
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE,
    start_lat DOUBLE PRECISION NOT NULL,
    start_lon DOUBLE PRECISION NOT NULL,
    end_lat DOUBLE PRECISION,
    end_lon DOUBLE PRECISION,
    photo_url VARCHAR(255),
    is_compliant_parking BOOLEAN,
    parking_violation_reason VARCHAR(255),
    fare_cents INT,
    route_trace JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Autonomous & Operator Rebalance Tasks
CREATE TABLE rebalance_tasks (
    task_id VARCHAR(32) PRIMARY KEY,
    source_station_id VARCHAR(32) NOT NULL REFERENCES charging_stations(station_id),
    target_station_id VARCHAR(32) NOT NULL REFERENCES charging_stations(station_id),
    scooter_ids TEXT[] NOT NULL,
    status VARCHAR(24) NOT NULL, -- PROPOSED, APPROVED, IN_PROGRESS, COMPLETED, REJECTED
    proposed_by VARCHAR(32) NOT NULL,
    justification TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

---

## 6. Redis 7 Key Conventions

1. **`fleet:stationary:geo`** (Redis Geospatial Sorted Set)
   - Primitives: `GEOADD fleet:stationary:geo <lon> <lat> <scooter_id>`
   - Queries: `GEORADIUS fleet:stationary:geo <lon> <lat> <radius> m WITHDIST WITHCOORD`
   - Removals: `ZREM fleet:stationary:geo <scooter_id>` on ride start or reservation.
2. **`fleet:scooter:{id}:meta`** (Redis Hash)
   - Fields: `model`, `batteryLevel`, `status`, `stationId`, `stationName`, `stationPort`, `stationUrl`
   - Updated whenever scooter attributes or station handovers occur.
3. **`fleet:scooter:{id}:live`** (Redis Hash - Real-time Telemetry Cache)
   - Fields: `latitude`, `longitude`, `speedKmh`, `batteryLevel`, `status`, `timestamp`
   - Populated automatically by Kafka consumer listening to `scooter-telemetry-topic`.
   - Set with a 5-minute (300 seconds) sliding TTL. In-transit telemetry stays in-memory to prevent relational database thrashing.

---

## 7. Multi-Container Orchestration (`docker-compose.yml`)

The platform is orchestrated as follows:
- **`postgres-db` (Port 5432):** Persistent storage volume `postgres_data`.
- **`redis-cache` (Port 6379):** In-memory cache volume `redis_data`.
- **`kafka-broker` (Port 9092):** Confluent Kafka in KRaft mode (`29092` internal, `9092` external).
- **`central-server` (Port 8081):** Spring Boot backend mounting `/var/run/docker.sock`.
- **`admin-frontend` (Port 3010 / 8081):** React application dashboard.
