package com.example.micromobility.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.micromobility.core.location.LocationTracker
import com.example.micromobility.core.location.UserLocationState
import com.example.micromobility.data.repository.DemoFleetRepository
import com.example.micromobility.domain.model.ActiveConnectionTarget
import com.example.micromobility.domain.model.ChargingStation
import com.example.micromobility.domain.model.Scooter
import com.example.micromobility.domain.model.ScooterStatus
import com.example.micromobility.domain.repository.FleetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MapDiscoveryUiState(
    val userLocation: UserLocationState = LocationTracker.defaultDemoLocation(),
    val searchRadiusMeters: Int = 750,
    val stations: List<ChargingStation> = emptyList(),
    val nearbyScooters: List<Scooter> = emptyList(),
    val allScooters: List<Scooter> = emptyList(),
    val selectedScooterId: String? = "sc_101",
    val activeConnection: ActiveConnectionTarget = ActiveConnectionTarget.CentralServer(8081),
    val snackbarMessage: String? = null
) {
    val selectedScooter: Scooter?
        get() = nearbyScooters.find { it.scooterId == selectedScooterId }
            ?: allScooters.find { it.scooterId == selectedScooterId }
}

class MapDiscoveryViewModel(
    private val locationTracker: LocationTracker,
    private val fleetRepository: FleetRepository
) : ViewModel() {

    private val _userLocation = MutableStateFlow(LocationTracker.defaultDemoLocation())
    private val _radiusMeters = MutableStateFlow(750)
    private val _selectedScooterId = MutableStateFlow<String?>("sc_101")
    private val _snackbarMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MapDiscoveryUiState> = combine(
        _userLocation,
        _radiusMeters,
        _selectedScooterId,
        fleetRepository.stations,
        fleetRepository.allScooters,
        fleetRepository.activeConnection,
        _snackbarMessage
    ) { values ->
        val loc = values[0] as UserLocationState
        val radius = values[1] as Int
        val selectedId = values[2] as String?
        @Suppress("UNCHECKED_CAST")
        val stations = values[3] as List<ChargingStation>
        @Suppress("UNCHECKED_CAST")
        val allScooters = values[4] as List<Scooter>
        val conn = values[5] as ActiveConnectionTarget
        val snackbar = values[6] as String?

        val nearby = allScooters
            .map { sc ->
                val dist = DemoFleetRepository.haversineMeters(loc.lat, loc.lon, sc.lat, sc.lon).toInt()
                sc.copy(distanceFromUserMeters = dist)
            }
            .filter { sc ->
                sc.status == ScooterStatus.AVAILABLE && sc.distanceFromUserMeters <= radius
            }
            .sortedBy { it.distanceFromUserMeters }

        MapDiscoveryUiState(
            userLocation = loc,
            searchRadiusMeters = radius,
            stations = stations,
            nearbyScooters = nearby,
            allScooters = allScooters,
            selectedScooterId = selectedId,
            activeConnection = conn,
            snackbarMessage = snackbar
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = MapDiscoveryUiState()
    )

    init {
        refreshUserLocation()
    }

    fun refreshUserLocation() {
        viewModelScope.launch {
            val loc = locationTracker.fetchCurrentLocation()
            _userLocation.value = loc
            fleetRepository.getNearbyScooters(loc.lat, loc.lon, _radiusMeters.value)
        }
    }

    fun setSearchRadius(radiusMeters: Int) {
        _radiusMeters.value = radiusMeters
        val loc = _userLocation.value
        fleetRepository.getNearbyScooters(loc.lat, loc.lon, radiusMeters)
    }

    fun selectScooter(scooterId: String?) {
        _selectedScooterId.value = scooterId
    }

    fun showStepPendingMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    companion object {
        fun provideFactory(
            locationTracker: LocationTracker,
            fleetRepository: FleetRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MapDiscoveryViewModel(locationTracker, fleetRepository) as T
            }
        }
    }
}