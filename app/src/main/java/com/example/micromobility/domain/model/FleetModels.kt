package com.example.micromobility.domain.model

enum class ScooterStatus {
    AVAILABLE,
    RESERVED,
    MOVING,
    PAUSED,
    LOW_BATTERY,
    MAINTENANCE
}

enum class StationStatus {
    ACTIVE,
    MAINTENANCE,
    OFFLINE
}

enum class ProtocolChannel(val badge: String) {
    CENTRAL_REST("CENTRAL :8081"),
    EDGE_REST("EDGE REST"),
    EDGE_SSE("EDGE SSE 3s"),
    KAFKA_BUS("KAFKA"),
    MCP_AGENT("MCP :8083")
}

sealed interface ActiveConnectionTarget {
    data class CentralServer(val port: Int = 8081) : ActiveConnectionTarget
    data class EdgeStationNode(
        val stationId: String,
        val stationName: String,
        val port: Int
    ) : ActiveConnectionTarget
}

data class ChargingStation(
    val stationId: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val capacity: Int,
    val dockedCount: Int,
    val port: Int,
    val status: StationStatus = StationStatus.ACTIVE,
    val dockerContainerId: String
) {
    val url: String get() = "http://$stationId:$port"
}

data class Scooter(
    val scooterId: String,
    val model: String,
    val batteryLevel: Int,
    val status: ScooterStatus,
    val lat: Double,
    val lon: Double,
    val stationId: String,
    val stationName: String,
    val stationUrl: String,
    val stationPort: Int,
    val distanceFromUserMeters: Int = 0
)

data class DemoProtocolLog(
    val id: String,
    val timestampLabel: String,
    val channel: ProtocolChannel,
    val operation: String,
    val details: String
)