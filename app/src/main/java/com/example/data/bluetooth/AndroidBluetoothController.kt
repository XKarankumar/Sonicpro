package com.example.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import com.example.domain.model.AudioCodecInfo
import com.example.domain.model.BatteryTelemetry
import com.example.domain.model.BluetoothConnectionState
import com.example.domain.model.DataSourceMode
import com.example.domain.model.EarbudsDevice
import com.example.domain.protocol.DeviceProtocolRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class BluetoothPermissionState(
    val hasScanPermission: Boolean,
    val hasConnectPermission: Boolean,
    val hasLocationPermission: Boolean,
    val hasNotificationPermission: Boolean
) {
    val hasAllRequiredBluetoothPermissions: Boolean
        get() = hasScanPermission && hasConnectPermission
}

data class BluetoothHardwareSnapshot(
    val isBluetoothSupported: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val isScanning: Boolean = false,
    val a2dpProxyReady: Boolean = false,
    val headsetProxyReady: Boolean = false,
    val permissionState: BluetoothPermissionState,
    val bondedAudioDevices: List<EarbudsDevice> = emptyList(),
    val discoveredDevices: List<EarbudsDevice> = emptyList(),
    val activeLiveDevice: EarbudsDevice? = null,
    val lastStatusMessage: String = "Ready"
)

/**
 * Lifecycle-aware real Android Bluetooth Classic + BLE + GATT controller.
 * Reads genuine bonded devices, BLE scan advertisements, GATT Battery (0x180F),
 * GATT Device Information (0x180A), A2DP codec status, and Android battery intents.
 */
@SuppressLint("MissingPermission")
class AndroidBluetoothController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onConnectionLifecycleEvent: (address: String, name: String, event: String, detail: String) -> Unit,
    private val onBatteryObserved: (address: String, name: String, battery: BatteryTelemetry) -> Unit
) {
    companion object {
        private val UUID_BATTERY_SERVICE: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        private val UUID_BATTERY_LEVEL: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        private val UUID_DEVICE_INFO_SERVICE: UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")
        private val UUID_FIRMWARE_REV: UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")
        private val UUID_HARDWARE_REV: UUID = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb")
        private val UUID_MANUFACTURER_NAME: UUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")
        private const val ACTION_BATTERY_LEVEL_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        private const val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var a2dpProxy: BluetoothA2dp? = null
    private var headsetProxy: BluetoothHeadset? = null
    private var activeGatt: BluetoothGatt? = null
    private var scanTimeoutJob: Job? = null
    private var receiverRegistered = false

    private val gattQueue = ArrayDeque<BluetoothGattCharacteristic>()
    private val cachedGattMetadata = mutableMapOf<String, GattDeviceSnapshot>()

    private data class GattDeviceSnapshot(
        val batteryPercent: Int? = null,
        val firmwareVersion: String? = null,
        val hardwareRevision: String? = null,
        val manufacturerName: String? = null,
        val hasBatteryService: Boolean = false,
        val hasDeviceInfoService: Boolean = false
    )

    private var systemEqAvailable = false
    private var systemBassAvailable = false
    private var systemVirtAvailable = false

    private val _snapshot = MutableStateFlow(
        BluetoothHardwareSnapshot(
            isBluetoothSupported = bluetoothAdapter != null,
            isBluetoothEnabled = runCatching { bluetoothAdapter?.isEnabled == true }.getOrDefault(false),
            permissionState = computePermissionState()
        )
    )
    val snapshot: StateFlow<BluetoothHardwareSnapshot> = _snapshot.asStateFlow()

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.A2DP) {
                a2dpProxy = proxy as? BluetoothA2dp
                _snapshot.update { it.copy(a2dpProxyReady = true) }
            } else if (profile == BluetoothProfile.HEADSET) {
                headsetProxy = proxy as? BluetoothHeadset
                _snapshot.update { it.copy(headsetProxyReady = true) }
            }
            refreshBondedAndConnected()
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.A2DP) {
                a2dpProxy = null
                _snapshot.update { it.copy(a2dpProxyReady = false) }
            } else if (profile == BluetoothProfile.HEADSET) {
                headsetProxy = null
                _snapshot.update { it.copy(headsetProxyReady = false) }
            }
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            when (action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    val enabled = state == BluetoothAdapter.STATE_ON
                    _snapshot.update {
                        it.copy(
                            isBluetoothEnabled = enabled,
                            lastStatusMessage = if (enabled) "Bluetooth powered on" else "Bluetooth turned off"
                        )
                    }
                    if (enabled) refreshBondedAndConnected()
                }
                BluetoothDevice.ACTION_FOUND -> {
                    val device = extractDevice(intent) ?: return
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                        .takeIf { it != Short.MIN_VALUE }?.toInt()
                    upsertDiscoveredDevice(device, rssi, emptySet(), emptyList())
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device = extractDevice(intent) ?: return
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
                    val label = when (bondState) {
                        BluetoothDevice.BOND_BONDED -> "BONDED"
                        BluetoothDevice.BOND_BONDING -> "BONDING"
                        else -> "UNBONDED"
                    }
                    val name = safeDeviceName(device)
                    onConnectionLifecycleEvent(device.address, name, label, "Bond state updated to $label")
                    refreshBondedAndConnected()
                }
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED,
                BluetoothDevice.ACTION_ACL_CONNECTED,
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = extractDevice(intent)
                    if (device != null) {
                        val name = safeDeviceName(device)
                        val eventType = if (action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
                            "DISCONNECTED"
                        } else {
                            "CONNECTION_UPDATE"
                        }
                        onConnectionLifecycleEvent(device.address, name, eventType, "System profile update: ${action.substringAfterLast('.')}")
                    }
                    refreshBondedAndConnected()
                }
                ACTION_BATTERY_LEVEL_CHANGED -> {
                    val device = extractDevice(intent) ?: return
                    val level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1)
                    if (level in 0..100) {
                        val name = safeDeviceName(device)
                        val telemetry = BatteryTelemetry(
                            singlePercent = level,
                            sourceLabel = "Android Bluetooth Battery API",
                            lastUpdatedMs = System.currentTimeMillis()
                        )
                        onBatteryObserved(device.address, name, telemetry)
                    }
                    refreshBondedAndConnected()
                }
            }
        }
    }

    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val record = result.scanRecord
            val mfgIds = mutableSetOf<Int>()
            record?.manufacturerSpecificData?.let { sparse ->
                for (i in 0 until sparse.size()) {
                    mfgIds.add(sparse.keyAt(i))
                }
            }
            val uuids = record?.serviceUuids.orEmpty()
            upsertDiscoveredDevice(device, result.rssi, mfgIds, uuids)
        }

        override fun onScanFailed(errorCode: Int) {
            _snapshot.update {
                it.copy(
                    isScanning = false,
                    lastStatusMessage = "BLE scan failed (code $errorCode)"
                )
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                runCatching { gatt.discoverServices() }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runCatching { gatt.close() }
                if (activeGatt == gatt) activeGatt = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return
            val addr = gatt.device.address
            val batteryService = gatt.getService(UUID_BATTERY_SERVICE)
            val infoService = gatt.getService(UUID_DEVICE_INFO_SERVICE)

            val prev = cachedGattMetadata[addr] ?: GattDeviceSnapshot()
            cachedGattMetadata[addr] = prev.copy(
                hasBatteryService = batteryService != null,
                hasDeviceInfoService = infoService != null
            )

            gattQueue.clear()
            batteryService?.getCharacteristic(UUID_BATTERY_LEVEL)?.let { gattQueue.addLast(it) }
            infoService?.getCharacteristic(UUID_FIRMWARE_REV)?.let { gattQueue.addLast(it) }
            infoService?.getCharacteristic(UUID_HARDWARE_REV)?.let { gattQueue.addLast(it) }
            infoService?.getCharacteristic(UUID_MANUFACTURER_NAME)?.let { gattQueue.addLast(it) }

            readNextGattCharacteristic(gatt)
            refreshBondedAndConnected()
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                @Suppress("DEPRECATION")
                val value = characteristic.value ?: ByteArray(0)
                handleCharacteristicBytes(gatt.device, characteristic.uuid, value)
            }
            readNextGattCharacteristic(gatt)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristicBytes(gatt.device, characteristic.uuid, value)
            }
            readNextGattCharacteristic(gatt)
        }
    }

    fun updateAudioFxAvailability(eqAvailable: Boolean, bassAvailable: Boolean, virtAvailable: Boolean) {
        systemEqAvailable = eqAvailable
        systemBassAvailable = bassAvailable
        systemVirtAvailable = virtAvailable
        refreshBondedAndConnected()
    }

    fun start() {
        registerReceiverIfNeeded()
        refreshPermissionState()
        val adapter = bluetoothAdapter ?: return
        if (_snapshot.value.permissionState.hasConnectPermission) {
            runCatching {
                adapter.getProfileProxy(context, profileListener, BluetoothProfile.A2DP)
                adapter.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET)
            }
        }
        refreshBondedAndConnected()
    }

    fun stop() {
        stopScan()
        runCatching { activeGatt?.close() }
        activeGatt = null
        if (receiverRegistered) {
            runCatching { context.unregisterReceiver(bluetoothReceiver) }
            receiverRegistered = false
        }
        a2dpProxy?.let { runCatching { bluetoothAdapter?.closeProfileProxy(BluetoothProfile.A2DP, it) } }
        headsetProxy?.let { runCatching { bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HEADSET, it) } }
        a2dpProxy = null
        headsetProxy = null
    }

    fun refreshPermissionState() {
        val perms = computePermissionState()
        _snapshot.update {
            it.copy(
                permissionState = perms,
                isBluetoothSupported = bluetoothAdapter != null,
                isBluetoothEnabled = runCatching { bluetoothAdapter?.isEnabled == true }.getOrDefault(false)
            )
        }
        if (perms.hasConnectPermission) {
            val adapter = bluetoothAdapter
            if (adapter != null && a2dpProxy == null) {
                runCatching {
                    adapter.getProfileProxy(context, profileListener, BluetoothProfile.A2DP)
                    adapter.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET)
                }
            }
            refreshBondedAndConnected()
        }
    }

    fun refreshBondedAndConnected() {
        val perms = computePermissionState()
        val adapter = bluetoothAdapter
        if (adapter == null || !perms.hasConnectPermission) {
            _snapshot.update {
                it.copy(
                    permissionState = perms,
                    isBluetoothEnabled = runCatching { adapter?.isEnabled == true }.getOrDefault(false),
                    bondedAudioDevices = emptyList(),
                    activeLiveDevice = null
                )
            }
            return
        }

        val connectedA2dp = runCatching { a2dpProxy?.connectedDevices.orEmpty() }.getOrDefault(emptyList())
        val connectedHfp = runCatching { headsetProxy?.connectedDevices.orEmpty() }.getOrDefault(emptyList())
        val connectedSet = (connectedA2dp + connectedHfp).map { it.address }.toSet()

        val bonded = runCatching { adapter.bondedDevices.orEmpty() }.getOrDefault(emptySet())
        val mapped = bonded.map { device ->
            mapBluetoothDevice(
                device = device,
                isConnected = device.address in connectedSet,
                rssiDbm = null,
                manufacturerIds = emptySet(),
                serviceUuids = emptyList()
            )
        }.sortedWith(
            compareByDescending<EarbudsDevice> { it.connectionState == BluetoothConnectionState.CONNECTED }
                .thenByDescending { it.isAudioDevice }
                .thenBy { it.name }
        )

        val primaryConnected = mapped.firstOrNull {
            it.connectionState == BluetoothConnectionState.CONNECTED && it.isAudioDevice
        } ?: mapped.firstOrNull { it.connectionState == BluetoothConnectionState.CONNECTED }

        if (primaryConnected != null && activeGatt?.device?.address != primaryConnected.address) {
            val rawDevice = bonded.firstOrNull { it.address == primaryConnected.address }
            if (rawDevice != null) {
                connectGattForTelemetry(rawDevice)
            }
        }

        _snapshot.update { state ->
            state.copy(
                permissionState = perms,
                isBluetoothEnabled = runCatching { adapter.isEnabled }.getOrDefault(false),
                bondedAudioDevices = mapped,
                activeLiveDevice = primaryConnected
            )
        }
    }

    fun startScan() {
        val perms = computePermissionState()
        if (!perms.hasScanPermission || !perms.hasConnectPermission) {
            _snapshot.update { it.copy(lastStatusMessage = "Bluetooth permissions required to scan") }
            return
        }
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _snapshot.update { it.copy(lastStatusMessage = "Turn on Bluetooth to scan for earbuds") }
            return
        }

        stopScan()
        _snapshot.update {
            it.copy(
                isScanning = true,
                discoveredDevices = emptyList(),
                lastStatusMessage = "Scanning nearby Bluetooth & BLE audio devices..."
            )
        }

        val scanner: BluetoothLeScanner? = runCatching { adapter.bluetoothLeScanner }.getOrNull()
        if (scanner != null) {
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            runCatching { scanner.startScan(null, settings, leScanCallback) }
        }
        runCatching {
            if (!adapter.isDiscovering) adapter.startDiscovery()
        }

        scanTimeoutJob = scope.launch {
            delay(12_000L)
            stopScan()
        }
    }

    fun stopScan() {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null
        val adapter = bluetoothAdapter ?: return
        if (computePermissionState().hasScanPermission) {
            runCatching { adapter.bluetoothLeScanner?.stopScan(leScanCallback) }
            runCatching { if (adapter.isDiscovering) adapter.cancelDiscovery() }
        }
        _snapshot.update {
            it.copy(
                isScanning = false,
                lastStatusMessage = if (it.discoveredDevices.isEmpty()) "Scan finished" else "Found ${it.discoveredDevices.size} nearby device(s)"
            )
        }
    }

    fun pairDevice(address: String): Boolean {
        val adapter = bluetoothAdapter ?: return false
        if (!computePermissionState().hasConnectPermission) return false
        val device = runCatching { adapter.getRemoteDevice(address) }.getOrNull() ?: return false
        stopScan()
        return runCatching { device.createBond() }.getOrDefault(false)
    }

    fun connectDevice(address: String): Boolean {
        val adapter = bluetoothAdapter ?: return false
        if (!computePermissionState().hasConnectPermission) return false
        val device = runCatching { adapter.getRemoteDevice(address) }.getOrNull() ?: return false

        var initiated = false
        a2dpProxy?.let { proxy ->
            runCatching {
                val method = BluetoothA2dp::class.java.getMethod("connect", BluetoothDevice::class.java)
                initiated = (method.invoke(proxy, device) as? Boolean) == true
            }
        }
        headsetProxy?.let { proxy ->
            runCatching {
                val method = BluetoothHeadset::class.java.getMethod("connect", BluetoothDevice::class.java)
                val hfpOk = (method.invoke(proxy, device) as? Boolean) == true
                initiated = initiated || hfpOk
            }
        }
        connectGattForTelemetry(device)
        return initiated
    }

    fun disconnectDevice(address: String): Boolean {
        val adapter = bluetoothAdapter ?: return false
        if (!computePermissionState().hasConnectPermission) return false
        val device = runCatching { adapter.getRemoteDevice(address) }.getOrNull() ?: return false

        var disconnected = false
        a2dpProxy?.let { proxy ->
            runCatching {
                val method = BluetoothA2dp::class.java.getMethod("disconnect", BluetoothDevice::class.java)
                disconnected = (method.invoke(proxy, device) as? Boolean) == true
            }
        }
        headsetProxy?.let { proxy ->
            runCatching {
                val method = BluetoothHeadset::class.java.getMethod("disconnect", BluetoothDevice::class.java)
                val hfpOk = (method.invoke(proxy, device) as? Boolean) == true
                disconnected = disconnected || hfpOk
            }
        }
        if (activeGatt?.device?.address == address) {
            runCatching { activeGatt?.disconnect() }
        }
        return disconnected
    }

    fun refreshLiveTelemetryNow() {
        val active = _snapshot.value.activeLiveDevice ?: return
        val adapter = bluetoothAdapter ?: return
        if (!computePermissionState().hasConnectPermission) return
        val raw = runCatching { adapter.getRemoteDevice(active.address) }.getOrNull() ?: return
        connectGattForTelemetry(raw)
        refreshBondedAndConnected()
    }

    private fun connectGattForTelemetry(device: BluetoothDevice) {
        if (!computePermissionState().hasConnectPermission) return
        runCatching {
            activeGatt?.close()
            activeGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        }
    }

    private fun readNextGattCharacteristic(gatt: BluetoothGatt) {
        val next = if (gattQueue.isNotEmpty()) gattQueue.removeFirst() else null
        if (next != null) {
            runCatching { gatt.readCharacteristic(next) }
        }
    }

    private fun handleCharacteristicBytes(device: BluetoothDevice, uuid: UUID, value: ByteArray) {
        val prev = cachedGattMetadata[device.address] ?: GattDeviceSnapshot()
        val updated = when (uuid) {
            UUID_BATTERY_LEVEL -> {
                val pct = value.firstOrNull()?.toInt()?.and(0xFF)?.takeIf { it in 0..100 }
                if (pct != null) {
                    onBatteryObserved(
                        device.address,
                        safeDeviceName(device),
                        BatteryTelemetry(
                            singlePercent = pct,
                            sourceLabel = "BLE GATT Battery Service (0x180F)",
                            lastUpdatedMs = System.currentTimeMillis()
                        )
                    )
                }
                prev.copy(batteryPercent = pct ?: prev.batteryPercent, hasBatteryService = true)
            }
            UUID_FIRMWARE_REV -> prev.copy(
                firmwareVersion = value.toString(Charsets.UTF_8).trim('\u0000', ' '),
                hasDeviceInfoService = true
            )
            UUID_HARDWARE_REV -> prev.copy(
                hardwareRevision = value.toString(Charsets.UTF_8).trim('\u0000', ' '),
                hasDeviceInfoService = true
            )
            UUID_MANUFACTURER_NAME -> prev.copy(
                manufacturerName = value.toString(Charsets.UTF_8).trim('\u0000', ' '),
                hasDeviceInfoService = true
            )
            else -> prev
        }
        cachedGattMetadata[device.address] = updated
        refreshBondedAndConnected()
    }

    private fun upsertDiscoveredDevice(
        device: BluetoothDevice,
        rssi: Int?,
        manufacturerIds: Set<Int>,
        serviceUuids: List<ParcelUuid>
    ) {
        val mapped = mapBluetoothDevice(
            device = device,
            isConnected = false,
            rssiDbm = rssi,
            manufacturerIds = manufacturerIds,
            serviceUuids = serviceUuids
        )
        _snapshot.update { state ->
            val current = state.discoveredDevices.toMutableList()
            val idx = current.indexOfFirst { it.address == mapped.address }
            if (idx >= 0) {
                current[idx] = mapped
            } else {
                current.add(mapped)
            }
            state.copy(
                discoveredDevices = current.sortedWith(
                    compareByDescending<EarbudsDevice> { it.isAudioDevice }
                        .thenByDescending { it.rssiDbm ?: -120 }
                )
            )
        }
    }

    private fun mapBluetoothDevice(
        device: BluetoothDevice,
        isConnected: Boolean,
        rssiDbm: Int?,
        manufacturerIds: Set<Int>,
        serviceUuids: List<ParcelUuid>
    ): EarbudsDevice {
        val name = safeDeviceName(device)
        val gattMeta = cachedGattMetadata[device.address]
        val systemBattery = readSystemBatteryLevel(device)
        val bestBattery = gattMeta?.batteryPercent ?: systemBattery
        val batterySource = when {
            gattMeta?.batteryPercent != null -> "BLE GATT Battery (0x180F)"
            systemBattery != null -> "Android HFP/Battery API"
            else -> "Not reported by device"
        }

        val codec = if (isConnected) readA2dpCodecInfo(device) else null
        val adapter = DeviceProtocolRegistry.resolveAdapter(name, manufacturerIds, serviceUuids)
        val brand = DeviceProtocolRegistry.inferBrand(name, manufacturerIds, serviceUuids)
        val isAudio = runCatching {
            device.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO ||
                name.lowercase().let { n ->
                    n.contains("bud") || n.contains("pod") || n.contains("ear") ||
                        n.contains("audio") || n.contains("head") || n.contains("sony") ||
                        n.contains("enco") || n.contains("oneplus")
                }
        }.getOrDefault(false)

        val capabilities = adapter.resolveCapabilities(
            hasGattBatteryService = gattMeta?.hasBatteryService == true,
            hasDeviceInfoService = gattMeta?.hasDeviceInfoService == true,
            hasSingleBatteryLevel = systemBattery != null,
            hasCodecInfo = codec != null,
            systemEqAvailable = systemEqAvailable,
            systemBassAvailable = systemBassAvailable,
            systemVirtualizerAvailable = systemVirtAvailable
        )

        return EarbudsDevice(
            address = device.address,
            name = name,
            brand = brand,
            rssiDbm = rssiDbm,
            isBonded = runCatching { device.bondState == BluetoothDevice.BOND_BONDED }.getOrDefault(false),
            isAudioDevice = isAudio,
            connectionState = if (isConnected) BluetoothConnectionState.CONNECTED else BluetoothConnectionState.DISCONNECTED,
            battery = BatteryTelemetry(
                singlePercent = bestBattery,
                sourceLabel = batterySource,
                lastUpdatedMs = System.currentTimeMillis()
            ),
            codecInfo = codec,
            firmwareVersion = gattMeta?.firmwareVersion,
            hardwareRevision = gattMeta?.hardwareRevision,
            manufacturerName = gattMeta?.manufacturerName,
            capabilities = capabilities,
            dataSourceMode = DataSourceMode.LIVE_HARDWARE
        )
    }

    private fun readSystemBatteryLevel(device: BluetoothDevice): Int? {
        return runCatching {
            val method = BluetoothDevice::class.java.getMethod("getBatteryLevel")
            val level = method.invoke(device) as? Int ?: -1
            level.takeIf { it in 0..100 }
        }.getOrNull()
    }

    private fun readA2dpCodecInfo(device: BluetoothDevice): AudioCodecInfo? {
        val proxy = a2dpProxy ?: return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        return runCatching {
            val statusMethod = BluetoothA2dp::class.java.getMethod("getCodecStatus", BluetoothDevice::class.java)
            val status = statusMethod.invoke(proxy, device) ?: return null
            val configMethod = status.javaClass.getMethod("getCodecConfig")
            val config = configMethod.invoke(status) ?: return null
            val type = config.javaClass.getMethod("getCodecType").invoke(config) as? Int ?: -1
            val sampleRateCode = config.javaClass.getMethod("getSampleRate").invoke(config) as? Int ?: 0
            val bitsCode = config.javaClass.getMethod("getBitsPerSample").invoke(config) as? Int ?: 0
            val channelCode = config.javaClass.getMethod("getChannelMode").invoke(config) as? Int ?: 0

            val codecName = when (type) {
                0 -> "SBC"
                1 -> "AAC"
                2 -> "aptX"
                3 -> "aptX HD"
                4 -> "LDAC"
                5 -> "LC3"
                else -> "Bluetooth Audio ($type)"
            }
            val sampleRateHz = when {
                (sampleRateCode and 0x01) != 0 -> 44100
                (sampleRateCode and 0x02) != 0 -> 48000
                (sampleRateCode and 0x04) != 0 -> 88200
                (sampleRateCode and 0x08) != 0 -> 96000
                else -> null
            }
            val bits = when {
                (bitsCode and 0x01) != 0 -> 16
                (bitsCode and 0x02) != 0 -> 24
                (bitsCode and 0x04) != 0 -> 32
                else -> null
            }
            val ch = when {
                (channelCode and 0x02) != 0 -> "Stereo"
                (channelCode and 0x01) != 0 -> "Mono"
                else -> null
            }
            AudioCodecInfo(codecName, sampleRateHz, bits, ch)
        }.getOrNull()
    }

    private fun safeDeviceName(device: BluetoothDevice): String {
        return runCatching { device.name?.takeIf { it.isNotBlank() } }.getOrNull()
            ?: "Bluetooth Device (${device.address.takeLast(5)})"
    }

    @Suppress("DEPRECATION")
    private fun extractDevice(intent: Intent): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }

    private fun registerReceiverIfNeeded() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(ACTION_BATTERY_LEVEL_CHANGED)
        }
        ContextCompat.registerReceiver(
            context,
            bluetoothReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        receiverRegistered = true
    }

    fun computePermissionState(): BluetoothPermissionState {
        val scanOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPerm(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            hasPerm(Manifest.permission.BLUETOOTH) &&
                hasPerm(Manifest.permission.BLUETOOTH_ADMIN) &&
                hasPerm(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        val connectOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPerm(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            hasPerm(Manifest.permission.BLUETOOTH)
        }
        val locOk = hasPerm(Manifest.permission.ACCESS_COARSE_LOCATION) ||
            hasPerm(Manifest.permission.ACCESS_FINE_LOCATION)
        val notifOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPerm(Manifest.permission.POST_NOTIFICATIONS)
        } else true
        return BluetoothPermissionState(scanOk, connectOk, locOk, notifOk)
    }

    private fun hasPerm(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
