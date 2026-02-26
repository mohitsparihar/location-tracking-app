package com.example.locationtracker.data.repository

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.locationtracker.data.network.ApiConfig
import com.example.locationtracker.data.network.ApiService
import com.example.locationtracker.data.network.LoginRequest
import org.json.JSONObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "auth_prefs")

class AuthRepository(
    private val context: Context,
    private val apiService: ApiService
) {
    private val signedInKey = booleanPreferencesKey("signed_in")
    private val tokenKey = stringPreferencesKey("token")
    private val emailKey = stringPreferencesKey("user_email")

    val isSignedIn: Flow<Boolean> = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs -> prefs[signedInKey] ?: false }

    val tokenFlow: Flow<String?> = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs -> prefs[tokenKey] }

    val userEmail: Flow<String?> = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs -> prefs[emailKey] }

    suspend fun login(email: String, password: String): Result<Unit> {
        return runCatching {
            val response = apiService.login(
                url = ApiConfig.LOGIN_URL,
                request = LoginRequest(emailId = email, password = password)
            )

            val body = response.body()
            if (!response.isSuccessful) {
                val raw = response.errorBody()?.string().orEmpty()
                val message = parseMessage(raw) ?: body?.message ?: "Login failed"
                error(message)
            }

            if (body?.status == 401) {
                error(body.resolvedData?.message ?: body.message ?: "Invalid credentials")
            }

            val data = body?.resolvedData
            val token = data?.token
            if (token.isNullOrBlank()) {
                error(data?.message ?: body?.message ?: "Invalid credentials")
            }

            context.dataStore.edit { prefs: MutablePreferences ->
                prefs[signedInKey] = true
                prefs[tokenKey] = token
                data?.email?.let { prefs[emailKey] = it }
            }
        }
    }

    suspend fun logout() {
        context.dataStore.edit { prefs: MutablePreferences ->
            prefs[signedInKey] = false
            prefs.remove(tokenKey)
            prefs.remove(emailKey)
        }
    }

    suspend fun setToken(token: String) {
        context.dataStore.edit { prefs: MutablePreferences ->
            prefs[tokenKey] = token
        }
    }

    suspend fun getTokenNow(): String? {
        return tokenFlow.firstOrNull()
    }

    private fun parseMessage(rawJson: String): String? {
        return runCatching { JSONObject(rawJson).optString("message").takeIf { it.isNotBlank() } }
            .getOrNull()
    }
}
