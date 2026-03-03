package com.example.locationtracker

import android.app.Application
import androidx.room.Room
import com.example.locationtracker.data.local.AppDatabase
import com.example.locationtracker.data.network.ApiService
import com.example.locationtracker.data.network.NetworkModule
import com.example.locationtracker.data.repository.AuthRepository
import com.example.locationtracker.data.repository.LocationRepository

class LocationTrackerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "location_tracker.db"
        ).fallbackToDestructiveMigration().build()

        val apiService = NetworkModule.createApiService()
        val authRepository = AuthRepository(applicationContext, apiService)
        val locationRepository = LocationRepository(
            context = applicationContext,
            dao = db.locationDao(),
            apiService = apiService,
            authRepository = authRepository
        )

        container = AppContainer(applicationContext, authRepository, locationRepository, apiService)
    }
}

// COMPLIANCE ADDED: Context is needed in container for DataStore
data class AppContainer(
    val context: android.content.Context,
    val authRepository: AuthRepository,
    val locationRepository: LocationRepository,
    val apiService: ApiService
)
