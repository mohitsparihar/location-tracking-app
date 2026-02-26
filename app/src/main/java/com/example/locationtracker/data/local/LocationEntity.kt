package com.example.locationtracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?,
    val altitude: Double?,
    val altitudeAccuracy: Float?,
    val heading: Float?,
    val speed: Float?,
    val timestamp: Long,
    val isBackground: Boolean,
    val uploaded: Boolean
)
