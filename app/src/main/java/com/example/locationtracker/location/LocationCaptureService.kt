package com.example.locationtracker.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.locationtracker.LocationTrackerApp
import com.example.locationtracker.MainActivity
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.util.Calendar

class LocationCaptureService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRepository: LocationRepository

    private var tracking = false
    private var notificationJob: Job? = null

    // Continuous callback removed for compliance

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
        
        // Periodic checks and notification re-posts
        notificationJob?.cancel()
        notificationJob = serviceScope.launch {
            while (true) {
                delay(60_000)
                
                if (!hasLocationPermission()) {
                    triggerInterruptionNotification()
                    stopSelf()
                    break
                }
                
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NOTIFICATION_ID, foregroundNotification())
            }
        }
        
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        serviceScope.launch {
            while (tracking) {
                if (!hasLocationPermission()) {
                    triggerInterruptionNotification()
                    stopSelf()
                    break
                }
                
                if (isWithinTrackingWindow()) {
                    try {
                        // COMPLIANCE ADDED: 10-minute periodic fetch instead of continuous updates
                        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                            .addOnSuccessListener { location ->
                                if (location != null) {
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
                    } catch (e: Exception) {
                        // Ignore exceptions on fetch
                    }
                }
                
                delay(TEN_MINUTES_MS)
            }
        }
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
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Field Activity Active")
            .setContentText("Your market presence is being logged for TrackIQ.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
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

    // Time window logic implemented below in isWithinTrackingWindow

    private fun triggerInterruptionNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Tracking Interrupted")
            .setContentText("Please re-enable location to complete your business visit log.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID + 1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        tracking = false
        notificationJob?.cancel()
        serviceScope.cancel()
    }

    override fun onBind(intent: android.content.Intent?): IBinder? = null

    private fun isWithinTrackingWindow(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        // Track from 06:00 (inclusive) until 24:00 (exclusive).
        return hour >= 6
    }

    /**
     * Verifies if the user is at the specific property within a 50-meter geofence.
     * Uses Android's Location.distanceBetween to accurately calculate distance.
     */
    fun isUserAtProperty(
        targetLat: Double, 
        targetLong: Double, 
        currentLat: Double, 
        currentLong: Double
    ): Boolean {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            currentLat, currentLong,
            targetLat, targetLong,
            results
        )
        return results[0] <= 50.0f
    }

    companion object {
        private const val TEN_MINUTES_MS = 10 * 60 * 1000L
        private const val CHANNEL_ID = "location_tracking"
        private const val NOTIFICATION_ID = 77
    }
}
