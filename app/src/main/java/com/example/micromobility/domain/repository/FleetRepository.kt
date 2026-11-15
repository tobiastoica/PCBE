package com.example.micromobility.domain.repository

import com.example.micromobility.domain.model.ActiveConnectionTarget
import com.example.micromobility.domain.model.ChargingStation
import com.example.micromobility.domain.model.DemoProtocolLog
import com.example.micromobility.domain.model.Scooter
import kotlinx.coroutines.flow.StateFlow

interface FleetRepository {
    val stations: StateFlow<List<ChargingStation>>
    val allScooters: StateFlow<List<Scooter>>
    val activeConnection: StateFlow<ActiveConnectionTarget>
    val protocolLogs: StateFlow<List<DemoProtocolLog>>

    fun getNearbyScooters(userLat: Double, userLon: Double, radiusMeters: Int): List<Scooter>
}