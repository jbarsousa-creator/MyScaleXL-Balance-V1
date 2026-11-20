package com.example.myapplication.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.myapplication.AnalysisState
import com.example.myapplication.MainViewModel
import com.example.myapplication.R
import com.example.myapplication.data.FoodEntry
import com.example.myapplication.util.DateUtils
import com.example.myapplication.util.FoodAnalyzer
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val MEAL_TYPES = listOf("breakfast", "lunch", "snack", "dinner")

private fun mealLabelRes(type: String): Int = when (type) {
    "breakfast" -> R.string.meal_breakfast
    "lunch" -> R.string.meal_lunch
    "snack" -> R.string.meal_snack
    else -> R.string.meal_dinner
}

private fun defaultMealType(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 10 -> "breakfast"
        hour < 15 -> "lunch"
        hour < 18 -> "snack"
        else -> "dinner"
    }
}

@Composable
fun MealsScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val user by vm.activeUser.collectAsState()
    val goal by vm.dailyGoal.collectAsState()
    val day by vm.selectedDay.collectAsState()
    val foods by vm.foodsForSelectedDay.collectAsState()
    val week by vm.foodsLast7Days.collectAsState()
    val analysis by vm.analysis.collectAsState()

    var mealType by rememberSaveable { mutableStateOf(defaultMealType()) }
    var pendingUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var showManual by remember { mutableStateOf(false) }
    var showGoal by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<FoodEntry?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingUri
        if (ok && uri != null) vm.analyzePhoto(uri)
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.analyzePhoto(uri)
    }

    val today = DateUtils.startOfDay(System.currentTimeMillis())
    val consumed = foods.sumOf { it.calories }

    if (user == null) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.home_no_profile), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Navegação por dia
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.changeDay(-1) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(R.string.meals_prev_day))
            }
            Text(
                dayLabel(day, today),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { vm.changeDay(1) }, enabled = day < today) {
                Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.meals_next_day))
            }
        }

        // Resumo de calorias do dia (consumidas vs meta)
        CalorieSummaryForDay(goal, consumed)
        TextButton(onClick = { showGoal = true }) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.edit_goal_title))
        }

        // Adicionar alimento
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.meals_add_title), fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MEAL_TYPES.forEach { type ->
                        FilterChip(
                            selected = mealType == type,
                            onClick = { mealType = type },
                            label = { Text(stringResource(mealLabelRes(type))) }
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val dir = File(context.cacheDir, "camera")
                            dir.mkdirs()
                            val file = File(dir, "meal_" + System.currentTimeMillis() + ".jpg")
                            val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                            pendingUri = uri
                            cameraLauncher.launch(uri)
                        },
                        modifier = Modifier.weight(1.4f)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_photo), maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    }
                    OutlinedButton(onClick = { showManual = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                    }
                }
                Text(
                    stringResource(R.string.photo_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Refeições do dia
        MEAL_TYPES.forEach { type ->
            MealSection(
                title = stringResource(mealLabelRes(type)),
                entries = foods.filter { it.mealType == type },
                onEdit = { editing = it }
            )
        }

        // Últimos 7 dias
        WeekCard(week, goal, today)
    }

    // ----- Diálogos -----
    when (val a = analysis) {
        AnalysisState.Idle -> {}
        AnalysisState.Loading -> {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(28.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(stringResource(R.string.analyzing))
                    }
                }
            )
        }
        is AnalysisState.Result -> {
            ReviewDialog(
                items = a.items,
                mealType = mealType,
                onSave = { items -> vm.saveFoods(mealType, items, a.jpeg) },
                onDismiss = { vm.dismissAnalysis() }
            )
        }
        is AnalysisState.Error -> {
            val message = when (a.kind) {
                AnalysisState.Kind.NO_KEY -> stringResource(R.string.err_no_key)
                AnalysisState.Kind.NETWORK -> stringResource(R.string.err_network, a.detail)
                AnalysisState.Kind.PARSE -> stringResource(R.string.err_parse)
                AnalysisState.Kind.NO_FOOD -> stringResource(R.string.err_no_food)
            }
            AlertDialog(
                onDismissRequest = { vm.dismissAnalysis() },
                confirmButton = { TextButton(onClick = { vm.dismissAnalysis() }) { Text(stringResource(R.string.ok)) } },
                text = { Text(message) }
            )
        }
    }

    val toEdit = editing
    if (toEdit != null) {
        EditFoodDialog(
            entry = toEdit,
            onDismiss = { editing = null },
            onSave = { name, kcal, type ->
                vm.updateFood(toEdit, name, kcal, type)
                editing = null
            },
            onDelete = {
                vm.deleteFood(toEdit)
                editing = null
            }
        )
    }

    if (showManual) {
        ManualFoodDialog(
            onDismiss = { showManual = false },
            onConfirm = { name, kcal ->
                vm.addManualFood(mealType, name, kcal)
                showManual = false
            }
        )
    }

    if (showGoal) {
        GoalDialog(
            current = vm.manualGoal,
            onDismiss = { showGoal = false },
            onConfirm = { kcal ->
                vm.setDailyGoal(kcal)
                showGoal = false
            }
        )
    }
}

@Composable
private fun CalorieSummaryForDay(goal: Int, consumed: Int) {
    if (goal > 0) {
        CalorieSummary(goal, consumed)
    }
}

@Composable
private fun dayLabel(day: Long, today: Long): String {
    return when (day) {
        today -> stringResource(R.string.meals_today)
        DateUtils.addDays(today, -1) -> stringResource(R.string.meals_yesterday)
        else -> SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(day))
            .replaceFirstChar { it.uppercase() }
    }
}

@Composable
private fun MealSection(title: String, entries: List<FoodEntry>, onEdit: (FoodEntry) -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.kcal_value, entries.sumOf { it.calories }),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (entries.isEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.no_foods),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            entries.forEach { e ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (e.photoPath != null) {
                        FoodThumb(e.photoPath, Modifier.size(40.dp))
                        Spacer(Modifier.width(10.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(e.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(R.string.kcal_value, e.calories),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onEdit(e) }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekCard(entries: List<FoodEntry>, goal: Int, today: Long) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.week_title), fontWeight = FontWeight.Bold)
            for (i in 6 downTo 0) {
                val start = DateUtils.addDays(today, -i)
                val end = DateUtils.addDays(start, 1)
                val total = entries.filter { it.timestamp >= start && it.timestamp < end }.sumOf { it.calories }
                val fraction = if (goal > 0) (total.toFloat() / goal).coerceIn(0f, 1f) else 0f
                val over = goal > 0 && total > goal
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        SimpleDateFormat("EEE d", Locale.getDefault()).format(Date(start)),
                        modifier = Modifier.width(64.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                    LinearProgressIndicator(
                        progress = fraction,
                        modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = if (over) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        stringResource(R.string.kcal_value, total),
                        modifier = Modifier.width(84.dp),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

private class EditRow(val name: String, val kcal: String, val protein: Float?, val carbs: Float?, val fat: Float?)

@Composable
private fun ReviewDialog(
    items: List<FoodAnalyzer.FoodItem>,
    mealType: String,
    onSave: (List<FoodAnalyzer.FoodItem>) -> Unit,
    onDismiss: () -> Unit
) {
    val rows = remember {
        mutableStateListOf<EditRow>().apply {
            addAll(items.map { EditRow(it.name, it.calories.toString(), it.proteinG, it.carbsG, it.fatG) })
        }
    }
    val total = rows.sumOf { it.kcal.toIntOrNull() ?: 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.review_title) + " • " + stringResource(mealLabelRes(mealType))) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.review_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                rows.forEachIndexed { index, row ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = row.name,
                            onValueChange = { rows[index] = EditRow(it, row.kcal, row.protein, row.carbs, row.fat) },
                            label = { Text(stringResource(R.string.food_name)) },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(6.dp))
                        OutlinedTextField(
                            value = row.kcal,
                            onValueChange = { v ->
                                rows[index] = EditRow(row.name, v.filter { c -> c.isDigit() }.take(5), row.protein, row.carbs, row.fat)
                            },
                            label = { Text(stringResource(R.string.calories_label)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(84.dp)
                        )
                        IconButton(onClick = { rows.removeAt(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Text(stringResource(R.string.total_fmt, total), fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        rows.filter { it.name.isNotBlank() }.map {
                            FoodAnalyzer.FoodItem(it.name.trim(), it.kcal.toIntOrNull() ?: 0, it.protein, it.carbs, it.fat)
                        }
                    )
                },
                enabled = rows.isNotEmpty()
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun EditFoodDialog(
    entry: FoodEntry,
    onDismiss: () -> Unit,
    onSave: (String, Int, String) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(entry.name) }
    var kcal by remember { mutableStateOf(entry.calories.toString()) }
    var type by remember { mutableStateOf(entry.mealType) }
    var confirmDelete by remember { mutableStateOf(false) }
    val value = kcal.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_food_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (entry.photoPath != null) {
                    FoodThumb(entry.photoPath, Modifier.fillMaxWidth().height(140.dp))
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.food_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = kcal,
                    onValueChange = { v -> kcal = v.filter { c -> c.isDigit() }.take(5) },
                    label = { Text(stringResource(R.string.calories_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MEAL_TYPES.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(stringResource(mealLabelRes(t))) }
                        )
                    }
                }
                TextButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (value != null) onSave(name.trim(), value, type) },
                enabled = name.isNotBlank() && value != null
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_body, entry.name)) },
            confirmButton = {
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun ManualFoodDialog(onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    val value = kcal.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manual_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.food_name)) }, singleLine = true)
                OutlinedTextField(
                    kcal,
                    { v -> kcal = v.filter { c -> c.isDigit() }.take(5) },
                    label = { Text(stringResource(R.string.calories_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (value != null) onConfirm(name.trim(), value) },
                enabled = name.isNotBlank() && value != null
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun GoalDialog(current: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by remember { mutableStateOf(if (current > 0) current.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_goal_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    text,
                    { v -> text = v.filter { c -> c.isDigit() }.take(5) },
                    label = { Text(stringResource(R.string.goal_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Text(
                    stringResource(R.string.goal_auto_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(text.toIntOrNull() ?: 0) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
