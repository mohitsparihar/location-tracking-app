package com.example.locationtracker.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.example.locationtracker.data.local.LocationDao
import com.example.locationtracker.data.local.LocationEntity
import com.example.locationtracker.data.network.ApiConfig
import com.example.locationtracker.data.network.ApiService
import com.example.locationtracker.data.network.UploadLocationBatchRequest
import com.example.locationtracker.data.network.UploadLocationRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.random.Random

class LocationRepository(
    private val context: Context,
    private val dao: LocationDao,
    private val apiService: ApiService,
    private val authRepository: AuthRepository
) {
    fun observeLocations(): Flow<List<LocationEntity>> = dao.observeAll()

    suspend fun recordLocation(
        latitude: Double,
        longitude: Double,
        accuracy: Float?,
        altitude: Double?,
        altitudeAccuracy: Float?,
        heading: Float?,
        speed: Float?,
        timestamp: Long,
        isBackground: Boolean
    ) {
        withContext(Dispatchers.IO) {
            val id = dao.insert(
                LocationEntity(
                    latitude = latitude,
                    longitude = longitude,
                    accuracy = accuracy,
                    altitude = altitude,
                    altitudeAccuracy = altitudeAccuracy,
                    heading = heading,
                    speed = speed,
                    timestamp = timestamp,
                    isBackground = isBackground,
                    uploaded = false
                )
            )

            // Spread load for background updates.
            if (isBackground) {
                delay(Random.nextLong(1_000L, 8_000L))
            }

            uploadByIdIfPossible(id)
        }
    }

    suspend fun uploadPendingLocations() {
        withContext(Dispatchers.IO) {
            val token = authRepository.tokenFlow.firstOrNull() ?: return@withContext
            val pending = dao.getPendingUploads()
            if (pending.isEmpty()) return@withContext

            if (pending.size == 1) {
                val entity = pending.first()
                if (uploadSingle(entity, token)) {
                    dao.markUploaded(entity.id)
                }
                return@withContext
            }

            if (uploadBatch(pending, token)) {
                pending.forEach { entity ->
                    dao.markUploaded(entity.id)
                }
            }
        }
    }

    private suspend fun uploadByIdIfPossible(id: Long) {
        val token = authRepository.tokenFlow.firstOrNull() ?: return
        val pending = dao.getPendingUploads().firstOrNull { it.id == id } ?: return
        if (uploadSingle(pending, token)) {
            dao.markUploaded(pending.id)
        }
    }

    private suspend fun uploadSingle(entity: LocationEntity, token: String): Boolean {
        val response = apiService.uploadLocation(
            url = ApiConfig.LOCATION_UPLOAD_URL,
            bearerToken = "Bearer $token",
            request = toUploadRequest(entity)
        )

        if (response.code() in setOf(401, 403, 498)) {
            authRepository.logout()
            return false
        }

        return response.isSuccessful
    }

    private suspend fun uploadBatch(entities: List<LocationEntity>, token: String): Boolean {
        val response = apiService.uploadLocationBatch(
            url = ApiConfig.LOCATION_UPLOAD_BATCH_URL,
            bearerToken = "Bearer $token",
            request = UploadLocationBatchRequest(items = entities.map(::toUploadRequest))
        )

        if (response.code() in setOf(401, 403, 498)) {
            authRepository.logout()
            return false
        }

        return response.isSuccessful
    }

    private fun toUploadRequest(entity: LocationEntity): UploadLocationRequest {
        return UploadLocationRequest(
            latitude = entity.latitude,
            longitude = entity.longitude,
            accuracy = entity.accuracy,
            altitude = entity.altitude,
            altitudeAccuracy = entity.altitudeAccuracy,
            heading = entity.heading,
            speed = entity.speed,
            deviceId = getDeviceId(),
            deviceName = Build.DEVICE,
            deviceModel = Build.MODEL,
            deviceBrand = Build.BRAND,
            osName = "android",
            osVersion = Build.VERSION.RELEASE ?: "unknown",
            appVersion = getAppVersion(),
            timestamp = formatUtcTimestamp(entity.timestamp),
            clientTimestamp = entity.timestamp,
            isBackground = entity.isBackground
        )
    }

    @SuppressLint("HardwareIds")
    private fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown-android-id"
    }

    private fun formatUtcTimestamp(millis: Long): String {
        val formatter = SimpleDateFormat("MMMM d, yyyy 'at' h:mm:ss a 'UTC'", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date(millis))
    }

    private fun getAppVersion(): String? {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
}
