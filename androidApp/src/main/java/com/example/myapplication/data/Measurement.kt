package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "measurements",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Measurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val weightKg: Float,
    val bmi: Float? = null,
    val bodyFatPercent: Float? = null,
    val muscleMassKg: Float? = null,
    val boneMassKg: Float? = null,
    val waterPercent: Float? = null,
    val visceralFat: Float? = null,
    val bmr: Int? = null,
    val impedance: Int? = null,
    val notes: String? = null
)
