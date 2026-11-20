package com.example.myapplication

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class BleManager(private val context: Context) {

    companion object {
        private const val TAG = "MyScaleBLE"
        val SERVICE_FFE0 = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB")
        val CHAR_FFE1 = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")
        val CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
    }

    data class ScaleReading(
        val weightKg: Float,
        val impedance: Int? = null,
        val rawData: ByteArray? = null,
        val isStable: Boolean = true
    )

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private var scanner: BluetoothLeScanner? = null
    private var gatt: BluetoothGatt? = null

    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = _devices

    private val _connectionState = MutableStateFlow("Desconectado")
    val connectionState: StateFlow<String> = _connectionState

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs

    private val _lastReading = MutableStateFlow<ScaleReading?>(null)
    val lastReading: StateFlow<ScaleReading?> = _lastReading

    var onStableWeight: ((ScaleReading) -> Unit)? = null

    private var lastWeight: Float = 0f
    private var stableCount = 0

    private fun log(msg: String) {
        Log.d(TAG, msg)
        _logs.value = (_logs.value + msg).takeLast(200)
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (adapter == null || !adapter.isEnabled) {
            log("Bluetooth desligado")
            return
        }
        scanner = adapter.bluetoothLeScanner
        _devices.value = emptyList()
        log("Iniciando scan...")
        scanner?.startScan(
            null,
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
            scanCallback
        )
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanner?.stopScan(scanCallback)
        log("Scan parado")
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: result.scanRecord?.deviceName ?: return
            val isScale = listOf("bw-sc", "myscale", "bewell", "bwsc", "scale")
                .any { name.contains(it, ignoreCase = true) }
            if (isScale || name.startsWith("BW-", ignoreCase = true)) {
                val list = _devices.value.toMutableList()
                if (list.none { it.address == device.address }) {
                    list.add(device)
                    _devices.value = list
                    log("Encontrado: $name (${device.address})")
                }
            }
        }
        override fun onScanFailed(errorCode: Int) = log("Erro no scan: $errorCode")
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        stopScan()
        log("Conectando a ${device.name ?: device.address}")
        _connectionState.value = "Conectando..."
        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        _connectionState.value = "Desconectado"
        log("Desconectado")
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = "Conectado"
                    log("Conectado — a descobrir serviços")
                    g.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = "Desconectado"
                    log("Desconectado")
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                log("Erro ao descobrir serviços: $status")
                return
            }
            log("=== Serviços descobertos ===")
            g.services.forEach { service ->
                log("Serviço: ${service.uuid}")
                service.characteristics.forEach { char ->
                    val props = buildList {
                        if (char.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) add("R")
                        if (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) add("W")
                        if (char.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) add("N")
                        if (char.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) add("I")
                    }.joinToString()
                    log("  Char: ${char.uuid} [$props]")
                }
            }
            val characteristic = g.getService(SERVICE_FFE0)?.getCharacteristic(CHAR_FFE1)
            if (characteristic != null) {
                enableNotify(g, characteristic)
                log("Notificações ativadas em FFE1")
            } else {
                log("Característica FFE1 não encontrada!")
            }
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray) {
            val hex = value.joinToString("-") { "%02X".format(it) }
            log("NOTIFY: $hex")
            parseWeight(value)
        }

        @Deprecated("Deprecated")
        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
            c.value?.let { onCharacteristicChanged(g, c, it) }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotify(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        g.setCharacteristicNotification(characteristic, true)
        characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)?.let {
            it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            g.writeDescriptor(it)
        }
    }

    private fun parseWeight(data: ByteArray) {
        if (data.size < 7 || data[0] != 0x08.toByte()) return
        val raw = ((data[5].toInt() and 0xFF) shl 8) or (data[6].toInt() and 0xFF)
        val weightKg = raw / 200f
        if (weightKg < 5f || weightKg > 250f) return

        log("Peso detetado: %.2f kg".format(weightKg))
        val reading = ScaleReading(weightKg = weightKg, rawData = data)
        _lastReading.value = reading

        if (kotlin.math.abs(weightKg - lastWeight) < 0.15f) {
            stableCount++
            if (stableCount >= 3) {
                log("Peso estável: %.2f kg".format(weightKg))
                onStableWeight?.invoke(reading)
                stableCount = 0
            }
        } else {
            stableCount = 0
        }
        lastWeight = weightKg
    }
}
