package com.example.micromobility.core.session

import com.example.micromobility.domain.model.UserSession
import kotlinx.coroutines.flow.StateFlow

interface SessionStorage {
    val session: StateFlow<UserSession?>
    val isLoaded: StateFlow<Boolean>
    suspend fun saveSession(userSession: UserSession)
    suspend fun clearSession()
}