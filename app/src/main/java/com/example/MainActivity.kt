package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Earbuds
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Earbuds
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DevicesScreen
import com.example.ui.screens.SoundScreen
import com.example.ui.screens.SystemScreen
import com.example.ui.theme.SoniqProTheme
import com.example.ui.viewmodel.SoniqUiEvent
import com.example.ui.viewmodel.SoniqViewModel
import kotlinx.coroutines.launch

object SoniqRoutes {
    const val STUDIO = "studio"
    const val SOUND = "sound"
    const val DEVICES = "devices"
    const val SYSTEM = "system"
}

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val soniqTabs = listOf(
    NavTab(SoniqRoutes.STUDIO, "Studio", Icons.Rounded.Earbuds, Icons.Outlined.Earbuds),
    NavTab(SoniqRoutes.SOUND, "Sound", Icons.Rounded.GraphicEq, Icons.Outlined.GraphicEq),
    NavTab(SoniqRoutes.DEVICES, "Devices", Icons.Rounded.Devices, Icons.Outlined.Devices),
    NavTab(SoniqRoutes.SYSTEM, "System", Icons.Rounded.Tune, Icons.Outlined.Tune)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as SoniqApplication
        setContent {
            val viewModel: SoniqViewModel = viewModel(
                factory = SoniqViewModel.Factory(app.repository)
            )
            SoniqAppRoot(
                viewModel = viewModel,
                onStartServices = { app.repository.startHardwareServices() },
                onStopServices = { app.repository.stopHardwareServices() }
            )
        }
    }
}

@Composable
fun SoniqAppRoot(
    viewModel: SoniqViewModel,
    onStartServices: () -> Unit,
    onStopServices: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: SoniqRoutes.STUDIO

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> onStartServices()
                Lifecycle.Event.ON_STOP -> onStopServices()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.onPermissionsChanged()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPermissionsChanged()
        if (granted) {
            viewModel.captureLastKnownLocation()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.onPermissionsChanged()
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                runCatching {
                    val json = viewModel.generateBackupJson()
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    }
                }.onSuccess {
                    snackbarHostState.showSnackbar("SONIQ PRO backup exported")
                }.onFailure { err ->
                    snackbarHostState.showSnackbar("Export failed: ${err.message}")
                }
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: ""
                }.onSuccess { raw ->
                    if (raw.isNotBlank()) viewModel.restoreBackupJson(raw)
                }.onFailure { err ->
                    snackbarHostState.showSnackbar("Could not read backup: ${err.message}")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SoniqUiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                SoniqUiEvent.OpenSystemBluetoothSettings -> {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    }
                }
            }
        }
    }

    val requestBluetoothPermissions = {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
        btPermissionLauncher.launch(perms)
    }

    val requestNotificationPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    SoniqProTheme(themePreference = state.preferences.themePreference) {
        val bgTop = MaterialTheme.colorScheme.background
        val bgGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(bgGlow, bgTop, bgTop))
                )
        ) {
            val useRail = maxWidth >= 720.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                        ) {
                            soniqTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    modifier = Modifier.testTag("nav_tab_${tab.route}"),
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        ) {
                            soniqTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    modifier = Modifier.testTag("nav_rail_${tab.route}"),
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    )
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        NavHost(
                            navController = navController,
                            startDestination = SoniqRoutes.STUDIO
                        ) {
                            composable(SoniqRoutes.STUDIO) {
                                DashboardScreen(
                                    state = state,
                                    onRequestBluetoothPermissions = requestBluetoothPermissions,
                                    onNavigateToDevices = { navigateToTab(SoniqRoutes.DEVICES) },
                                    onNavigateToSound = { navigateToTab(SoniqRoutes.SOUND) },
                                    onEnableDemoPreview = viewModel::enableDemoPreview,
                                    onDisableDemoPreview = viewModel::disableDemoPreview,
                                    onToggleDemoCaseLid = viewModel::toggleDemoCaseLid,
                                    onRefreshLiveTelemetry = viewModel::refreshLiveHardware,
                                    onSelectAncMode = viewModel::setAncMode,
                                    onChangeAncIntensity = viewModel::setAncIntensity,
                                    onToggleGamingMode = viewModel::toggleGamingMode,
                                    onCycleSpatialAudio = viewModel::setSpatialAudioMode,
                                    onToggleInEarDetection = viewModel::toggleInEarDetection,
                                    onSwitchMultipointHost = viewModel::switchMultipointPrimary
                                )
                            }
                            composable(SoniqRoutes.SOUND) {
                                SoundScreen(
                                    state = state,
                                    onToggleEqEnabled = viewModel::toggleEqualizerEnabled,
                                    onSelectPreset = viewModel::selectEqPreset,
                                    onUpdateBand = viewModel::updateEqBand,
                                    onUpdateBassBoost = viewModel::updateBassBoost,
                                    onSelectSpatialMode = viewModel::setSpatialAudioMode,
                                    onToggleGamingMode = viewModel::toggleGamingMode,
                                    onSaveProfile = viewModel::saveCurrentAsProfile
                                )
                            }
                            composable(SoniqRoutes.DEVICES) {
                                DevicesScreen(
                                    state = state,
                                    onRequestBluetoothPermissions = requestBluetoothPermissions,
                                    onRequestLocationPermission = {
                                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                    },
                                    onStartScan = viewModel::startScan,
                                    onStopScan = viewModel::stopScan,
                                    onPairDevice = viewModel::pairDevice,
                                    onConnectDevice = viewModel::connectDevice,
                                    onDisconnectDevice = viewModel::disconnectDevice,
                                    onEnableDemoPreview = viewModel::enableDemoPreview,
                                    onDisableDemoPreview = viewModel::disableDemoPreview,
                                    onUpdateGesture = viewModel::updateTouchGesture,
                                    onToggleDualDevice = viewModel::toggleDualDeviceMultipoint,
                                    onToggleCallNoiseReduction = viewModel::toggleCallNoiseReduction,
                                    onToggleMicSidetone = viewModel::toggleMicSidetone,
                                    onToggleFindChime = viewModel::toggleFindChime,
                                    onCaptureLastKnownLocation = viewModel::captureLastKnownLocation,
                                    onOpenBluetoothSettings = {
                                        runCatching {
                                            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                        }
                                    }
                                )
                            }
                            composable(SoniqRoutes.SYSTEM) {
                                SystemScreen(
                                    state = state,
                                    onSelectTheme = viewModel::setThemePreference,
                                    onToggleWidgetVisibility = viewModel::toggleDashboardWidgetVisibility,
                                    onMoveWidgetUp = viewModel::moveDashboardWidgetUp,
                                    onUpdateNotificationSettings = viewModel::updateNotificationSettings,
                                    onRequestNotificationPermission = requestNotificationPermission,
                                    onApplyProfile = viewModel::applyProfile,
                                    onDeleteProfile = viewModel::deleteProfile,
                                    onExportBackup = {
                                        exportBackupLauncher.launch("soniq_pro_backup.json")
                                    },
                                    onImportBackup = {
                                        importBackupLauncher.launch(arrayOf("application/json", "text/plain"))
                                    },
                                    onClearHistory = viewModel::clearHistory,
                                    onSetDiagnosticsConsent = viewModel::setDiagnosticsConsent,
                                    onRunDiagnostics = viewModel::runDiagnostics,
                                    onClearDiagnosticLogs = viewModel::clearDiagnosticLogs
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
