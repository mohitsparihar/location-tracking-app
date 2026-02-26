package com.example.locationtracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.locationtracker.AppContainer
import com.example.locationtracker.data.local.LocationEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(private val container: AppContainer) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                container.authRepository.isSignedIn,
                container.locationRepository.observeLocations()
            ) { signedIn, locations ->
                Pair(signedIn, locations)
            }.collect { (signedIn, locations) ->
                _uiState.update {
                    it.copy(
                        checkingAuth = false,
                        isSignedIn = signedIn,
                        locations = locations
                    )
                }
            }
        }
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
        }
    }

    fun syncPendingLocations() {
        viewModelScope.launch {
            container.locationRepository.uploadPendingLocations()
        }
    }
}

data class UiState(
    val checkingAuth: Boolean = true,
    val isSignedIn: Boolean = false,
    val loginInProgress: Boolean = false,
    val authError: String? = null,
    val locations: List<LocationEntity> = emptyList()
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
