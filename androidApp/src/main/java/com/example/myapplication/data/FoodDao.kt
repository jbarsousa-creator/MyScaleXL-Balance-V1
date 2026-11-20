package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_entries WHERE userId = :userId AND timestamp >= :from AND timestamp < :to ORDER BY timestamp ASC")
    fun getBetween(userId: Long, from: Long, to: Long): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entries WHERE userId = :userId AND timestamp >= :from ORDER BY timestamp DESC")
    fun getSince(userId: Long, from: Long): Flow<List<FoodEntry>>

    @Insert
    suspend fun insertAll(entries: List<FoodEntry>)

    @Update
    suspend fun update(entry: FoodEntry)

    @Delete
    suspend fun delete(entry: FoodEntry)
}
