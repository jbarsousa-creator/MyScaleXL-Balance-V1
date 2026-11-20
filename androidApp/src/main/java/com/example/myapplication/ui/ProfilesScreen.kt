package com.example.myapplication.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.myapplication.MainViewModel
import com.example.myapplication.R
import com.example.myapplication.data.User

@Composable
fun ProfilesScreen(vm: MainViewModel) {
    val users by vm.users.collectAsState()
    val active by vm.activeUser.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.profiles_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            FilledTonalButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.profiles_new))
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(users) { user ->
                UserItem(user, user.id == active?.id, onSelect = { vm.setActiveUser(user.id) }, onDelete = { vm.deleteUser(user) })
            }
        }
    }
    if (showDialog) {
        AddUserDialog(onDismiss = { showDialog = false }, onConfirm = { name, gender, year, height, target ->
            vm.addUser(name, gender, year, height, target); showDialog = false
        })
    }
}

@Composable
fun UserItem(user: User, isActive: Boolean, onSelect: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onSelect() },
        colors = CardDefaults.cardColors(containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(48.dp).clip(CircleShape), color = MaterialTheme.colorScheme.primary) {
                Box(contentAlignment = Alignment.Center) {
                    Text(user.name.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.profile_line, user.gender, user.heightCm.toInt(), user.targetWeight?.let { "%.1f kg".format(it) } ?: "—"), style = MaterialTheme.typography.bodySmall)
            }
            if (isActive) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
fun AddUserDialog(onDismiss: () -> Unit, onConfirm: (String, String, Int, Float, Float?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("M") }
    var birthYear by remember { mutableStateOf("1990") }
    var height by remember { mutableStateOf("170") }
    var target by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_profile)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.name_label)) }, singleLine = true)
                Row {
                    FilterChip(selected = gender == "M", onClick = { gender = "M" }, label = { Text(stringResource(R.string.gender_male)) })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = gender == "F", onClick = { gender = "F" }, label = { Text(stringResource(R.string.gender_female)) })
                }
                OutlinedTextField(birthYear, { birthYear = it }, label = { Text(stringResource(R.string.birth_year)) }, singleLine = true)
                OutlinedTextField(height, { height = it }, label = { Text(stringResource(R.string.height_cm)) }, singleLine = true)
                OutlinedTextField(target, { target = it }, label = { Text(stringResource(R.string.target_weight_opt)) }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) onConfirm(name, gender, birthYear.toIntOrNull() ?: 1990, height.toFloatOrNull() ?: 170f, target.toFloatOrNull())
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
