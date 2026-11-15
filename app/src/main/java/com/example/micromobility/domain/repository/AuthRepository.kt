package com.example.micromobility.domain.repository

import com.example.micromobility.domain.model.UserSession

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<UserSession>
    suspend fun register(fullName: String, email: String, password: String): Result<UserSession>
    suspend fun logout()
}