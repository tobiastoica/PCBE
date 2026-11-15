package com.example.micromobility.core.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.micromobility.domain.model.UserSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException

private val Context.sessionDataStore by preferencesDataStore(name = "micromobility_session_prefs")

class DataStoreSessionManager(private val context: Context) : SessionStorage {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _session = MutableStateFlow<UserSession?>(null)
    override val session: StateFlow<UserSession?> = _session.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    override val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    companion object {
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_FULL_NAME = stringPreferencesKey("full_name")
        private val KEY_EMAIL = stringPreferencesKey("email")
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
    }

    init {
        scope.launch {
            val prefs = context.sessionDataStore.data
                .catch { e ->
                    if (e is IOException) emit(emptyPreferences()) else throw e
                }
                .first()

            val userId = prefs[KEY_USER_ID]
            val fullName = prefs[KEY_FULL_NAME]
            val email = prefs[KEY_EMAIL]
            val token = prefs[KEY_ACCESS_TOKEN]

            if (userId != null && fullName != null && email != null && token != null) {
                _session.value = UserSession(userId, fullName, email, token)
            }
            _isLoaded.value = true
        }
    }

    override suspend fun saveSession(userSession: UserSession) {
        context.sessionDataStore.edit { prefs ->
            prefs[KEY_USER_ID] = userSession.userId
            prefs[KEY_FULL_NAME] = userSession.fullName
            prefs[KEY_EMAIL] = userSession.email
            prefs[KEY_ACCESS_TOKEN] = userSession.accessToken
        }
        _session.value = userSession
    }

    override suspend fun clearSession() {
        context.sessionDataStore.edit { it.clear() }
        _session.value = null
    }
}