package com.example.locationtracker.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.locationtracker.LocationTrackerApp
import com.example.locationtracker.data.repository.LocationRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Calendar

class LocationCaptureService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRepository: LocationRepository

    private var tracking = false

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            if (!isWithinTrackingWindow()) return
            result.locations.forEach { location ->
                serviceScope.launch {
                    locationRepository.recordLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        altitude = if (location.hasAltitude()) location.altitude else null,
                        altitudeAccuracy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasVerticalAccuracy()) {
                            location.verticalAccuracyMeters
                        } else {
                            null
                        },
                        heading = if (location.hasBearing()) location.bearing else null,
                        speed = if (location.hasSpeed()) location.speed else null,
                        timestamp = location.time,
                        isBackground = true
                    )
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val app = application as LocationTrackerApp
        locationRepository = app.container.locationRepository
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, foregroundNotification())
        if (!tracking) {
            tracking = true
            startLocationUpdates()
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (!hasLocationPermission()) {
            stopSelf()
            return
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, TEN_MINUTES_MS)
            .setMinUpdateIntervalMillis(TEN_MINUTES_MS)
            .setMaxUpdateDelayMillis(TEN_MINUTES_MS)
            .build()

        fusedLocationClient.requestLocationUpdates(request, callback, mainLooper)
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val bg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return fine && bg
    }

    private fun foregroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location tracking active")
            .setContentText("Recording and syncing your location every 10 minutes")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Location Tracking",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(callback)
        serviceScope.cancel()
    }

    override fun onBind(intent: android.content.Intent?): IBinder? = null

    private fun isWithinTrackingWindow(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        // Track from 06:00 (inclusive) until 24:00 (exclusive).
        return hour >= 6
    }

    companion object {
        private const val TEN_MINUTES_MS = 10 * 60 * 1000L
        private const val CHANNEL_ID = "location_tracking"
        private const val NOTIFICATION_ID = 77
    }
}
