package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.myapplication.util.LocaleHelper
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.*

class MainActivity : ComponentActivity() {

    private lateinit var bleManager: BleManager
    private lateinit var healthConnectManager: HealthConnectManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    private val healthPermissionLauncher = registerForActivityResult(
        androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()
    ) { }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bleManager = BleManager(this)
        healthConnectManager = HealthConnectManager(this)

        requestBluetoothPermissions()
        requestHealthConnectPermissions()

        setContent {
            val navController = rememberNavController()
            val vm: MainViewModel = viewModel()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: "home"
            val activeUser by vm.activeUser.collectAsState()

            LaunchedEffect(Unit) {
                bleManager.onStableWeight = { reading ->
                    vm.saveMeasurement(reading.weightKg, reading.impedance)
                }
            }

            MyScaleTheme {
                Scaffold(
                    topBar = {
                        AppHeader(
                            "MyScale XL",
                            activeUser?.let { stringResource(R.string.header_profile, it.name) },
                            onSettingsClick = { navController.navigate("settings") { launchSingleTop = true } }
                        )
                    },
                    bottomBar = {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                            NavigationBarItem(
                                selected = currentRoute == "home",
                                onClick = { navController.navigate("home") { launchSingleTop = true } },
                                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                label = { Text(stringResource(R.string.nav_home), maxLines = 1, fontSize = 10.sp) },
                                colors = navItemColors()
                            )
                            NavigationBarItem(
                                selected = currentRoute == "meals",
                                onClick = { navController.navigate("meals") { launchSingleTop = true } },
                                icon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
                                label = { Text(stringResource(R.string.nav_meals), maxLines = 1, fontSize = 10.sp) },
                                colors = navItemColors()
                            )
                            NavigationBarItem(
                                selected = currentRoute == "history",
                                onClick = { navController.navigate("history") { launchSingleTop = true } },
                                icon = { Icon(Icons.Default.History, contentDescription = null) },
                                label = { Text(stringResource(R.string.nav_history), maxLines = 1, fontSize = 10.sp) },
                                colors = navItemColors()
                            )
                            NavigationBarItem(
                                selected = currentRoute == "profiles",
                                onClick = { navController.navigate("profiles") { launchSingleTop = true } },
                                icon = { Icon(Icons.Default.Person, contentDescription = null) },
                                label = { Text(stringResource(R.string.nav_profiles), maxLines = 1, fontSize = 10.sp) },
                                colors = navItemColors()
                            )
                            NavigationBarItem(
                                selected = currentRoute == "ble",
                                onClick = { navController.navigate("ble") { launchSingleTop = true } },
                                icon = { Icon(Icons.Default.Bluetooth, contentDescription = null) },
                                label = { Text(stringResource(R.string.nav_connect), maxLines = 1, fontSize = 10.sp) },
                                colors = navItemColors()
                            )
                        }
                    }
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(padding)
                    ) {
                        composable("home") { HomeScreen(vm) }
                        composable("meals") { MealsScreen(vm) }
                        composable("history") { HistoryScreen(vm) }
                        composable("profiles") { ProfilesScreen(vm) }
                        composable("ble") { BleScreen(bleManager) }
                        composable("settings") { SettingsScreen() }
                    }
                }
            }
        }
    }

    @Composable
    private fun navItemColors() = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
        selectedTextColor = MaterialTheme.colorScheme.onSurface,
        indicatorColor = MaterialTheme.colorScheme.primary,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    private fun requestBluetoothPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun requestHealthConnectPermissions() {
        if (healthConnectManager.isAvailable()) {
            healthPermissionLauncher.launch(healthConnectManager.permissions)
        }
    }

    override fun onDestroy() {
        bleManager.disconnect()
        super.onDestroy()
    }
}
