package com.example.myapplication.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.myapplication.BleManager
import com.example.myapplication.R

@SuppressLint("MissingPermission")
@Composable
fun BleScreen(bleManager: BleManager) {
    val devices by bleManager.devices.collectAsState()
    val connectionState by bleManager.connectionState.collectAsState()
    val logs by bleManager.logs.collectAsState()
    val lastReading by bleManager.lastReading.collectAsState()
    var isScanning by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.ble_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
            containerColor = when (connectionState) {
                "Conectado" -> MaterialTheme.colorScheme.primaryContainer
                "Conectando..." -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(when (connectionState) {
                    "Conectado" -> Icons.Default.BluetoothConnected
                    "Conectando..." -> Icons.Default.BluetoothSearching
                    else -> Icons.Default.BluetoothDisabled
                }, null)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        stringResource(
                            when (connectionState) {
                                "Conectado" -> R.string.ble_connected
                                "Conectando..." -> R.string.ble_connecting
                                else -> R.string.ble_disconnected
                            }
                        ),
                        fontWeight = FontWeight.Bold
                    )
                    lastReading?.let { Text(stringResource(R.string.ble_last_weight, it.weightKg)) }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                if (isScanning) { bleManager.stopScan(); isScanning = false }
                else { bleManager.startScan(); isScanning = true }
            }, modifier = Modifier.weight(1f)) {
                Icon(if (isScanning) Icons.Default.Stop else Icons.Default.Search, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (isScanning) R.string.ble_stop else R.string.ble_scan))
            }
            OutlinedButton(onClick = { bleManager.disconnect() }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Close, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.ble_disconnect))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.ble_devices), style = MaterialTheme.typography.titleMedium)
        LazyColumn(Modifier.weight(0.4f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(devices) { device ->
                DeviceItem(device) { bleManager.connect(device); isScanning = false }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.ble_logs), style = MaterialTheme.typography.titleMedium)
        LazyColumn(Modifier.weight(0.5f).fillMaxWidth()) {
            items(logs.reversed()) { log ->
                Text(log, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(vertical = 1.dp))
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DeviceItem(device: BluetoothDevice, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Bluetooth, null)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(device.name ?: stringResource(R.string.ble_no_name), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(device.address, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
