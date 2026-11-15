package com.example.micromobility.di

import android.content.Context
import com.example.micromobility.core.location.LocationTracker
import com.example.micromobility.core.session.DataStoreSessionManager
import com.example.micromobility.core.session.SessionStorage
import com.example.micromobility.data.repository.DemoAuthRepository
import com.example.micromobility.data.repository.DemoFleetRepository
import com.example.micromobility.domain.repository.AuthRepository
import com.example.micromobility.domain.repository.FleetRepository

class AppContainer(context: Context) {
    val sessionStorage: SessionStorage = DataStoreSessionManager(context)

    val authRepository: AuthRepository by lazy {
        DemoAuthRepository(sessionStorage)
    }

    val locationTracker: LocationTracker by lazy {
        LocationTracker(context.applicationContext)
    }

    val fleetRepository: FleetRepository by lazy {
        DemoFleetRepository()
    }
}