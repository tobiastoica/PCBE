package com.example.micromobility.data.repository

import com.example.micromobility.core.session.SessionStorage
import com.example.micromobility.domain.model.UserSession
import com.example.micromobility.domain.repository.AuthRepository
import kotlinx.coroutines.delay

class DemoAuthRepository(
    private val sessionStorage: SessionStorage,
    private val simulatedDelayMs: Long = 550L
) : AuthRepository {

    private data class MockUser(
        val userId: String,
        val fullName: String,
        val email: String,
        val password: String
    )

    private val users = mutableListOf(
        MockUser(
            userId = "usr_1",
            fullName = "John Doe",
            email = "john.doe1@example.com",
            password = "john.doe1@password"
        )
    )

    override suspend fun login(email: String, password: String): Result<UserSession> {
        if (simulatedDelayMs > 0) delay(simulatedDelayMs)
        val normalizedEmail = email.trim().lowercase()
        val user = users.find { it.email.lowercase() == normalizedEmail && it.password == password }
            ?: return Result.failure(IllegalArgumentException("Invalid email or password."))

        val session = UserSession(
            userId = user.userId,
            fullName = user.fullName,
            email = user.email,
            accessToken = "demo-token-${user.userId}"
        )
        sessionStorage.saveSession(session)
        return Result.success(session)
    }

    override suspend fun register(
        fullName: String,
        email: String,
        password: String
    ): Result<UserSession> {
        if (simulatedDelayMs > 0) delay(simulatedDelayMs)

        val normalizedEmail = email.trim().lowercase()
        if (users.any { it.email.lowercase() == normalizedEmail }) {
            return Result.failure(IllegalStateException("An account with this email already exists."))
        }

        val passwordError = com.example.micromobility.core.validation.PasswordValidator.validate(password)
        if (passwordError != null) {
            return Result.failure(IllegalArgumentException(passwordError))
        }

        val newUser = MockUser(
            userId = "usr_${1 + users.size}",
            fullName = fullName.trim(),
            email = normalizedEmail,
            password = password
        )
        users.add(newUser)

        val session = UserSession(
            userId = newUser.userId,
            fullName = newUser.fullName,
            email = newUser.email,
            accessToken = "demo-token-${newUser.userId}"
        )
        sessionStorage.saveSession(session)
        return Result.success(session)
    }

    override suspend fun logout() {
        sessionStorage.clearSession()
    }
}