# Specificații Detaliate de Arhitectură și Implementare: Sistem Distribuit de Gestionare a unei Flote de Trotinete Electrice
## Platformă de Micro-Mobilitate Urbană și Rebalansare Autonomă (Documentație Tehnică Completă)

---

## 1. Prezentare Generală și Rezumat Executiv

Platforma de **Micro-Mobilitate Urbană și Rebalansare Autonomă** reprezintă un sistem distribuit orientat pe evenimente (*event-driven*), conceput pentru administrarea la scară urbană a unei flote de trotinete electrice. Sistemul soluționează provocările fundamentale ale sistemelor distribuite moderne: mutații concurente de stare, interogări geospațiale de mare viteză cu latență redusă, consecvență eventuală între depozite de date decuplate, procesare la marginea rețelei (*Edge Computing*), inspecție automată prin inteligență artificială multimodală și rebalansare autonomă coordonată prin agenți inteligenți.

Arhitectura combină un **Server Central de Management** (care acționează ca autoritate unică de date și orchestrator de infrastructură), **Noduri Stații de Încărcare la Marginea Rețelei (Edge Services)** (care preiau gestiunea sesiunilor active de deplasare și a telemetriei locale), un **Simulator IoT** bazat pe fire de execuție virtuale, un **Server MCP (Model Context Protocol)** și un **Agent AI Autonom** pentru operațiuni logistice nocturne.

### Repere Arhitecturale Cheie
- **Procesare la Marginea Rețelei (Edge / Fog Computing):** Degrevarea Serverului Central în timpul curselor active. Telemetria GPS la 3 secunde și starea de pauză sunt procesate exclusiv de nodurile locale (Stațiile de Încărcare), traficul fiind descentralizat.
- **Sursă Unică de Adevăr (Single Source of Truth - SSoT):** O singură bază de date relațională autoritară (PostgreSQL 16) administrată exclusiv de Serverul Central. Niciun alt nod nu deține drepturi de scriere directă în baza persistentă.
- **Indexare Geospațială Rapidă în Memorie:** Utilizarea Redis 7 cu primitive native geospațiale (`GEO`) pentru menținerea poziționării trotinetelor staționare și maparea apartenenței la stații, oferind interogări de proximitate cu latență sub-milisecundă.
- **Streaming Asincron de Evenimente:** Apache Kafka asigură decuplarea asincronă a componentelor, transmițând evenimentele de ciclu de viață (`ride-started`, `ride-ended`, `station-handover`, `admin-transfer`) și telemetria hardware.
- **Client Mobil Nativ la Margine:** Aplicație mobilă Android dezvoltată cu Jetpack Compose și Kotlin, integrată cu senzori de geolocație de precizie (`FusedLocationProviderClient`) și subsistemul camerei foto (`CameraX`) pentru capturarea dovezii de parcare la finalul cursei.
- **Inspecție Vizuală Multimodală cu AI:** Evaluarea conformității parcării la finalul cursei prin intermediul unui model de viziune artificială (Google Gemini Flash Vision), verificând automat respectarea normelor municipale (blocarea trotuarelor, obstrucționarea rampelor de acces pentru persoane cu dizabilități).
- **Guvernanță Agentic AI prin Server Remote MCP:** Un server dedicat Model Context Protocol care expune unelte standardizate (JSON-RPC 2.0) prin transport HTTP/SSE, permițând unui Agent AI autonom să inspecteze inventarul flotei și să execute transferuri optime de rebalansare fără intervenție umană directă.
- **Orchestrare Containerizată și Instanțiere Dinamică:** Întreaga arhitectură este containerizată în Docker. Serverul Central integrează socket-ul Docker al host-ului (`/var/run/docker.sock`) pentru a instanția dinamic, la cererea administratorului, noi noduri de stații de încărcare.

---

## 2. Arhitectura Distribuită a Sistemului

Diagrama structurală de mai jos ilustrează interconectarea tuturor componentelor, canalele de comunicație și fluxurile de date din cadrul platformei:

```
                                   +------------------------------------+
                                   |       Client Application           |
                                   |   (Mobile App / User Web UI)       |
                                   +--------+------------------+--------+
                                            |                  |
            [Phase 1: Discovery &           |                  | [Phase 2: Booking to Ride End]
             Phase 3: Post-Ride / History]  |                  | - POST /rides/reserve (Booking)
            - GET /api/v1/scooters/nearby   |                  | - POST /rides/start
            - View Past Rides / Profile     |                  | - GET /rides/{id}/stream (SSE 3s GPS)
            (HTTP REST - OpenAPI)           |                  | - POST /rides/pause & /rides/stop
                                            v                  | (HTTP REST + SSE Stream)
 +-----------------------------------------------------------+ |
 |            Central Management Server & Admin UI           | |
 |                 (Single Source of Truth)                  | |
 +-------+------------------+-------------------+------------+ |
         |                  |                   |              |
         | Reads / Writes   | Reads / Caches    | Docker       |
         v                  v                   | Socket       |
 +---------------+  +---------------+           v              |
 |  PostgreSQL   |  |     Redis     |    +--------------+      |
 | (Users, Rides,|  |  (Stationary  |    | Docker Daemon|      |
 |  Stations SSoT|  |   Geo-Index & |    | (/var/run/   |      |
 |  & History)   |  |  Station Map) |    |  docker.sock)|      |
 +---------------+  +---------------+    +-------+------+      |
         ^                                       |             |
         |                               Spawns  |             |
         | Updates via Handover/End Events       v             |
 +-------+-----------------------------------------------+     |
 |                      Apache Kafka Broker              |     |
 |   Topics: ride-lifecycle-events, station-handover-    |     |
 |           events, admin-transfer-commands             |     |
 +-------+---------------------------------------+-------+     |
         ^                                       ^             |
         | Handover / Ride End                   | Admin /     |
         | Notifications                         | Handover    |
         v                                       v             v
 +-------------------------------+       +-------------------------------+
 |    Charging Station Node A    | <---> |    Charging Station Node B    |
 |   (Edge Service - Port 808x)  | Hand- |   (Edge Service - Port 808y)  |
 | - Local In-Memory Active State| over  | - Local In-Memory Active State|
 | - Route buffer & 1h Timer     |       | - Route buffer & 1h Timer     |
 +---------------+---------------+       +---------------+---------------+
                 ^                                       ^
                 | Consumes 3s GPS Telemetry             | Consumes 3s GPS Telemetry
                 +-------------------+-------------------+
                                     |
                     +---------------+---------------+
                     |     Apache Kafka Broker       |
                     |  (scooter-telemetry-topic)    |
                     +---------------+---------------+
                                     ^
                                     | Periodic GPS Heartbeat (every 3s)
                     +---------------+---------------+
                     |   IoT Fleet Simulator         |
                     | (Virtual Threads Java 21)     |
                     | [Scooter S1]   [Scooter S2]...|
                     +-------------------------------+

 ==============================================================================================
                           AGENTIC AI & MCP GOVERNANCE LAYER
 ==============================================================================================

                     +-------------------------------+
                     |  Nightly Autonomous AI Agent  |
                     | (Scheduled Cron / Spring AI)  |
                     +---------------+---------------+
                                     |
                                     | MCP Protocol (JSON-RPC)
                                     v
                     +---------------+---------------+
                     |       Fleet MCP Server        |
                     |     (Port 8083 - HTTP/SSE)    |
                     +-------+---------------+-------+
                             |               |
        Reads Fleet &        |               | Dispatches Rebalance
        Station Status       |               | Proposals / Bookings
                             v               v
            +----------------+---------------+----------------+
            | Calls Central Server APIs / Station Booking APIs|
            +-------------------------------------------------+
```

### Principii și Garanții Arhitecturale
1. **Sursă Unică de Adevăr (SSoT):** PostgreSQL stochează starea persistentă autoritară (utilizatori, trotinete, stații, curse încheiate, bilete de rebalansare). Doar Serverul Central deține credențialele de conexiune și drepturile de citire/scriere asupra acestei baze de date.
2. **Consecvență Eventuală prin Kafka:** Schimbările de stare (pornirea/încheierea cursei, predarea între stații) sunt publicate pe topice Kafka dedicate. Serverul Central și nodurile de calcul consumă aceste mesaje pentru a sincroniza structurile din memorie și din Redis, asigurând convergența datelor fără blocaje distribuite.
3. **Decuplare și Toleranță la Erori:** O întrerupere temporară a Serverului Central nu afectează cursele aflate deja în desfășurare, acestea fiind gestionate local în memoria nodurilor Stație de Încărcare (Edge). Invers, interogările frecvente ale clienților pe hartă nu degradează performanța tranzacțiilor din baza de date relațională.
4. **Procesare la Margine (Edge Offloading) și Delegare Completă:** Aplicația client comută comunicarea direct către Stația de Încărcare responsabilă încă din momentul **rezervării trotinetei** (rezervare ce poate fi menținută pentru **cel mult o oră** înainte de pornirea cursei). Pe toată durata rezervării și a cursei active ulterioare, clientul comunică **exclusiv cu serverul de pe stația respectivă (Edge Node)**, până la finalizarea cursei sau până la anularea rezervării. Telemetria GPS la 3 secunde este acumulată în memoria stației locale, eliminând complet supraîncărcarea rețelei centrale.
5. **Arhitectură Remote MCP:** Serverul MCP este o componentă de rețea independentă ce respectă standardul deschis Model Context Protocol, acționând ca un gateway securizat care expune unelte sistem fără a oferi modelului LLM acces direct la baza de date sau la infrastructura internă.

---

## 3. Specificații Detaliate ale Componentelor

### 3.1. Serverul Central de Management și Interfața Admin UI (Central Management Service)
* **Tehnologie & Runtime:** Java 21, Spring Boot 3.x
* **Port de Rulare:** `8081` (sau `8080`)
* **Depozite de Date Conectate:** PostgreSQL 16 (persistență autoritară SSoT) și Redis 7 (indexare geospațială și cache de apartenență)
* **Mesagerie:** Producător și consumator Apache Kafka
* **Documentație API:** OpenAPI 3.0 / Swagger UI (`/swagger-ui.html`, `/v3/api-docs`)
* **Integrare Infrastructură:** Acces la daemonul Docker al sistemului gazdă prin montarea `/var/run/docker.sock` utilizând biblioteca `docker-java`

#### Responsabilități Principale:
- Menține evidența globală a flotei: starea și poziția tuturor trotinetelor staționare, precum și indexul de mapare `Trotinetă -> Stație de Încărcare Părinte`.
- Deservește interogările de descoperire (*Discovery*) ale aplicațiilor client, returnând trotinetele disponibile pe o rază geografică dată prin căutări rapide în Redis (`GEORADIUS`).
- Actualizează baza de date PostgreSQL și structurile Redis la primirea evenimentelor de finalizare a cursei sau de migrare (handover).
- Permite transferul logic al trotinetelor staționare între stații, notificând nodurile implicate prin Kafka.
- Expune o interfață web de administrare (Admin UI) securizată:
  - Vizualizarea pe hartă a tuturor stațiilor și a trotinetelor staționare arondate.
  - Interogare live la cerere: pentru o trotinetă în mișcare, Serverul Central interoghează stația edge aferentă și afișează locația curentă.
  - Spawning dinamic de containere: permite crearea unei noi stații direct din UI, pornind automat un nou container Docker cu parametrii specifici (`STATION_ID`, coordonate GPS, adresa Kafka).
  - Inspectarea și aprobarea propunerilor de rebalansare generate de Agentul AI prin Swagger UI sau tabloul de bord.

---

### 3.2. Serviciul Stație de Încărcare (Charging Station Edge Service)
* **Tehnologie & Runtime:** Java 21, Spring Boot 3.x
* **Port de Rulare:** Dinamic alocat (`8084`, `8085`, etc.)
* **Stare Locală:** Exclusiv în memorie (In-Memory). Nu deține acces direct la PostgreSQL sau Redis.
* **Canale de Comunicare:** REST HTTP și Server-Sent Events (SSE) către clienți; Apache Kafka către trotinete și Serverul Central.

#### Responsabilități Principale:
- Gestionează întregul ciclu de viață al interacțiunii pentru trotinetele arondate, **începând din momentul rezervării premergătoare cursei până la finalizarea sau anularea acesteia**.
- **Rezervare cu Cronometru de Cel Mult 1 Oră:** Permite utilizatorului să rezerve o trotinetă pentru maximum o oră (60 de minute) înainte de a începe cursa. Din momentul confirmării rezervării, clientul comunică exclusiv cu acest server de stație. Dacă utilizatorul anulează rezervarea sau expiră timpul de o oră fără începerea cursei, stația eliberează trotineta și notifică Serverul Central.
- Primește la fiecare 3 secunde coordonatele GPS de la simulatorul de trotinete prin Kafka, le stochează în memoria locală (construind vectorul complet al traseului) și le transmite în timp real către aplicația mobilă a utilizatorului prin SSE.
- Controlează stările operaționale ale cursei: *Rezervată* (cronometru max. 1h), *În Mișcare*, *Pauză* (cu pornirea unui cronometru local de 60 de minute în care vehiculul rămâne blocat pentru utilizatorul curent) și *Finalizată*.
- La finalizarea cursei, transmite pe Kafka evenimentul `RIDE_ENDED` conținând coordonatele finale și întregul istoric al traseului către Serverul Central pentru persistență.
- **Protocolul de Handover (Migrare între Stații):** Când o trotinetă finalizează cursa în raza unei noi stații de încărcare:
  - Noua stație notifică vechea stație prin Kafka (`station-handover-events`) pentru a goli starea din memorie și a face flush la segmentele de traseu netransmise.
  - Noua stație notifică Serverul Central pentru a actualiza indexul de apartenență în Redis și PostgreSQL.

---

### 3.3. Aplicația Client Mobil / Web
* **Platformă & Tehnologii:** Android (minSDK 26, targetSDK 34+), Kotlin, Jetpack Compose, Retrofit 2, OkHttp 3, Coroutines, StateFlow (sau interfață Web echivalentă)
* **Senzori & Hardware:** Google Play Services `FusedLocationProviderClient`, Android `CameraX`

#### Responsabilități Principale:
- Obține coordonatele GPS ale utilizatorului prin intermediul serviciilor de localizare nativă.
- Interoghează Serverul Central (`GET /api/v1/scooters/nearby`) pentru afișarea pe hartă a trotinetelor staționare disponibile și a stațiilor aferente.
- **Rezervare și Delegare Exclusivă la Margine:** Utilizatorul poate rezerva o trotinetă pentru **cel mult o oră** înainte de pornirea cursei. Imediat ce trimite cererea de rezervare (`POST /api/v1/rides/reserve`), aplicația **se deconectează de la Serverul Central și comunică exclusiv cu serverul Stației de Încărcare** aferente pe toată durata rezervării și a cursei, până la finalizarea cursei sau până la anularea rezervării.
- Ascultă stream-ul SSE deschis pe Stația de Încărcare pentru a desena traseul în timp real la fiecare 3 secunde și a afișa timpul rămas din pauză sau timpul rămas din rezervare.
- Permite utilizatorului să comute cursa în starea de Pauză, să anuleze rezervarea sau să solicite Oprirea Cursei.
- La finalizarea cursei, activează camera dispozitivului prin `CameraX` pentru capturarea dovezii foto de parcare regulamentară și transmite cererea de finalizare către Stația de Încărcare.

---

### 3.4. Simulatorul IoT pentru Flotă (IoT Fleet Simulator)
* **Tehnologie & Runtime:** Java 21, Spring Boot
* **Model de Concurență:** Fire de execuție virtuale (*Virtual Threads* - Project Loom)
* **Mesagerie:** Producător Apache Kafka (topic: `scooter-telemetry-topic`)

#### Responsabilități Principale:
- Emulează comportamentul hardware al flotei fără consum prohibitiv de resurse RAM, alocând fiecărui vehicul un fir de execuție virtual independent.
- Pentru fiecare vehicul aflat în starea `MOVING`, firul generează la fiecare 3 secunde pachete de telemetrie cu coordonate interpolate, descreșterea bateriei și viteza instantanee.
- Reacționează la starea de pauză și oprește generarea activă la finalizarea cursei.

---

### 3.5. Serverul MCP pentru Flotă (Fleet Remote MCP Gateway)
* **Tehnologie & Runtime:** Java 21, Spring Boot 3.x / Spring AI MCP SDK
* **Port de Rulare:** `8083`
* **Protocol de Comunicație:** Model Context Protocol (JSON-RPC 2.0 peste transport HTTP/SSE)
* **Rol:** Gateway securizat pentru integrarea agenților autonomi inteligenți.

#### Responsabilități Principale:
- Expune un set standardizat de unelte (*tools*) puternic tipizate conform specificațiilor MCP.
- Intermediază apelurile uneltelor către API-urile REST ale Serverului Central sau ale Stațiilor de Încărcare, agregate și formatate conform schemelor JSON-RPC cerute de LLM.
- Garantează izolarea securizată: agentul AI nu primește acces direct la conexiunile de bază de date sau la brokerul Kafka, ci operează exclusiv prin uneltele validate puse la dispoziție.

---

### 3.6. Agentul Autonom de Rebalansare a Flotei (Autonomous Rebalancing Agent)
* **Tehnologie & Runtime:** Serviciu autonom programat în Java 21 (Spring Boot Daemon integrat cu Spring AI sau LangChain4j)
* **Mecanism de Declanșare:** Expresie cron configurabilă (`@Scheduled(cron = "0 0 23 * * *")` pentru noapte, sau la fiecare 2 minute în demonstrator)
* **Furnizor LLM:** Google Gemini API / OpenAI / Groq (nivel gratuit)

#### Promptul de Sistem (System Prompt):
```text
Ești agentul autonom de optimizare și rebalansare logistică a flotei de trotinete electrice.
Rolul tău este să asiguri distribuția optimă a vehiculelor în toate stațiile municipale pentru a preveni supraaglomerarea și a garanta disponibilitatea pentru cererea din dimineața următoare.

Instrucțiuni de operare:
1. Apelează unealta 'get_fleet_and_stations_status' pentru a obține topologia curentă, capacitatea maximă a fiecărei stații, numărul de trotinete andocate și nivelul bateriilor.
2. Identifică stațiile deficitare și stațiile care depășesc capacitatea maximă admisă.
3. Filtrează trotinetele eligibile pentru transfer: prioritizează vehiculele cu un nivel al bateriei mai mare de 70%. Nu propune transferul trotinetelor cu bateria descărcată în stații cu cerere ridicată.
4. Formulează un plan optim de transfer logic și apelează unealta 'propose_fleet_rebalancing' furnizând lista de mutări și justificarea operațională completă pentru fiecare acțiune.
5. Menține un format JSON strict și un raționament clar și concis.
```

---

### 3.7. Modulul Multimodal de Inspecție Vizuală AI (Parking Compliance)
* **Serviciu Integrator:** `Central Management Service`
* **Furnizor / Model:** Google Gemini Flash Vision REST API
* **Declanșator:** Recepționarea fotografiei de parcare la apelul `POST /api/v1/rides/stop`.

#### Promptul de Sistem pentru Viziune Artificială (System Prompt):
```text
You are an automated municipal micro-mobility parking compliance inspector.
Analyze the provided photograph of an electric scooter parked on an urban sidewalk or public space.

Verify the following safety and municipal criteria strictly:
1. Is the scooter standing upright on its kickstand? (Reject if tipped over, leaning dangerously, or lying on the ground).
2. Does the parked scooter obstruct pedestrian walkways, tactile paving surfaces for the visually impaired, wheelchair curb ramps, building access doors, emergency exits, or fire hydrants?
3. Is the scooter parked within designated painted bays or in a non-obstructive street corner area?

You MUST respond strictly in valid JSON matching the following schema:
{
  "compliant": boolean,
  "violationType": "SIDEWALK_OBSTRUCTION" | "WHEELCHAIR_RAMP_BLOCKED" | "TIPPED_OVER" | "FIRE_HYDRANT_BLOCKED" | "NONE",
  "confidence": number (between 0.0 and 1.0),
  "reasoning": string
}
```

#### Schema Răspunsului JSON Generat de Model:
```json
{
  "compliant": false,
  "violationType": "WHEELCHAIR_RAMP_BLOCKED",
  "confidence": 0.94,
  "reasoning": "The electric scooter is parked horizontally across a tactile truncated dome curb cut, completely blocking wheelchair and stroller descent to the crosswalk."
}
```

#### Acțiune Automată de Sistem:
Dacă `compliant == false` și `confidence > 0.80`, Serverul Central marchează cursa în PostgreSQL cu flag-ul de parcare neconformă, înregistrează motivul contravenției și adaugă o taxă de penalizare la costul total al utilizatorului.

---

## 4. Protocoale de Comunicare, OpenAPI și Scheme de Date

### 4.1. Arhitectura de Rețea și Separarea Responsabilităților
- **Clienți $\to$ Noduri Backend:** HTTP REST (OpenAPI 3.0) pentru comenzi deterministe și Server-Sent Events (SSE) pentru fluxul de poziții GPS la 3 secunde.
- **Backend $\to$ Broker:** Apache Kafka pentru mesagerie asincronă bazată pe evenimente și telemetrie hardware.
- **Agent AI $\to$ Backend:** Model Context Protocol (JSON-RPC 2.0 peste HTTP/SSE).

---

### 4.2. Lista Completă a Endpoint-urilor HTTP REST (OpenAPI 3.0)

#### A. Endpoint-uri Serverul Central (`Central-Server API` - Port 8081)

| Metodă | Endpoint | Descriere Funcțională | Payload Cerere / Parametri | Răspuns HTTP & Format Date |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/scooters/nearby` | Caută trotinetele staționare disponibile în proximitate | `?lat=45.7537&lon=21.2257&radiusMeters=500` | `200 OK`<br>`[{"scooterId": "sc_101", "stationId": "station_a", "stationUrl": "http://station-a:8084", "lat": 45.7538, "lon": 21.2257, "batteryLevel": 88, "model": "Ninebot Max"}]` |
| `GET` | `/api/v1/admin/fleet-overview` | Prezentarea globală a flotei pentru Admin UI | *Niciunul (necesită Auth)* | `200 OK`<br>`{"totalStations": 4, "totalScooters": 50, "activeRides": 3, "stations": [{"stationId": "station_a", "capacity": 15, "dockedCount": 8, "status": "ACTIVE"}]}` |
| `GET` | `/api/v1/admin/scooters/{id}/live-location` | Interogare în timp real a locației unei trotinete active | Cale: `{id}` (ex: `sc_101`) | `200 OK`<br>`{"scooterId": "sc_101", "status": "MOVING", "managingStation": "station_a", "lat": 45.7512, "lon": 21.2280, "batteryLevel": 79, "speedKmh": 18.2}` |
| `POST` | `/api/v1/admin/stations/spawn` | Instanțierea dinamică a unui container Docker de stație | `{"stationId": "station_c", "name": "Stația Parcul Central", "lat": 45.7580, "lon": 21.2310, "capacity": 20, "port": 8086}` | `201 Created`<br>`{"stationId": "station_c", "dockerContainerId": "e3b0c44298fc1c149afbf4c8996fb924", "status": "SPAWNED_AND_READY"}` |
| `POST` | `/api/v1/admin/scooters/transfer` | Mutarea administrativă a unei trotinete între stații | `{"scooterId": "sc_101", "fromStationId": "station_a", "toStationId": "station_b", "reason": "Mentenanță zonă"}` | `200 OK`<br>`{"status": "TRANSFER_INITIATED", "scooterId": "sc_101", "targetStationId": "station_b"}` |
| `GET` | `/api/v1/rebalance/pending` | Listează propunerile de rebalansare generate de AI | *Niciunul* | `200 OK`<br>`[{"taskId": "reb_9921", "sourceStationId": "station_a", "targetStationId": "station_b", "scooterIds": ["sc_101", "sc_105"], "status": "PROPOSED", "justification": "Echilibrare cerere dimineață"}]` |
| `POST` | `/api/v1/rebalance/{id}/approve` | Aprobă propunerea de rebalansare | Cale: `{id}` (ex: `reb_9921`) | `200 OK`<br>`{"taskId": "reb_9921", "status": "APPROVED", "dispatchedAt": "2026-11-05T00:05:00Z"}` |
| `POST` | `/api/v1/rebalance/propose` | Înregistrează propunerea generată de serverul MCP | `{"sourceStationId": "station_a", "targetStationId": "station_b", "scooterIds": ["sc_101"], "justification": "Surplus stația A"}` | `201 Created`<br>`{"taskId": "reb_9921", "status": "PROPOSED"}` |
| `GET` | `/api/v1/analytics/demand` | Predicție istorică a cererii de trotinete | `?stationId=station_a&targetHour=8` | `200 OK`<br>`{"stationId": "station_a", "targetHour": 8, "predictedDemand": 25, "historicalAvg": 22}` |
| `GET` | `/api/v1/users/{id}/rides` | Istoric curse și plăți pentru utilizator | Cale: `{id}` (ex: `usr_44`) | `200 OK`<br>`[{"rideId": "rd_7712", "startTime": "...", "fareCents": 1850, "isCompliantParking": true}]` |

#### B. Endpoint-uri Stații de Încărcare (`Charging-Station Edge API` - Port 808x)

| Metodă | Endpoint | Descriere Funcțională | Payload Cerere / Parametri | Răspuns HTTP & Format Date |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/rides/reserve` | Rezervă trotineta selectată pentru **cel mult o oră** (60 minute) înainte de cursă | `{"scooterId": "sc_101", "userId": "usr_44"}` | `200 OK`<br>`{"scooterId": "sc_101", "reservationStatus": "LOCKED_FOR_USER", "expiresInSeconds": 3600}` |
| `POST` | `/api/v1/rides/reserve/cancel` | Anulează rezervarea activă, deblocând trotineta și notificând Serverul Central | `{"scooterId": "sc_101", "userId": "usr_44"}` | `200 OK`<br>`{"scooterId": "sc_101", "reservationStatus": "CANCELLED_AND_UNLOCKED"}` |
| `POST` | `/api/v1/rides/start` | Inițiază cursa și trimite comanda de deblocare | `{"scooterId": "sc_101", "userId": "usr_44"}` | `201 Created`<br>`{"rideId": "rd_7712", "scooterId": "sc_101", "status": "ACTIVE", "startTime": "2026-11-04T22:10:00Z"}` |
| `POST` | `/api/v1/rides/pause` | Comută cursa pe pauză (start timer 60 min) | `{"rideId": "rd_7712"}` | `200 OK`<br>`{"rideId": "rd_7712", "status": "PAUSED", "pauseMaxDurationMinutes": 60, "pauseStartedAt": "2026-11-04T22:25:00Z"}` |
| `POST` | `/api/v1/rides/stop` | Încheie cursa, trimite traseul și poza parcării | `multipart/form-data`:<br>- `rideId`: `"rd_7712"`<br>- `lat`: `45.7489`<br>- `lon`: `21.2391`<br>- `photoFile`: *(fișier binar JPEG)* | `200 OK`<br>`{"rideId": "rd_7712", "status": "COMPLETED", "durationSeconds": 1500, "distanceMeters": 3200, "fareCents": 1850, "parkingCompliance": {"compliant": true, "confidence": 0.96}}` |
| `GET` | `/api/v1/rides/{rideId}/stream` | Stream SSE cu telemetria din 3 în 3 secunde și starea rezervării/pauzei | Cale: `{rideId}` | `200 OK` (`text/event-stream`)<br>`event: telemetry`<br>`data: {"lat": 45.7538, "lon": 21.2260, "batteryLevel": 85, "speed": 17.2}` |

---

### 4.3. Schemele Evenimentelor Apache Kafka (Topic-uri de Domeniu)

#### 1. Evenimentul `fleet.ride-started` (Topic: `ride-lifecycle-events`)
```json
{
  "eventId": "c7a10f82-3d52-47bc-9b63-12c8a1438901",
  "eventType": "RIDE_STARTED",
  "timestamp": "2026-11-04T22:10:00Z",
  "payload": {
    "rideId": "rd_7712",
    "scooterId": "sc_101",
    "userId": "usr_44",
    "stationId": "station_a",
    "startLatitude": 45.7538,
    "startLongitude": 21.2257,
    "batteryLevel": 92
  }
}
```

#### 2. Evenimentul `fleet.ride-ended` (Topic: `ride-lifecycle-events`)
```json
{
  "eventId": "e9b21f93-4e63-58cd-ac74-23d9b2549012",
  "eventType": "RIDE_ENDED",
  "timestamp": "2026-11-04T22:35:00Z",
  "payload": {
    "rideId": "rd_7712",
    "scooterId": "sc_101",
    "userId": "usr_44",
    "destinationStationId": "station_b",
    "endLatitude": 45.7489,
    "endLongitude": 21.2391,
    "batteryLevel": 81,
    "durationSeconds": 1500,
    "distanceMeters": 3200,
    "photoUrl": "http://minio:9000/parking-photos/rd_7712.jpg",
    "routePoints": [
      {"lat": 45.7538, "lon": 21.2257, "t": "2026-11-04T22:10:00Z"},
      {"lat": 45.7525, "lon": 21.2270, "t": "2026-11-04T22:10:03Z"},
      {"lat": 45.7489, "lon": 21.2391, "t": "2026-11-04T22:35:00Z"}
    ]
  }
}
```

#### 3. Evenimentul `fleet.telemetry-updated` (Topic: `scooter-telemetry-topic`)
```json
{
  "scooterId": "sc_101",
  "timestamp": "2026-11-04T22:15:03Z",
  "latitude": 45.7512,
  "longitude": 21.2280,
  "speedKmh": 18.5,
  "batteryLevel": 88,
  "status": "MOVING"
}
```

#### 4. Evenimentul `station.handover-request` (Topic: `station-handover-events`)
```json
{
  "eventId": "fa129841-1193-43bb-a612-880192348571",
  "eventType": "HANDOVER_REQUEST",
  "timestamp": "2026-11-04T22:35:05Z",
  "payload": {
    "scooterId": "sc_101",
    "fromStationId": "station_a",
    "toStationId": "station_b",
    "arrivalLatitude": 45.7489,
    "arrivalLongitude": 21.2391,
    "batteryLevel": 81
  }
}
```

---

### 4.4. Schemele Instrumentelor MCP (Model Context Protocol JSON Schemas)

#### Tool 1: `find_available_scooters`
- **Descriere:** Identifică trotinetele staționare libere într-o zonă geografică circulară.
- **JSON Schema de Intrare:**
```json
{
  "type": "object",
  "properties": {
    "latitude": { "type": "number", "description": "Latitudinea centrului zonei căutate" },
    "longitude": { "type": "number", "description": "Longitudinea centrului zonei căutate" },
    "radiusMeters": { "type": "number", "description": "Raza de căutare în metri" }
  },
  "required": ["latitude", "longitude", "radiusMeters"]
}
```

#### Tool 2: `book_scooter_for_user`
- **Descriere:** Rezervă și blochează o trotinetă pentru un utilizator specific.
- **JSON Schema de Intrare:**
```json
{
  "type": "object",
  "properties": {
    "userId": { "type": "string", "description": "Identificatorul unic al utilizatorului" },
    "scooterId": { "type": "string", "description": "Identificatorul unic al trotinetei" }
  },
  "required": ["userId", "scooterId"]
}
```

#### Tool 3: `get_fleet_and_stations_status`
- **Descriere:** Returnează inventarul global al stațiilor și al trotinetelor pentru auditul AI nocturn.
- **JSON Schema de Intrare:**
```json
{
  "type": "object",
  "properties": {},
  "required": []
}
```
- **Exemplu Răspuns Returnat Către Agent:**
```json
{
  "stations": [
    {
      "stationId": "station_a",
      "capacity": 15,
      "dockedCount": 18,
      "scooterIds": ["sc_101", "sc_102", "sc_105", "..."],
      "deficit": -3
    },
    {
      "stationId": "station_b",
      "capacity": 15,
      "dockedCount": 3,
      "scooterIds": ["sc_144", "sc_145"],
      "deficit": 12
    }
  ],
  "scooters": [
    {"scooterId": "sc_101", "batteryLevel": 92, "stationId": "station_a"},
    {"scooterId": "sc_105", "batteryLevel": 85, "stationId": "station_a"}
  ]
}
```

#### Tool 4: `propose_fleet_rebalancing`
- **Descriere:** Transmite biletul de redistribuire a flotei către Serverul Central.
- **JSON Schema de Intrare:**
```json
{
  "type": "object",
  "properties": {
    "sourceStationId": { "type": "string", "description": "ID-ul stației cu surplus" },
    "targetStationId": { "type": "string", "description": "ID-ul stației cu deficit" },
    "scooterIds": {
      "type": "array",
      "items": { "type": "string" },
      "description": "Lista identificatorilor trotinetelor propuse spre mutare"
    },
    "justification": { "type": "string", "description": "Argumentul operațional al modelului LLM" }
  },
  "required": ["sourceStationId", "targetStationId", "scooterIds", "justification"]
}
```

---

## 5. Fluxurile Complete de Date Pas cu Pas

### Fluxul 1: Descoperirea, Rezervarea și Pornirea unei Curse
1. **Faza de Descoperire (Central Server):** Utilizatorul deschide aplicația mobilă. Dispozitivul obține coordonatele curente prin GPS și apelează `GET /api/v1/scooters/nearby` pe Serverul Central. Serverul Central interoghează indexul geospațial din Redis și returnează lista vehiculelor libere din perimetru, alături de adresa/portul stației de încărcare de care aparține fiecare vehicul.
2. **Rezervarea Premergătoare Cursei (Delegare Exclusivă la Margine):**
   - Utilizatorul selectează trotineta dorită și apasă „Rezervă” (`POST /api/v1/rides/reserve`), având posibilitatea de a o rezerva pentru **cel mult o oră (60 de minute)** înainte de a începe cursa.
   - În momentul emiterii cererii de rezervare, aplicația client **își suspendă complet comunicarea cu Serverul Central și comută conexiunea direct către serverul Stației de Încărcare (Edge Node)**.
   - Stația de Încărcare blochează trotineta în memoria locală pentru utilizatorul solicitant, pornește cronometrul descrescător de maximum o oră și notifică Serverul Central prin Kafka pentru a ascunde temporar trotineta de ceilalți utilizatori.
   - **Pe toată durata acestei rezervări și pe durata cursei ulterioare, clientul comunică exclusiv cu serverul Stației de Încărcare.**
3. **Anularea Rezervării sau Expirarea Timpului:**
   - Dacă utilizatorul decide să renunțe, trimite comanda de anulare către stație (`POST /api/v1/rides/reserve/cancel`).
   - În caz de anulare sau dacă expiră timpul maxim de o oră fără pornirea cursei, Stația de Încărcare deblochează trotineta în memoria locală, emite un eveniment Kafka către Serverul Central pentru a repune vehiculul la dispoziția publicului în Redis, iar clientul revine la comunicarea cu Serverul Central.
4. **Pornirea Cursei Active:**
   - Când utilizatorul ajunge la trotinetă, apasă „Start Cursă” (`POST /api/v1/rides/start`), apel trimis tot direct către Stația de Încărcare.
   - Stația de Încărcare deblochează fizic vehiculul, marchează starea `MOVING` în memoria sa locală și publică evenimentul `RIDE_STARTED` pe Kafka.
   - Conexiunea continuă exclusiv între client și stație (pe HTTP și stream SSE pentru telemetria la 3 secunde) până la oprirea definitivă a cursei.

### Fluxul 2: Desfășurarea Cursei Active, Pauza și Finalizarea
1. Pe durata deplasării, simulatorul IoT emite la fiecare 3 secunde pachete de telemetrie pe topicul Kafka `scooter-telemetry-topic`.
2. Stația de Încărcare consumă aceste pachete, adaugă fiecare punct GPS în lista traseului din memoria locală și redirecționează datele prin conexiunea SSE către aplicația mobilă a utilizatorului.
3. Dacă utilizatorul dorește să oprească temporar, apasă butonul „Pauză” din aplicație (`POST /api/v1/rides/pause`). Stația activează cronometrul de pauză (maxim 60 de minute), păstrând trotineta blocată exclusiv pentru utilizatorul respectiv.
4. Când utilizatorul ajunge la destinație, apasă „Stop Cursă” (`POST /api/v1/rides/stop`) și încarcă o fotografie a parcării realizată prin `CameraX`.
5. Stația de Încărcare oprește sesiunea, calculează costul estimat și publică evenimentul `RIDE_ENDED` pe Kafka, transmițând întregul vector de puncte GPS și imaginea parcării.
6. Serverul Central consumă evenimentul `RIDE_ENDED`, transmite fotografia către modulul Gemini Flash Vision pentru validarea parcării, persistă cursa și traseul în PostgreSQL și actualizează noua poziție a trotinetei în Redis.

### Fluxul 3: Migrarea unei Trotinete între Stații (Handover Inter-Noduri)
1. În situația în care o trotinetă pornită de la Stația A își încheie cursa în proximitatea fizică a Stației B:
2. Stația B detectează andocarea sau prezența trotinetei în zona sa operațională și o înregistrează în memoria locală.
3. Stația B publică pe Kafka un eveniment de tip `HANDOVER_REQUEST` direcționat către Stația A.
4. Stația A recepționează notificarea, transmite către Serverul Central orice segment de telemetrie rezidual și șterge trotineta din gestiunea memoriei locale.
5. Concomitent, Stația B notifică Serverul Central cu privire la noua apartenență, iar acesta actualizează indexul din Redis și înregistrarea din PostgreSQL, noul părinte oficial devenind Stația B.

### Fluxul 4: Monitorizarea Live de către Administrator și Rebalansarea Autonomă
1. **Pe parcursul zilei:** Administratorul monitorizează harta flotei din Admin UI. Pentru vizualizarea unei trotinete aflate în deplasare, Serverul Central interoghează transparent stația edge responsabilă și obține coordonatele instantanee din bufferul de memorie.
2. **La finalul zilei (Operațiune Autonomă):**
   - Serviciul Agent AI se activează conform programării cron (ora 23:59).
   - Agentul interoghează Serverul MCP prin `get_fleet_and_stations_status` pentru a analiza stocul fiecărei stații și nivelul bateriilor.
   - Folosind raționamentul furnizat de modelul LLM, agentul stabilește planul optim de redistribuire pentru a evita supraaglomerarea unor stații și a asigura vehicule suficiente în zonele de tranzit pentru dimineața următoare.
   - Agentul apelează unealta `propose_fleet_rebalancing`, salvând planul în baza de date centrală.
   - Administratorul inspectează planul generat direct din interfață și îl aprobă, moment în care comenzile de transfer sunt dispecerizate prin Kafka către nodurile vizate.

---

## 6. Modelul de Date și Persistența (PostgreSQL 16 & Redis 7)

### Scriptul SQL DDL Complet (PostgreSQL 16 - SSoT)

```sql
-- Extensie pentru operațiuni geospațiale avansate
CREATE EXTENSION IF NOT EXISTS postgis;

-- 1. Tabela zonelor urbane operaționale
CREATE TABLE zones (
    zone_id VARCHAR(32) PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    min_capacity INT NOT NULL DEFAULT 5,
    target_capacity INT NOT NULL DEFAULT 20,
    boundary_polygon GEOMETRY(Polygon, 4326),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabela stațiilor de încărcare (noduri Edge)
CREATE TABLE charging_stations (
    station_id VARCHAR(32) PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lon DOUBLE PRECISION NOT NULL,
    capacity INT NOT NULL DEFAULT 15,
    docker_container_id VARCHAR(64),
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, MAINTENANCE, OFFLINE
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Tabela flotei de trotinete
CREATE TABLE scooters (
    scooter_id VARCHAR(32) PRIMARY KEY,
    model VARCHAR(32) NOT NULL,
    battery_level INT NOT NULL CHECK (battery_level BETWEEN 0 AND 100),
    current_status VARCHAR(24) NOT NULL, -- AVAILABLE, IN_USE, LOW_BATTERY, MAINTENANCE
    current_lat DOUBLE PRECISION NOT NULL,
    current_lon DOUBLE PRECISION NOT NULL,
    assigned_station_id VARCHAR(32) REFERENCES charging_stations(station_id),
    assigned_zone_id VARCHAR(32) REFERENCES zones(zone_id),
    version BIGINT NOT NULL DEFAULT 0, -- Blocare optimistă (Optimistic Locking)
    last_maintenance TIMESTAMP WITH TIME ZONE
);

-- 4. Tabela istoricului curselor
CREATE TABLE rides (
    ride_id VARCHAR(32) PRIMARY KEY,
    scooter_id VARCHAR(32) NOT NULL REFERENCES scooters(scooter_id),
    user_id VARCHAR(32) NOT NULL,
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
    route_trace JSONB, -- Punctele intermediare GPS ale traseului
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Tabela sarcinilor de rebalansare (generate de AI sau Admin)
CREATE TABLE rebalance_tasks (
    task_id VARCHAR(32) PRIMARY KEY,
    source_station_id VARCHAR(32) NOT NULL REFERENCES charging_stations(station_id),
    target_station_id VARCHAR(32) NOT NULL REFERENCES charging_stations(station_id),
    scooter_ids TEXT[] NOT NULL,
    status VARCHAR(24) NOT NULL, -- PROPOSED, APPROVED, IN_PROGRESS, COMPLETED, REJECTED
    proposed_by VARCHAR(32) NOT NULL, -- 'AUTONOMOUS_AGENT' sau identificator de operator
    justification TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexare pentru performanță
CREATE INDEX idx_scooters_station ON scooters(assigned_station_id);
CREATE INDEX idx_scooters_status ON scooters(current_status);
CREATE INDEX idx_rides_user ON rides(user_id);
CREATE INDEX idx_rides_scooter ON rides(scooter_id);
```

---

## 7. Arhitectura de Containerizare și Orchestrare (Docker Compose)

Fișierul `docker-compose.yml` complet integrează întregul cluster distribuit, inclusiv brokerul Kafka, depozitele de date, serviciul central cu montarea socket-ului Docker, instanțele inițiale ale stațiilor edge, simulatorul IoT, serverul MCP și agentul de rebalansare.

```yaml
version: '3.8'

networks:
  fleet-net:
    driver: bridge

volumes:
  pgdata:
  redisdata:

services:
  # -------------------------------------------------------------
  # 1. Baza de Date Autoritară: PostgreSQL 16
  # -------------------------------------------------------------
  postgres-db:
    image: postgres:16-alpine
    container_name: postgres-db
    restart: unless-stopped
    networks:
      - fleet-net
    environment:
      POSTGRES_DB: fleet_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgrespassword
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

  # -------------------------------------------------------------
  # 2. Depozit Geospațial In-Memory: Redis 7
  # -------------------------------------------------------------
  redis-cache:
    image: redis:7-alpine
    container_name: redis-cache
    restart: unless-stopped
    networks:
      - fleet-net
    ports:
      - "6379:6379"
    volumes:
      - redisdata:/data

  # -------------------------------------------------------------
  # 3. Broker de Mesagerie: Apache Kafka (KRaft mode)
  # -------------------------------------------------------------
  kafka-broker:
    image: confluentinc/cp-kafka:7.6.0
    container_name: kafka-broker
    restart: unless-stopped
    networks:
      - fleet-net
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: 'CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT'
      KAFKA_ADVERTISED_LISTENERS: 'PLAINTEXT://kafka-broker:29092,PLAINTEXT_HOST://localhost:9092'
      KAFKA_PROCESS_ROLES: 'broker,controller'
      KAFKA_CONTROLLER_QUORUM_VOTERS: '1@kafka-broker:29093'
      KAFKA_LISTENERS: 'PLAINTEXT://0.0.0.0:29092,CONTROLLER://0.0.0.0:29093,PLAINTEXT_HOST://0.0.0.0:9092'
      KAFKA_CONTROLLER_LISTENER_NAMES: 'CONTROLLER'
      CLUSTER_ID: '4L622nShTWWkrFuMmBNBur'
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

  # -------------------------------------------------------------
  # 4. Serverul Central & Admin UI (SSoT + Docker Controller)
  # -------------------------------------------------------------
  central-server:
    build:
      context: ./central-server
      dockerfile: Dockerfile
    container_name: central-server
    restart: unless-stopped
    networks:
      - fleet-net
    ports:
      - "8081:8081"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-db:5432/fleet_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgrespassword
      SPRING_DATA_REDIS_HOST: redis-cache
      SPRING_DATA_REDIS_PORT: 6379
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka-broker:29092
      GEMINI_API_KEY: ${GEMINI_API_KEY}
    volumes:
      # Montarea socket-ului Docker pentru pornirea dinamică a containerelor de stații
      - /var/run/docker.sock:/var/run/docker.sock
    depends_on:
      - postgres-db
      - redis-cache
      - kafka-broker

  # -------------------------------------------------------------
  # 5. Serviciul Edge: Nod Stație de Încărcare A
  # -------------------------------------------------------------
  charging-station-a:
    build:
      context: ./charging-station-service
      dockerfile: Dockerfile
    container_name: charging-station-a
    restart: unless-stopped
    networks:
      - fleet-net
    ports:
      - "8084:8084"
    environment:
      STATION_ID: station_a
      SERVER_PORT: 8084
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka-broker:29092
      CENTRAL_SERVER_URL: http://central-server:8081
    depends_on:
      - kafka-broker
      - central-server

  # -------------------------------------------------------------
  # 6. Serviciul Edge: Nod Stație de Încărcare B
  # -------------------------------------------------------------
  charging-station-b:
    build:
      context: ./charging-station-service
      dockerfile: Dockerfile
    container_name: charging-station-b
    restart: unless-stopped
    networks:
      - fleet-net
    ports:
      - "8085:8085"
    environment:
      STATION_ID: station_b
      SERVER_PORT: 8085
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka-broker:29092
      CENTRAL_SERVER_URL: http://central-server:8081
    depends_on:
      - kafka-broker
      - central-server

  # -------------------------------------------------------------
  # 7. Simulatorul IoT pentru Flotă (Virtual Threads Java 21)
  # -------------------------------------------------------------
  scooter-simulator:
    build:
      context: ./scooter-simulator
      dockerfile: Dockerfile
    container_name: scooter-simulator
    restart: unless-stopped
    networks:
      - fleet-net
    environment:
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka-broker:29092
      FLEET_SIZE: 50
    depends_on:
      - kafka-broker

  # -------------------------------------------------------------
  # 8. Serverul Remote MCP (Agent Gateway - Port 8083)
  # -------------------------------------------------------------
  fleet-mcp-server:
    build:
      context: ./fleet-mcp-server
      dockerfile: Dockerfile
    container_name: fleet-mcp-server
    restart: unless-stopped
    networks:
      - fleet-net
    ports:
      - "8083:8083"
    environment:
      CENTRAL_SERVER_URL: http://central-server:8081
    depends_on:
      - central-server

  # -------------------------------------------------------------
  # 9. Agentul Autonom de Rebalansare a Flotei (Daemon Cron)
  # -------------------------------------------------------------
  autonomous-rebalance-agent:
    build:
      context: ./autonomous-rebalance-agent
      dockerfile: Dockerfile
    container_name: autonomous-rebalance-agent
    restart: unless-stopped
    networks:
      - fleet-net
    environment:
      MCP_SERVER_URL: http://fleet-mcp-server:8083
      GEMINI_API_KEY: ${GEMINI_API_KEY}
      AGENT_CRON_SCHEDULE: "0 */2 * * * *" # La fiecare 2 minute în demonstrator
    depends_on:
      - fleet-mcp-server
```
