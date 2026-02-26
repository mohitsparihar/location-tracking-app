package com.example.locationtracker.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface ApiService {
    @POST
    suspend fun login(
        @Url url: String,
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST
    suspend fun uploadLocation(
        @Url url: String,
        @Header("Authorization") bearerToken: String,
        @Body request: UploadLocationRequest
    ): Response<UploadLocationResponse>

    @POST
    suspend fun uploadLocationBatch(
        @Url url: String,
        @Header("Authorization") bearerToken: String,
        @Body request: UploadLocationBatchRequest
    ): Response<UploadLocationResponse>
}
