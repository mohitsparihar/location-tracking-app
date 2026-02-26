package com.example.locationtracker.data.network

object ApiConfig {
    // Equivalent to EXPO_PUBLIC_API_BASE_URL
    const val API_BASE_URL = "https://beapis-in.staging.geoiq.ai/retailapp/stg/v3/"

    // Equivalent to EXPO_PUBLIC_LOCATION_UPLOAD_URL (full URL)
    const val LOCATION_UPLOAD_URL =
        "https://beapis-in.staging.geoiq.ai/bdapp/stg/v1/bd/locationTracking/updateUserLocation"
    const val LOCATION_UPLOAD_BATCH_URL =
        "https://beapis-in.staging.geoiq.ai/bdapp/stg/v1/bd/locationTracking/updateUserLocationBatch"

    const val LOGIN_PATH = "user/userlogin"

    // Config only (currently unused)
    const val FORGOT_PASSWORD_PATH = "user/forgot-password"
    const val RESET_PASSWORD_PATH = "user/reset-password"
}
