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
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SurroundSound
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SurroundSound
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.screens.DevicesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.screens.TheatreScreen
import com.example.ui.theme.SoniqProTheme
import com.example.ui.viewmodel.SoniqUiEvent
import com.example.ui.viewmodel.SoniqViewModel
import kotlinx.coroutines.launch

object SoniqRoutes {
    const val HOME = "home"
    const val DEVICES = "devices"
    const val STUDIO = "studio"
    const val THEATRE = "theatre"
    const val SETTINGS = "settings"
}

private data class NavTab(
    val route: String,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val soniqTabs = listOf(
    NavTab(SoniqRoutes.HOME, R.string.nav_home, Icons.Rounded.Home, Icons.Outlined.Home),
    NavTab(SoniqRoutes.DEVICES, R.string.nav_devices, Icons.Rounded.Devices, Icons.Outlined.Devices),
    NavTab(SoniqRoutes.STUDIO, R.string.nav_studio, Icons.Rounded.GraphicEq, Icons.Outlined.GraphicEq),
    NavTab(SoniqRoutes.THEATRE, R.string.nav_theatre, Icons.Rounded.SurroundSound, Icons.Outlined.SurroundSound),
    NavTab(SoniqRoutes.SETTINGS, R.string.nav_settings, Icons.Rounded.Settings, Icons.Outlined.Settings)
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
    val currentRoute = backStackEntry?.destination?.route ?: SoniqRoutes.HOME

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
                    snackbarHostState.showSnackbar("SONIQ backup exported")
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

    val exportReportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                runCatching {
                    val report = viewModel.generateCompatibilityReport()
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(report.toByteArray(Charsets.UTF_8))
                    }
                }.onSuccess {
                    snackbarHostState.showSnackbar("Compatibility report exported")
                }.onFailure { err ->
                    snackbarHostState.showSnackbar("Report export failed: ${err.message}")
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
                                val label = stringResource(tab.labelResId)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    modifier = Modifier.testTag("nav_tab_${tab.route}"),
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
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
                                val label = stringResource(tab.labelResId)
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    modifier = Modifier.testTag("nav_rail_${tab.route}"),
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label, maxLines = 1) },
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
                            startDestination = SoniqRoutes.HOME
                        ) {
                            composable(SoniqRoutes.HOME) {
                                HomeScreen(
                                    state = state,
                                    onRequestBluetoothPermissions = requestBluetoothPermissions,
                                    onNavigateToDevices = { navigateToTab(SoniqRoutes.DEVICES) },
                                    onNavigateToStudio = { navigateToTab(SoniqRoutes.STUDIO) },
                                    onRefreshLiveTelemetry = viewModel::refreshLiveHardware,
                                    onConnectDevice = viewModel::connectDevice,
                                    onDisconnectDevice = viewModel::disconnectDevice,
                                    onSelectInspectedDevice = viewModel::selectInspectedDevice
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
                                    onSelectInspectedDevice = viewModel::selectInspectedDevice,
                                    onToggleFindChime = viewModel::toggleFindChime,
                                    onCaptureLastKnownLocation = viewModel::captureLastKnownLocation,
                                    onOpenBluetoothSettings = {
                                        runCatching {
                                            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                        }
                                    }
                                )
                            }
                            composable(SoniqRoutes.STUDIO) {
                                StudioScreen(
                                    state = state,
                                    onToggleEqEnabled = viewModel::toggleEqualizerEnabled,
                                    onSelectPreset = viewModel::selectEqPreset,
                                    onUpdateBand = viewModel::updateEqBand,
                                    onUpdateBassBoost = viewModel::updateBassBoost,
                                    onUpdateVocalGain = viewModel::updateVocalGain,
                                    onUpdateVirtualizer = viewModel::updateVirtualizer,
                                    onRefreshSpatializer = viewModel::refreshSpatializer,
                                    onOpenSoundSettings = {
                                        runCatching {
                                            context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
                                        }
                                    },
                                    onSaveProfile = viewModel::saveCurrentAsProfile
                                )
                            }
                            composable(SoniqRoutes.THEATRE) {
                                TheatreScreen(
                                    state = state,
                                    onStartNetworkScan = viewModel::startTheatreScan,
                                    onStopNetworkScan = viewModel::stopTheatreScan,
                                    onRefreshRoutes = viewModel::refreshTheatreRoutes,
                                    onSetVolume = viewModel::setTheatreVolume,
                                    onToggleMute = viewModel::toggleTheatreMute,
                                    onMediaPlayPause = viewModel::dispatchMediaPlayPause,
                                    onMediaNext = viewModel::dispatchMediaNext,
                                    onMediaPrevious = viewModel::dispatchMediaPrevious,
                                    onConnectBluetoothSpeaker = viewModel::connectDevice,
                                    onDisconnectBluetoothSpeaker = viewModel::disconnectDevice
                                )
                            }
                            composable(SoniqRoutes.SETTINGS) {
                                SettingsScreen(
                                    state = state,
                                    onSelectTheme = viewModel::setThemePreference,
                                    onToggleWidgetVisibility = viewModel::toggleDashboardWidgetVisibility,
                                    onMoveWidgetUp = viewModel::moveDashboardWidgetUp,
                                    onUpdateNotificationSettings = viewModel::updateNotificationSettings,
                                    onRequestNotificationPermission = requestNotificationPermission,
                                    onApplyProfile = viewModel::applyProfile,
                                    onDeleteProfile = viewModel::deleteProfile,
                                    onExportBackup = {
                                        exportBackupLauncher.launch("soniq_backup.json")
                                    },
                                    onImportBackup = {
                                        importBackupLauncher.launch(arrayOf("application/json", "text/plain"))
                                    },
                                    onExportCompatibilityReport = {
                                        exportReportLauncher.launch("soniq_compatibility_report.txt")
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
