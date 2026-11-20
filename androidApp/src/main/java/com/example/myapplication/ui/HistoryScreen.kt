package com.example.myapplication.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.myapplication.MainViewModel
import com.example.myapplication.R
import com.example.myapplication.data.Measurement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun Float?.csv(): String = if (this == null) "" else "%.2f".format(Locale.US, this)

@Composable
fun HistoryScreen(vm: MainViewModel) {
    val measurements by vm.measurements.collectAsState()
    val user by vm.activeUser.collectAsState()
    val context = LocalContext.current
    var rangeDays by remember { mutableStateOf(30) }
    val subject = stringResource(R.string.export_subject)
    val chooserTitle = stringResource(R.string.export_chooser)

    val cutoff = if (rangeDays == 0) 0L else System.currentTimeMillis() - rangeDays * 86_400_000L
    val filtered = measurements.filter { it.timestamp >= cutoff }
    val ascending = filtered.reversed()

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
          Column {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.history_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.history_count, measurements.size, user?.name ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    enabled = measurements.isNotEmpty(),
                    onClick = {
                        val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                        val csv = buildString {
                            append("date,weight_kg,bmi,body_fat_pct,muscle_kg,bone_kg,water_pct,visceral_fat,bmr\n")
                            measurements.sortedBy { it.timestamp }.forEach { m ->
                                append(dateFmt.format(Date(m.timestamp))).append(',')
                                append(m.weightKg.csv()).append(',')
                                append(m.bmi.csv()).append(',')
                                append(m.bodyFatPercent.csv()).append(',')
                                append(m.muscleMassKg.csv()).append(',')
                                append(m.boneMassKg.csv()).append(',')
                                append(m.waterPercent.csv()).append(',')
                                append(m.visceralFat.csv()).append(',')
                                append(m.bmr?.toString() ?: "").append('\n')
                            }
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, subject)
                            putExtra(Intent.EXTRA_TEXT, csv)
                        }
                        context.startActivity(Intent.createChooser(intent, chooserTitle))
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.export))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = rangeDays == 7, onClick = { rangeDays = 7 }, label = { Text(stringResource(R.string.range_7)) })
                FilterChip(selected = rangeDays == 30, onClick = { rangeDays = 30 }, label = { Text(stringResource(R.string.range_30)) })
                FilterChip(selected = rangeDays == 0, onClick = { rangeDays = 0 }, label = { Text(stringResource(R.string.range_all)) })
            }
          }
        }

        item {
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.history_empty))
                }
            } else {
                val minW = filtered.minOf { it.weightKg }
                val maxW = filtered.maxOf { it.weightKg }
                val avgW = filtered.map { it.weightKg }.average().toFloat()
                val change = if (ascending.size >= 2) ascending.last().weightKg - ascending.first().weightKg else 0f

                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Stat(stringResource(R.string.stat_min), "%.1f".format(minW))
                            Stat(stringResource(R.string.stat_avg), "%.1f".format(avgW))
                            Stat(stringResource(R.string.stat_max), "%.1f".format(maxW))
                            Stat(stringResource(R.string.stat_change), "%+.1f".format(change))
                        }
                        if (ascending.size >= 2) {
                            Spacer(Modifier.height(12.dp))
                            WeightChart(ascending.map { it.weightKg }, Modifier.fillMaxWidth().height(160.dp))
                        }
                    }
                }
            }
        }

        items(filtered, key = { it.id }) { m ->
            MeasurementItem(m) { vm.deleteMeasurement(m) }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MeasurementItem(measurement: Measurement, onDelete: () -> Unit) {
    val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(measurement.timestamp))
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(measurement.timestamp))
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "%.1f kg".format(measurement.weightKg),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(stringResource(R.string.item_date_time, date, time), style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    measurement.bmi?.let { Text(stringResource(R.string.item_bmi, "%.1f".format(it)), style = MaterialTheme.typography.bodySmall) }
                    measurement.bodyFatPercent?.let { Text(stringResource(R.string.item_fat, "%.1f%%".format(it)), style = MaterialTheme.typography.bodySmall) }
                    measurement.bmr?.let { Text(stringResource(R.string.item_bmr, it), style = MaterialTheme.typography.bodySmall) }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
