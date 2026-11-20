package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.MainViewModel
import com.example.myapplication.R
import com.example.myapplication.data.Measurement
import com.example.myapplication.data.User
import com.example.myapplication.util.BodyMetrics
import com.example.myapplication.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun HomeScreen(vm: MainViewModel) {
    val user by vm.activeUser.collectAsState()
    val latest by vm.latestMeasurement.collectAsState()
    val measurements by vm.measurements.collectAsState()
    val foods by vm.foodsLast7Days.collectAsState()
    val goal by vm.dailyGoal.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    val u = user
    val l = latest

    val todayStart = DateUtils.startOfDay(System.currentTimeMillis())
    val consumedToday = foods.filter { it.timestamp >= todayStart }.sumOf { it.calories }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
                .replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        WeightHero(l, measurements.getOrNull(1))

        Button(
            onClick = { showAdd = true },
            enabled = u != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.home_add_weight))
        }

        if (u != null && goal > 0) {
            CalorieSummary(goal, consumedToday)
        }

        if (u == null) {
            Text(
                stringResource(R.string.home_no_profile),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (l != null) {
            BodyDetails(u, l, measurements)
        } else {
            Text(
                stringResource(R.string.home_no_weights),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showAdd) {
        AddWeightDialog(
            onDismiss = { showAdd = false },
            onConfirm = { kg ->
                vm.saveMeasurement(kg)
                showAdd = false
            }
        )
    }
}

@Composable
private fun WeightHero(latest: Measurement?, previous: Measurement?) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.home_last_weight), color = onPrimary.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    latest?.let { "%.1f".format(it.weightKg) } ?: "--",
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Bold,
                    color = onPrimary
                )
                Text(
                    " kg",
                    color = onPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            if (latest != null && previous != null) {
                val diff = latest.weightKg - previous.weightKg
                val icon = when {
                    diff > 0.05f -> Icons.Default.ArrowUpward
                    diff < -0.05f -> Icons.Default.ArrowDownward
                    else -> Icons.Default.Remove
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = onPrimary)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.home_diff_prev, diff), color = onPrimary)
                }
            }
            if (latest != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(latest.timestamp)),
                    color = onPrimary.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private data class TileData(val label: String, val value: String, val hint: String, val color: Color, val content: Color)

@Composable
private fun BodyDetails(u: User, l: Measurement, measurements: List<Measurement>) {
    val age = Calendar.getInstance().get(Calendar.YEAR) - u.birthYear
    val isMale = u.gender == "M"
    val w = l.weightKg
    val bmi = l.bmi ?: BodyMetrics.calculateBmi(w, u.heightCm)
    val fat = l.bodyFatPercent ?: BodyMetrics.estimateBodyFat(bmi, age, isMale)
    val boneKg = l.boneMassKg ?: BodyMetrics.estimateBoneKg(w, isMale)
    val muscleKg = l.muscleMassKg ?: BodyMetrics.estimateMuscleKg(w, fat, boneKg)
    val water = l.waterPercent ?: BodyMetrics.estimateWaterPercent(fat)
    val visceral = l.visceralFat ?: BodyMetrics.estimateVisceralFat(bmi, age, isMale)
    val bmr = l.bmr ?: BodyMetrics.estimateBmr(w, u.heightCm, age, isMale)
    val ideal = BodyMetrics.idealWeightRange(u.heightCm)
    val range = gaugeRange(w)
    val bmiCategory = stringResource(BodyMetrics.bmiCategory(bmi))

    val musclePct = muscleKg / w * 100f
    val bonePct = boneKg / w * 100f
    val bmiLevel = HealthLevels.bmi(bmi)
    val fatLevel = HealthLevels.bodyFat(fat, isMale)
    val visceralLevel = HealthLevels.visceral(visceral)
    val muscleLevel = HealthLevels.muscle(musclePct, isMale)
    val boneLevel = HealthLevels.bone(bonePct)
    val waterLevel = HealthLevels.water(water, isMale)
    val waterMin = if (isMale) 50f else 45f
    // métricas de referência (sem estado): cor neutra
    val neutral = MaterialTheme.colorScheme.surfaceVariant
    val onNeutral = MaterialTheme.colorScheme.onSurface

    // Mostrador
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            WeightGauge(w, Modifier.fillMaxWidth().height(150.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("%.0f".format(range.first), style = MaterialTheme.typography.bodySmall)
                Text("%.1f kg".format(w), fontWeight = FontWeight.Bold)
                Text("%.0f".format(range.second), style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    // IMC
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.bmi_label), fontWeight = FontWeight.Bold)
                Text("%.1f • %s".format(bmi, bmiCategory), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            BmiScale(bmi)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.ideal_weight_range, ideal.first, ideal.second),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Quadrados de métricas
    val tiles = listOf(
        TileData(stringResource(R.string.bmi_label), "%.1f".format(bmi), bmiCategory, bmiLevel.color, bmiLevel.onColor),
        TileData(stringResource(R.string.tile_fat), "%.1f%%".format(fat), stringResource(BodyMetrics.fatCategory(fat, isMale)), fatLevel.color, fatLevel.onColor),
        TileData(stringResource(R.string.tile_visceral), "%.0f".format(visceral), stringResource(BodyMetrics.visceralCategory(visceral)), visceralLevel.color, visceralLevel.onColor),
        TileData(stringResource(R.string.tile_muscle), "%.1f kg".format(muscleKg), stringResource(R.string.pct_of_weight, "%.0f%%".format(musclePct)), muscleLevel.color, muscleLevel.onColor),
        TileData(stringResource(R.string.tile_bone), "%.1f kg".format(boneKg), stringResource(R.string.pct_of_weight, "%.1f%%".format(bonePct)), boneLevel.color, boneLevel.onColor),
        TileData(
            stringResource(R.string.tile_water), "%.0f%%".format(water),
            stringResource(if (water < waterMin) R.string.hint_below_ideal else R.string.hint_hydrated),
            waterLevel.color, waterLevel.onColor
        ),
        TileData(stringResource(R.string.tile_bmr), "$bmr", stringResource(R.string.unit_kcal_day), neutral, onNeutral),
        TileData(stringResource(R.string.tile_ideal), "%.0f–%.0f".format(ideal.first, ideal.second), "kg", neutral, onNeutral)
    )
    tiles.chunked(2).forEach { pair ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            pair.forEach { t ->
                MetricTile(t.label, t.value, t.hint, Modifier.weight(1f), t.color, t.content)
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        LevelLegend(Level.GOOD, stringResource(R.string.legend_good))
        LevelLegend(Level.WATCH, stringResource(R.string.legend_watch))
        LevelLegend(Level.HIGH, stringResource(R.string.legend_high))
        LevelLegend(Level.ALERT, stringResource(R.string.legend_alert))
    }

    // Composição corporal
    val fatKg = w * fat / 100f
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.composition_title), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompositionPie(fatKg, muscleKg, boneKg, w, Modifier.size(140.dp))
                Spacer(Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LegendItem(BwColors.Fat, stringResource(R.string.legend_fat), fatKg)
                    LegendItem(BwColors.Muscle, stringResource(R.string.legend_muscle), muscleKg)
                    LegendItem(BwColors.Bone, stringResource(R.string.legend_bone), boneKg)
                    LegendItem(BwColors.Other, stringResource(R.string.legend_other), (w - fatKg - muscleKg - boneKg).coerceAtLeast(0f))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.composition_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Objetivo
    val target = u.targetWeight
    if (target != null) {
        val start = measurements.lastOrNull()?.weightKg ?: w
        val total = abs(start - target)
        val moved = if (target < start) start - w else w - start
        val progress = if (total < 0.01f) 1f else (moved / total).coerceIn(0f, 1f)
        val remaining = w - target

        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.goal_title), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.goal_complete, "%.0f%%".format(progress * 100f)),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    GoalStat(stringResource(R.string.goal_start), "%.1f kg".format(start))
                    GoalStat(stringResource(R.string.goal_target), "%.1f kg".format(target))
                    GoalStat(
                        stringResource(if (remaining > 0f) R.string.goal_to_lose else R.string.goal_to_gain),
                        "%.1f kg".format(abs(remaining))
                    )
                }
            }
        }
    }

    // Evolução
    val recent = measurements.take(14).reversed()
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.evolution_title), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (recent.size >= 2) {
                WeightChart(recent.map { it.weightKg }, Modifier.fillMaxWidth().height(160.dp))
            } else {
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.evolution_empty))
                }
            }
        }
    }
}

@Composable
private fun LevelLegend(level: Level, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(level.color))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LegendItem(color: Color, label: String, kg: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.legend_item, label, kg), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun GoalStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AddWeightDialog(onDismiss: () -> Unit, onConfirm: (Float) -> Unit) {
    var text by remember { mutableStateOf("") }
    val value = text.replace(',', '.').toFloatOrNull()
    val valid = value != null && value >= 20f && value <= 300f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_weight_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.weight_kg_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        },
        confirmButton = {
            Button(
                onClick = { if (value != null && valid) onConfirm(value) },
                enabled = valid
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
