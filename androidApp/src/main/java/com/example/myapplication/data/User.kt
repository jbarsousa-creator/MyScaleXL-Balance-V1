package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val gender: String,
    val birthYear: Int,
    val heightCm: Float,
    val targetWeight: Float? = null,
    val isActive: Boolean = false
)
