package com.example.micromobility.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class UserLocationState(
    val lat: Double,
    val lon: Double,
    val isRealGpsFix: Boolean,
    val label: String
)

class LocationTracker(private val context: Context) {

    private val fusedClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(): UserLocationState {
        if (!hasLocationPermission()) {
            return defaultDemoLocation("Timișoara Center (Demo Mode)")
        }

        val lastOrCurrent = suspendCancellableCoroutine<android.location.Location?> { cont ->
            val cts = CancellationTokenSource()
            cont.invokeOnCancellation { cts.cancel() }

            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        cont.resume(location)
                    } else {
                        fusedClient.lastLocation
                            .addOnSuccessListener { last -> cont.resume(last) }
                            .addOnFailureListener { cont.resume(null) }
                    }
                }
                .addOnFailureListener {
                    cont.resume(null)
                }
        }

        return when {
            lastOrCurrent != null && isNearDemoCity(lastOrCurrent.latitude, lastOrCurrent.longitude) -> {
                UserLocationState(
                    lat = lastOrCurrent.latitude,
                    lon = lastOrCurrent.longitude,
                    isRealGpsFix = true,
                    label = "Active Device GPS"
                )
            }
            lastOrCurrent != null -> {
                // Keep demo fleet discoverable if emulator GPS is in another region (e.g., Mountain View)
                defaultDemoLocation("Timișoara Demo (External Emulator GPS)")
            }
            else -> {
                defaultDemoLocation("Timișoara Demo (FusedLocation fallback)")
            }
        }
    }

    private fun isNearDemoCity(lat: Double, lon: Double): Boolean {
        return lat in 45.65..45.85 && lon in 21.10..21.35
    }

    companion object {
        const val DEFAULT_LAT = 45.7537
        const val DEFAULT_LON = 21.2257

        fun defaultDemoLocation(label: String = "Timișoara Center (Demo)"): UserLocationState {
            return UserLocationState(
                lat = DEFAULT_LAT,
                lon = DEFAULT_LON,
                isRealGpsFix = false,
                label = label
            )
        }
    }
}