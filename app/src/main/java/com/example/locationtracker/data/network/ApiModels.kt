package com.example.locationtracker.data.network

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("email_id") val emailId: String,
    val password: String
)

data class LoginResponse(
    @SerializedName("in") val loginData: LoginUser?,
    val user: LoginUser?,
    val message: String?,
    val status: Int?
) {
    /** Returns whichever payload the server sent (in or user) */
    val resolvedData: LoginUser? get() = loginData ?: user
}

data class LoginUser(
    val token: String?,
    val email: String?,
    @SerializedName("is_onboarding_completed") val isOnboardingCompleted: Boolean?,
    val message: String?
)

data class UploadLocationRequest(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?,
    val altitude: Double?,
    val altitudeAccuracy: Float?,
    val heading: Float?,
    val speed: Float?,
    val deviceId: String,
    val deviceName: String?,
    val deviceModel: String?,
    val deviceBrand: String?,
    val osName: String,
    val osVersion: String,
    val appVersion: String?,
    val timestamp: String,
    val clientTimestamp: Long,
    val isBackground: Boolean
)

data class UploadLocationBatchRequest(
    val items: List<UploadLocationRequest>
)

data class UploadLocationResponse(
    val data: UploadLocationResponseData?
)

data class UploadLocationResponseData(
    val token: String?
)

/**
 * Response from the app version-check endpoint.
 * API: GET /app/getAppConfig?app=location-tracker&platform=android
 * Example: { "version": "1.0", "version_code": 1, "app": "location-tracker", "platform": "android", "status": 200 }
 */
data class AppVersionResponse(
    @SerializedName("version") val version: String?,
    @SerializedName("version_code") val versionCode: Int?,
    @SerializedName("status") val status: Int?
)
