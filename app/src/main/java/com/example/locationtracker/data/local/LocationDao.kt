package com.example.locationtracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Insert
    suspend fun insert(entity: LocationEntity): Long

    @Query("SELECT * FROM locations ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE uploaded = 0 ORDER BY timestamp ASC")
    suspend fun getPendingUploads(): List<LocationEntity>

    @Query("UPDATE locations SET uploaded = 1 WHERE id = :id")
    suspend fun markUploaded(id: Long)

    @Query("DELETE FROM locations")
    suspend fun deleteAll()
}
