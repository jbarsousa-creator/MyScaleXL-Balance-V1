package com.example.myapplication

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Percentage
import java.time.Instant
import java.time.ZoneOffset

class HealthConnectManager(private val context: Context) {

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    val permissions = setOf(
        HealthPermission.getWritePermission(WeightRecord::class),
        HealthPermission.getWritePermission(BodyFatRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class)
    )

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    suspend fun hasAllPermissions(): Boolean {
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    suspend fun writeMeasurement(weightKg: Float, bodyFatPercent: Float? = null) {
        val time = Instant.now()
        val offset = ZoneOffset.systemDefault().rules.getOffset(time)

        val records = mutableListOf<Record>(
            WeightRecord(
                weight = Mass.kilograms(weightKg.toDouble()),
                time = time,
                zoneOffset = offset,
                metadata = Metadata(recordingMethod = Metadata.RECORDING_METHOD_MANUAL_ENTRY)
            )
        )
        bodyFatPercent?.let {
            records.add(
                BodyFatRecord(
                    percentage = Percentage(it.toDouble()),
                    time = time,
                    zoneOffset = offset,
                    metadata = Metadata(recordingMethod = Metadata.RECORDING_METHOD_MANUAL_ENTRY)
                )
            )
        }
        client.insertRecords(records)
    }
}
