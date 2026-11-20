package com.example.myapplication.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurements WHERE userId = :userId ORDER BY timestamp DESC")
    fun getByUser(userId: Long): Flow<List<Measurement>>

    @Query("SELECT * FROM measurements WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    fun getLatest(userId: Long): Flow<Measurement?>

    @Insert suspend fun insert(measurement: Measurement): Long
    @Delete suspend fun delete(measurement: Measurement)
}
