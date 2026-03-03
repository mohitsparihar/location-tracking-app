package com.example.locationtracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.locationtracker.AppContainer
import com.example.locationtracker.data.local.LocationEntity
import com.example.locationtracker.data.network.ApiConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// COMPLIANCE ADDED: Added preferences datastore for tracking consent given
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class MainViewModel(private val container: AppContainer) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // Check for mandatory update first
        checkForceUpdate()

        viewModelScope.launch {
            combine(
                container.authRepository.isSignedIn,
                container.locationRepository.observeLocations(),
                container.authRepository.userEmail
            ) { signedIn, locations, email ->
                // Simple tuple returning since combine can take up to 4 / 5 arguments
                object {
                    val signedIn = signedIn
                    val locations = locations
                    val email = email
                }
            }.collect { data ->
                _uiState.update {
                    it.copy(
                        checkingAuth = false,
                        isSignedIn = data.signedIn,
                        locations = data.locations,
                        userEmail = data.email
                    )
                }
            }
        }
        
        // COMPLIANCE ADDED: Observe consent
        viewModelScope.launch {
            val consentKey = booleanPreferencesKey("consent_given")
            container.context.dataStore.data
                .map { preferences -> preferences[consentKey] ?: false }
                .collect { consent ->
                    _uiState.update { it.copy(consentGiven = consent) }
                }
        }
    }

    /**
     * Fetches the latest required version from the API and sets forceUpdateRequired = true
     * if the installed versionName does not match the server's [version] field.
     * API: GET /app/getAppConfig?app=location-tracker&platform=android
     */
    private fun checkForceUpdate() {
        viewModelScope.launch {
            try {
                val installedVersion = getInstalledVersionName()
                val response = container.apiService.checkAppVersion(ApiConfig.APP_VERSION_CHECK_URL)
                if (response.isSuccessful) {
                    val serverVersion = response.body()?.version
                    if (!serverVersion.isNullOrBlank() && installedVersion.isOlderThan(serverVersion.trim())) {
                        _uiState.update { it.copy(forceUpdateRequired = true, serverVersion = serverVersion.trim()) }
                    }
                }
            } catch (e: Exception) {
                // Network errors are silently ignored — don't block users if the
                // version check endpoint is temporarily unavailable.
            }
        }
    }

    /** Returns the versionName declared in build.gradle.kts (e.g. "1.0") */
    private fun getInstalledVersionName(): String {
        return try {
            val pInfo = container.context.packageManager
                .getPackageInfo(container.context.packageName, 0)
            pInfo.versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Compares two semantic version strings numerically, part by part.
     * "1.0".isOlderThan("1.1.0") == true
     * "1.0".isOlderThan("0.9")   == false
     * "1.0".isOlderThan("1.0")   == false
     */
    private fun String.isOlderThan(other: String): Boolean {
        val thisParts  = this.trim().split(".").map { it.toIntOrNull() ?: 0 }
        val otherParts = other.trim().split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(thisParts.size, otherParts.size)
        for (i in 0 until maxLen) {
            val a = thisParts.getOrElse(i) { 0 }
            val b = otherParts.getOrElse(i) { 0 }
            if (a < b) return true   // installed is behind → needs update
            if (a > b) return false  // installed is ahead  → no update needed
        }
        return false // versions are equal
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(authError = "Email and password are required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(loginInProgress = true, authError = null) }
            val result = container.authRepository.login(email.trim(), password)
            if (result.isSuccess) {
                container.locationRepository.uploadPendingLocations()
                _uiState.update { it.copy(loginInProgress = false, authError = null) }
            } else {
                val message = result.exceptionOrNull()?.message ?: "Login failed"
                _uiState.update { it.copy(loginInProgress = false, authError = message) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            container.authRepository.logout()
            container.locationRepository.clearAll()
        }
    }

    fun syncPendingLocations() {
        viewModelScope.launch {
            container.locationRepository.uploadPendingLocations()
        }
    }

    // COMPLIANCE ADDED: Method to save consent
    fun acceptConsent() {
        viewModelScope.launch {
            val consentKey = booleanPreferencesKey("consent_given")
            container.context.dataStore.edit { preferences ->
                preferences[consentKey] = true
            }
            _uiState.update { it.copy(consentGiven = true) }
        }
    }
}

data class UiState(
    val checkingAuth: Boolean = true,
    val isSignedIn: Boolean = false,
    val loginInProgress: Boolean = false,
    val authError: String? = null,
    val locations: List<LocationEntity> = emptyList(),
    val userEmail: String? = null,
    val consentGiven: Boolean = false, // COMPLIANCE ADDED: state var for consent
    val forceUpdateRequired: Boolean = false,
    val serverVersion: String? = null
)

class MainViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(container) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
