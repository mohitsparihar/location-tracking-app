package com.example.locationtracker

import android.app.Application
import androidx.room.Room
import com.example.locationtracker.data.local.AppDatabase
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

        container = AppContainer(authRepository, locationRepository)
    }
}

data class AppContainer(
    val authRepository: AuthRepository,
    val locationRepository: LocationRepository
)
