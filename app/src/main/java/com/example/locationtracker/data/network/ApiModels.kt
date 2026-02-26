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
