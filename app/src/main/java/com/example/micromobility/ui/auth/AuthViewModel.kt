package com.example.micromobility.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.micromobility.core.validation.PasswordValidator
import com.example.micromobility.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onFullNameChange(value: String) = _uiState.update { it.copy(fullName = value, errorMessage = null) }
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, errorMessage = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, errorMessage = null) }
    fun onConfirmPasswordChange(value: String) = _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }
    fun clearFormErrors() = _uiState.update { it.copy(errorMessage = null) }

    fun login() {
        val state = _uiState.value
        if (state.isLoading) return

        if (!isValidEmail(state.email)) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address.") }
            return
        }
        val passwordError = PasswordValidator.validate(state.password)
        if (passwordError != null) {
            _uiState.update { it.copy(errorMessage = passwordError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.login(state.email, state.password)
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun loginAsDemoUser() {
        if (_uiState.value.isLoading) return
        _uiState.update {
            it.copy(
                email = "john.doe1@example.com",
                password = "john.doe1@password",
                errorMessage = null
            )
        }
        login()
    }

    fun register() {
        val state = _uiState.value
        if (state.isLoading) return

        if (state.fullName.trim().length < 3) {
            _uiState.update { it.copy(errorMessage = "Please enter your full name (at least 3 characters).") }
            return
        }
        if (!isValidEmail(state.email)) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address.") }
            return
        }
        val passwordError = PasswordValidator.validate(state.password)
        if (passwordError != null) {
            _uiState.update { it.copy(errorMessage = passwordError) }
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.register(state.fullName, state.email, state.password)
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return EMAIL_REGEX.matches(email.trim())
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

        fun provideFactory(authRepository: AuthRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(authRepository) as T
                }
            }
    }
}