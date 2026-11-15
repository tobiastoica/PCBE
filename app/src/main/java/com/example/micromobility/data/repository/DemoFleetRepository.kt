package com.example.micromobility.data.repository

import com.example.micromobility.domain.model.ActiveConnectionTarget
import com.example.micromobility.domain.model.ChargingStation
import com.example.micromobility.domain.model.DemoProtocolLog
import com.example.micromobility.domain.model.ProtocolChannel
import com.example.micromobility.domain.model.Scooter
import com.example.micromobility.domain.model.ScooterStatus
import com.example.micromobility.domain.model.StationStatus
import com.example.micromobility.domain.repository.FleetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class DemoFleetRepository : FleetRepository {

    private val _stations = MutableStateFlow(initialStations())
    override val stations: StateFlow<List<ChargingStation>> = _stations.asStateFlow()

    private val _allScooters = MutableStateFlow(initialScooters())
    override val allScooters: StateFlow<List<Scooter>> = _allScooters.asStateFlow()

    private val _activeConnection = MutableStateFlow<ActiveConnectionTarget>(
        ActiveConnectionTarget.CentralServer(8081)
    )
    override val activeConnection: StateFlow<ActiveConnectionTarget> = _activeConnection.asStateFlow()

    private val _protocolLogs = MutableStateFlow(
        listOf(
            DemoProtocolLog(
                id = UUID.randomUUID().toString(),
                timestampLabel = nowTimeLabel(),
                channel = ProtocolChannel.CENTRAL_REST,
                operation = "CLUSTER_READY",
                details = "Central Server (:8081) + 4 Edge Station Nodes active in Local Demo Mode."
            )
        )
    )
    override val protocolLogs: StateFlow<List<DemoProtocolLog>> = _protocolLogs.asStateFlow()

    override fun getNearbyScooters(userLat: Double, userLon: Double, radiusMeters: Int): List<Scooter> {
        val nearby = _allScooters.value
            .map { scooter ->
                val dist = haversineMeters(userLat, userLon, scooter.lat, scooter.lon).roundToInt()
                scooter.copy(distanceFromUserMeters = dist)
            }
            .filter { it.status == ScooterStatus.AVAILABLE && it.distanceFromUserMeters <= radiusMeters }
            .sortedBy { it.distanceFromUserMeters }

        appendLog(
            channel = ProtocolChannel.CENTRAL_REST,
            operation = "GET /api/v1/scooters/nearby",
            details = "lat=%.4f, lon=%.4f, r=${radiusMeters}m -> ${nearby.size} available".format(
                Locale.US,
                userLat,
                userLon
            )
        )
        return nearby
    }

    private fun appendLog(channel: ProtocolChannel, operation: String, details: String) {
        val log = DemoProtocolLog(
            id = UUID.randomUUID().toString(),
            timestampLabel = nowTimeLabel(),
            channel = channel,
            operation = operation,
            details = details
        )
        _protocolLogs.update { list -> (listOf(log) + list).take(60) }
    }

    private fun nowTimeLabel(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
    }

    companion object {
        fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val earthRadius = 6371000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return earthRadius * c
        }

        private fun initialStations() = listOf(
            ChargingStation(
                stationId = "station_a",
                name = "Victory Square Station",
                lat = 45.7537,
                lon = 21.2257,
                capacity = 12,
                dockedCount = 4,
                port = 8084,
                status = StationStatus.ACTIVE,
                dockerContainerId = "c8f1a904e2b1"
            ),
            ChargingStation(
                stationId = "station_b",
                name = "Central Park Station",
                lat = 45.7506,
                lon = 21.2208,
                capacity = 12,
                dockedCount = 2,
                port = 8085,
                status = StationStatus.ACTIVE,
                dockerContainerId = "d41e90ab72c0"
            ),
            ChargingStation(
                stationId = "station_c",
                name = "Union Square Station",
                lat = 45.7579,
                lon = 21.2291,
                capacity = 10,
                dockedCount = 3,
                port = 8086,
                status = StationStatus.ACTIVE,
                dockerContainerId = "9a33f10bc4d9"
            ),
            ChargingStation(
                stationId = "station_d",
                name = "Bega Promenade Station",
                lat = 45.7492,
                lon = 21.2355,
                capacity = 12,
                dockedCount = 2,
                port = 8087,
                status = StationStatus.ACTIVE,
                dockerContainerId = "77be210fa910"
            )
        )

        private fun initialScooters() = listOf(
            Scooter("sc_101", "Ninebot Max G2", 92, ScooterStatus.AVAILABLE, 45.7540, 21.2259, "station_a", "Victory Square Station", "http://station-a:8084", 8084),
            Scooter("sc_102", "Ninebot Max G2", 84, ScooterStatus.AVAILABLE, 45.7534, 21.2252, "station_a", "Victory Square Station", "http://station-a:8084", 8084),
            Scooter("sc_103", "Xiaomi Pro 4", 77, ScooterStatus.AVAILABLE, 45.7544, 21.2266, "station_a", "Victory Square Station", "http://station-a:8084", 8084),
            Scooter("sc_104", "Segway Urban E+", 96, ScooterStatus.AVAILABLE, 45.7528, 21.2248, "station_a", "Victory Square Station", "http://station-a:8084", 8084),

            Scooter("sc_201", "Ninebot Max G2", 88, ScooterStatus.AVAILABLE, 45.7508, 21.2211, "station_b", "Central Park Station", "http://station-b:8085", 8085),
            Scooter("sc_202", "Xiaomi Pro 4", 65, ScooterStatus.AVAILABLE, 45.7502, 21.2202, "station_b", "Central Park Station", "http://station-b:8085", 8085),

            Scooter("sc_301", "Segway Urban E+", 91, ScooterStatus.AVAILABLE, 45.7576, 21.2288, "station_c", "Union Square Station", "http://station-c:8086", 8086),
            Scooter("sc_302", "Ninebot Max G2", 79, ScooterStatus.AVAILABLE, 45.7583, 21.2295, "station_c", "Union Square Station", "http://station-c:8086", 8086),
            Scooter("sc_303", "Xiaomi Pro 4", 54, ScooterStatus.AVAILABLE, 45.7573, 21.2282, "station_c", "Union Square Station", "http://station-c:8086", 8086),

            Scooter("sc_401", "Ninebot Max G2", 95, ScooterStatus.AVAILABLE, 45.7495, 21.2351, "station_d", "Bega Promenade Station", "http://station-d:8087", 8087),
            Scooter("sc_402", "Xiaomi Pro 4", 81, ScooterStatus.AVAILABLE, 45.7488, 21.2361, "station_d", "Bega Promenade Station", "http://station-d:8087", 8087)
        )
    }
}