# ⚡ Urban Micro-Mobility — Central Fleet Management System

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://react.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red.svg)](https://redis.io/)
[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-7.6-black.svg)](https://kafka.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg)](https://www.docker.com/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind-CSS-38B2AC.svg)](https://tailwindcss.com/)

> An enterprise-grade, distributed micro-mobility management platform orchestrating urban e-scooter fleets, charging station edge nodes, IoT telemetry, in-memory geospatial discovery, and operational analytics.

---

## 🏛️ System Architecture

The platform follows a **hierarchical distributed systems architecture** designed to decouple high-frequency edge IoT telemetry from global authoritative business transactions:

```
                          ┌──────────────────────────────────────────────┐
                          │            Android Mobile Client             │
                          │                 (Rider App)                  │
                          └─────────────┬───────────────────┬────────────┘
                                        │                   │
                  1. Discover Nearby    │                   │ 2. Rent Scooter
                  Stationary Scooters   │                   │    (Scan QR / RFID)
                                        ▼                   ▼
     ┌────────────────────────────────────┐       ┌────────────────────────────────────┐
     │      Central Management Server     │       │    Edge Charging Station Server    │
     │       (Java 21 / Spring Boot 3)    │       │     (Local SQLite / Docker Node)   │
     │      Port: 8081 | Admin UI: 3010   │       │         Port: 8084, 8085...        │
     └─────────┬──────────────┬───────────┘       └─────────┬────────────────┬─────────┘
               │              │                             │                │
               │ Synchronizes │ Sub-ms                      │ 3s Live GPS    │ High-Freq
               │ Relational   │ Geospatial                  │ Telemetry      │ IoT Control
               │ SSoT         │ Cache                       │                │
               ▼              ▼                             ▼                ▼
     ┌──────────────┐   ┌──────────────┐          ┌───────────────────┐  ┌───────────────┐
     │  PostgreSQL  │   │ Redis 7 GEO  │          │   Active Moving   │  │    Scooter    │
     │   Database   │   │  (In-Memory) │          │    E-Scooters     │  │   Hardware    │
     │  (Port 5432) │   │ (Port 6379)  │          │    (In-Transit)   │  │   Simulator   │
     └──────────────┘   └──────────────┘          └───────────────────┘  └───────────────┘
               ▲                                            │
               │          Apache Kafka Message Broker       │
               │                  (Port 9092)               │
               └────────────────────────────────────────────┘
                   Topics: station-handover-events
                           ride-lifecycle-events
                           admin-transfer-commands
```

---

## 🌟 Core Architectural Invariants

1. **Authoritative Single Source of Truth (SSoT):**
   - **PostgreSQL 16** serves as the authoritative persistence datastore for registered users, fleet inventory, charging stations, operating zones, rides, and maintenance/rebalance tasks.
   - Optimistic concurrency control (`@Version`) prevents race conditions during concurrent dock handovers and reservations.

2. **Sub-Millisecond Geospatial Discovery:**
   - **Redis 7** maintains in-memory spatial indexes (`fleet:stationary:geo`) using native `GEOADD` and `GEORADIUS` primitives.
   - Mobile clients discovering nearby scooters (`GET /api/v1/scooters/nearby`) query Redis directly, achieving sub-millisecond response times without database query overhead.

3. **Decentralized High-Frequency Telemetry:**
   - High-frequency GPS telemetry (3-second intervals) from in-transit scooters is ingested and retained locally by the **Edge Charging Station** managing the active ride.
   - The Central Server is deliberately insulated from high-frequency stream bottlenecks, maintaining only active session descriptors and executing on-demand live inspections (`GET /api/v1/admin/scooters/{id}/live-location`) when requested by fleet operators.

4. **Event-Driven Handover Protocol:**
   - All lifecycle transitions (`RIDE_STARTED`, `RIDE_ENDED`, `SCOOTER_DOCKED`, `SCOOTER_UNDOCKED`) stream asynchronously across **Apache Kafka** (KRaft mode).
   - The Central Server consumes these events to update PostgreSQL state, sync Redis geospatial indexes, and calculate trip durations and fees.

5. **Dynamic Edge Station Orchestration:**
   - The Central Server interfaces directly with the host Docker engine via `/var/run/docker.sock`, enabling fleet operators to provision, relocate, and deploy new edge station containers on demand via the web dashboard.

---

## 📁 Repository & Branch Structure

This project is partitioned into distinct branches corresponding to team responsibilities:

| Branch Name | Primary Component | Technology Stack | Responsibility |
| :--- | :--- | :--- | :--- |
| **`main`** | **Unified Production Trunk** | Full Platform | Merged baseline with all services orchestrated via Docker Compose. |
| **`Central_Server_+_Database`** | **Central Server & Datastores** | Java 21, Spring Boot, React, PostgreSQL, Redis, Kafka | Authoritative SSoT, Redis GEO cache, Kafka broker, REST APIs, and Admin Dashboard. |
| **`Session_Server_+_Scooter_Simulator`** | **Edge Stations & Scooter IoT** | Java, SQLite, IoT Simulator, Docker | Local charging dock controllers, 3s GPS telemetry processing, and scooter firmware simulator. |
| **`mobile-app`** | **Mobile Rider Client** | Kotlin / Android | Native mobile application for user registration, QR code unlocking, and ride tracking. |

---

## 🚢 Service Directory & Port Mapping

When deployed via `docker compose`, the ecosystem exposes the following ports:

| Service Name | Container Name | Port Mapping | Protocol / Purpose |
| :--- | :--- | :--- | :--- |
| **Admin Web UI** | `admin-frontend` | `3010:80` | Production React SPA served via Nginx reverse proxy. |
| **Central Management Server** | `central-server` | `8081:8081` | Spring Boot REST API & Swagger UI documentation. |
| **PostgreSQL Datastore** | `postgres-db` | `5432:5432` | SSoT Relational Database (`fleet_db`). |
| **Redis Cache** | `redis-cache` | `6379:6379` | In-memory spatial index (`fleet:stationary:geo`). |
| **Apache Kafka Broker** | `kafka-broker` | `9092:9092` | Event streaming broker (Internal: `29092`, Host: `9092`). |
| **Edge Station A** *(Colleague)* | `station_a` | `8084:8084` | Victory Square Charging Dock Controller. |
| **Edge Station B** *(Colleague)* | `station_b` | `8085:8085` | Central Park Charging Dock Controller. |
| **Edge Station C** *(Colleague)* | `station_c` | `8086:8086` | Union Square Charging Dock Controller. |
| **Edge Station D** *(Colleague)* | `station_d` | `8087:8087` | Bega Promenade Charging Dock Controller. |

---

## 🚀 Quickstart Guide

### Prerequisites
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (v24+ with Compose v2)
- Git

### 1. Clone & Start Containers
```bash
# Clone the repository
git clone https://github.com/tobiastoica/PCBE.git
cd PCBE

# Launch all central infrastructure and services in detached mode
docker compose up -d
```

### 2. Verify Container Health
```bash
docker ps
```
All 5 containers (`admin-frontend`, `central-server`, `postgres-db`, `redis-cache`, `kafka-broker`) should show status `Up` or `Up (healthy)`.

### 3. Access Web Services
- **Admin & Fleet Dashboard:** [http://localhost:3010](http://localhost:3010)
- **Interactive Swagger UI:** [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
- **OpenAPI Schema:** [http://localhost:8081/v3/api-docs](http://localhost:8081/v3/api-docs)
- **Actuator Health Check:** [http://localhost:8081/actuator/health](http://localhost:8081/actuator/health)

---

## 🔑 Pre-Seeded Credentials

### Human Web UI Authentication
| Username | Password | Role | Permissions |
| :--- | :--- | :--- | :--- |
| `admin` | `admin123` | `ROLE_ADMIN` | Full fleet oversight, spawning, rebalancing, relocate commands. |
| `rider` | `rider123` | `ROLE_USER` | Mobile scooter discovery, rental initiation, ride history. |

### Inter-Service API Key Authentication
For Edge Stations and external IoT microservices communicating with the Central Server:
```http
X-API-KEY: fleet-secret-key-2026
```

---

## 🖥️ Web UI Dashboard Features

The React administration dashboard ([http://localhost:3010](http://localhost:3010)) offers 5 dedicated views:

1. **🗺️ Interactive Fleet Map:**
   - High-performance Leaflet map centered on Timișoara, Romania.
   - **🌙 Dark Mode / ☀️ Streets Mode** live toggle.
   - Real-time station dock occupancy badges (`⚡ 4/15`, `⚡ 2/15`, etc.).
   - Available stationary scooter markers colored dynamically by battery level (green `≥80%`, amber `50-79%`, red `<50%`).
   - Interactive detail inspection drawer displaying battery, distance, model, and assigned station.

2. **🛴 Active Fleet Monitor:**
   - Real-time registry of ongoing rides and in-transit scooters.
   - On-demand inspection button querying the managing Edge Station for live GPS telemetry.

3. **⚡ Stations & Infrastructure:**
   - Real-time dock capacity, online status, and container IDs for each charging station.
   - **Relocate Scooter Modal:** Transfer docked scooters between stations.
   - **Spawn Station Modal:** Dynamically orchestrate and provision new Docker station containers via `/var/run/docker.sock`.

4. **🤖 AI Rebalancing Hub:**
   - Machine learning demand forecasting for urban zones.
   - Automated rebalance proposals indicating source/target stations and surplus vehicle units.
   - One-click operator approval workflows.

5. **📜 Ride History & Auditing:**
   - Comprehensive log of completed trips, start/end timestamps, distance traveled, cost calculations, and parking compliance verdicts.

---

## 📡 Key REST APIs & Kafka Topics

For comprehensive API contracts, request/response JSON schemas, and error codes, refer to [CENTRAL_SERVER_SPECIFICATION.md](file:///C:/Users/Tobi/PCBE/CENTRAL_SERVER_SPECIFICATION.md).

### Rider & Discovery Endpoints
- `GET /api/v1/scooters/nearby` — Sub-millisecond Redis geospatial discovery (`?lat=45.7538&lon=21.2257&radiusMeters=2000`). Returns scooter status and managing station network address (`stationUrl: http://station_a:8084`, `stationPort: 8084`).
- `GET /api/v1/scooters/{id}` — Query specific scooter hardware and its assigned station gateway for direct mobile app to station communication.
- `POST /api/v1/scooters/{id}/reserve` — Reserve and lock scooter for 1 hour (`LOCKED_FOR_USER`), temporarily evicting it from Redis `fleet:stationary:geo` to prevent double-booking.
- `POST /api/v1/scooters/{id}/cancel-reserve` — Cancel reservation, unlocking scooter and restoring it to Redis discovery.
- `POST /api/v1/rides/reserve` & `POST /api/v1/rides/reserve/cancel` — Direct edge station and mobile app payload contract (`{ "userId": "...", "scooterId": "..." }`).

### Remote MCP Server & AI Agent Gateway (Tools 1–4)
- `GET /api/v1/mcp/fleet-status` — **MCP Tool 3 (`get_fleet_and_stations_status`)**: Returns complete station occupancy, surplus/deficit vs target thresholds, docked scooter IDs, and fleet summary.
- `POST /api/v1/mcp/book-scooter` — **MCP Tool 2 (`book_scooter_for_user`)**: Locks and books a scooter on behalf of an AI-assisted rider.
- `POST /api/v1/rebalance/propose` — **MCP Tool 4 (`propose_fleet_rebalancing`)**: Submits automated rebalance proposals for operator approval.
- `GET /api/v1/scooters/nearby` — **MCP Tool 1 (`find_available_scooters`)**: In-memory Redis proximity search.

### Redis Datastore & Operational Analytics Reports
- `GET /api/v1/reports/redis-geospatial` — Direct inspection of Redis in-memory key `fleet:stationary:geo`, listing all currently indexed available scooters and distances.
- `GET /api/v1/reports/station-occupancy` — Real-time charging dock analytics, percentage utilization, deficits/surpluses, and docked scooter arrays.
- `GET /api/v1/reports/fleet-summary` — Multi-datastore statistical report (battery distribution histogram, total rides, revenue, parking compliance rate).

### Administration Endpoints
- `POST /api/v1/auth/login` — Authenticate and receive JWT.
- `GET /api/v1/admin/fleet-overview` — Global fleet status, active rides, and station occupancy.
- `POST /api/v1/admin/scooters/transfer` — Relocate a scooter between charging docks.
- `POST /api/v1/admin/stations/spawn` — Provision a new station container via Docker socket.
- `GET /api/v1/admin/scooters/{id}/live-location` — Query Edge Station or Redis live cache for in-transit telemetry.
- `GET /api/v1/rebalance/pending` — Retrieve pending AI rebalancing proposals.
- `POST /api/v1/rebalance/{id}/approve` — Approve a fleet rebalance task and dispatch via Kafka.
- `GET /api/v1/rides/history` — Audit log of completed rides and parking compliance flags.

### Kafka Event Streaming Topics
- `ride-lifecycle-events` (3 partitions) — Ingests `RIDE_STARTED` and `RIDE_ENDED` (with route points and parking compliance).
- `station-handover-events` (3 partitions) — Ingests `SCOOTER_DOCKED`, `HANDOVER_REQUEST`, and undock events.
- `scooter-telemetry-topic` (3 partitions) — Ingests high-frequency 3s GPS telemetry directly into Redis `fleet:scooter:{id}:live` with 5-minute sliding TTL, shielding PostgreSQL.
- `admin-transfer-commands` (3 partitions) — Dispatches administrative transfer orders to logistics dispatchers.

---

## 🧪 Testing & Verification

The Central Server test suite includes unit, integration, and security verification:

```bash
# Run Maven tests in central-server
cd central-server
./mvnw test
```

- **`DatabaseEntityTest`:** Validates JPA relational mapping, foreign keys, and `@Version` optimistic concurrency locking.
- **`AuthSecurityTest`:** Verifies JWT token generation, role-based endpoint authorization (`ROLE_ADMIN` vs `ROLE_USER`), and `X-API-KEY` filter authentication.
- **`ScooterDiscoveryTest`:** Verifies Redis geospatial distance calculations and zone demand analytics.

---

## 👥 Authors & University Collaboration

- **Central Server & Database Lead:** Tobias Toica (Authoritative datastores, Redis cache, Kafka broker, Spring Boot backend, React Admin UI).
- **Session Server & Simulator Lead:** Edge charging stations, SQLite edge storage, and Scooter IoT simulator.
- **Mobile Client Lead:** Android Kotlin mobile application.

*Developed as part of the PCBE (Parallel & Concurrent Programming / Distributed Systems) curriculum, 2026.*
