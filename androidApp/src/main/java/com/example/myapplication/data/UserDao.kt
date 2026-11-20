package com.example.myapplication.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY name")
    fun getAll(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE isActive = 1 LIMIT 1")
    fun getActive(): Flow<User?>

    @Insert suspend fun insert(user: User): Long
    @Update suspend fun update(user: User)
    @Query("UPDATE users SET isActive = 0") suspend fun clearActive()
    @Query("UPDATE users SET isActive = 1 WHERE id = :id") suspend fun setActive(id: Long)
    @Delete suspend fun delete(user: User)
}
