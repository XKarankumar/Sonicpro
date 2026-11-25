package com.example.data.bluetooth

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.SystemClock
import android.view.KeyEvent
import com.example.domain.model.AudioOutputRoute
import com.example.domain.model.TheatreEcosystemState
import com.example.domain.model.TheatreNetworkDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Genuine Home Theatre & Network Audio Manager.
 * Uses Android NsdManager (mDNS/DNS-SD) for Cast, AirPlay, Sonos, Spotify Connect, and DLNA discovery,
 * AudioManager.getDevices() for HDMI/ARC/eARC/USB/A2DP output inspection,
 * and AudioManager stream volume & KeyEvent media transport controls.
 */
class HomeTheatreManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val activeListeners = mutableListOf<NsdManager.DiscoveryListener>()
    private var scanTimeoutJob: Job? = null

    private val _state = MutableStateFlow(TheatreEcosystemState())
    val state: StateFlow<TheatreEcosystemState> = _state.asStateFlow()

    private val targetServices = listOf(
        "_googlecast._tcp." to "Google Cast",
        "_airplay._tcp." to "AirPlay",
        "_sonos._tcp." to "Sonos",
        "_spotify-connect._tcp." to "Spotify Connect",
        "_http._tcp." to "DLNA / Network AVR"
    )

    fun refreshRoutesAndVolume() {
        val routes = readActiveOutputRoutes()
        val curVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
        val muted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager?.isStreamMute(AudioManager.STREAM_MUSIC) ?: false
        } else {
            curVol == 0
        }
        val wifiOk = isWifiConnected()

        _state.update {
            it.copy(
                isWifiConnected = wifiOk,
                activeOutputRoutes = routes,
                currentStreamVolume = curVol,
                maxStreamVolume = maxVol,
                isMuted = muted
            )
        }
    }

    fun startNetworkDiscovery() {
        stopNetworkDiscovery()
        refreshRoutesAndVolume()

        val nsd = nsdManager
        if (nsd == null) {
            _state.update { it.copy(statusMessage = "Android NsdManager unavailable on this device") }
            return
        }
        if (!isWifiConnected()) {
            _state.update {
                it.copy(
                    isScanningNetwork = false,
                    statusMessage = "Connect to Wi-Fi to discover local Home Theatre & network speakers"
                )
            }
            return
        }

        _state.update {
            it.copy(
                isScanningNetwork = true,
                discoveredNetworkDevices = emptyList(),
                statusMessage = "Scanning LAN via mDNS (Cast, AirPlay, Sonos, Spotify Connect, DLNA)..."
            )
        }

        targetServices.forEach { (serviceType, label) ->
            val listener = object : NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                    runCatching { nsd.stopServiceDiscovery(this) }
                }

                override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                    runCatching { nsd.stopServiceDiscovery(this) }
                }

                override fun onDiscoveryStarted(regType: String?) {}

                override fun onDiscoveryStopped(serviceType: String?) {}

                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    val name = serviceInfo.serviceName ?: return
                    upsertDevice(
                        TheatreNetworkDevice(
                            serviceName = name,
                            serviceType = serviceType,
                            protocolLabel = label,
                            hostAddress = null,
                            port = null,
                            modelHint = null,
                            isResolved = false
                        )
                    )
                    @Suppress("DEPRECATION")
                    runCatching {
                        nsd.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(si: NsdServiceInfo?, errorCode: Int) {}
                            override fun onServiceResolved(resolved: NsdServiceInfo) {
                                val model = resolved.attributes["md"]?.toString(Charsets.UTF_8)
                                    ?: resolved.attributes["fn"]?.toString(Charsets.UTF_8)
                                @Suppress("DEPRECATION")
                                val host = resolved.host?.hostAddress
                                upsertDevice(
                                    TheatreNetworkDevice(
                                        serviceName = resolved.serviceName ?: name,
                                        serviceType = serviceType,
                                        protocolLabel = label,
                                        hostAddress = host,
                                        port = resolved.port.takeIf { p -> p > 0 },
                                        modelHint = model,
                                        isResolved = true
                                    )
                                )
                            }
                        })
                    }
                }

                override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                    val lost = serviceInfo.serviceName ?: return
                    _state.update { s ->
                        s.copy(
                            discoveredNetworkDevices = s.discoveredNetworkDevices.filterNot {
                                it.serviceName == lost && it.serviceType == serviceType
                            }
                        )
                    }
                }
            }
            activeListeners.add(listener)
            runCatching {
                nsd.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
            }
        }

        scanTimeoutJob = scope.launch {
            delay(10_000L)
            stopNetworkDiscovery()
        }
    }

    fun stopNetworkDiscovery() {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null
        val nsd = nsdManager
        if (nsd != null) {
            activeListeners.forEach { listener ->
                runCatching { nsd.stopServiceDiscovery(listener) }
            }
        }
        activeListeners.clear()
        _state.update { s ->
            s.copy(
                isScanningNetwork = false,
                statusMessage = if (s.discoveredNetworkDevices.isEmpty()) {
                    "LAN scan finished — no mDNS audio renderers advertised"
                } else {
                    "Discovered ${s.discoveredNetworkDevices.size} network audio endpoint(s)"
                }
            )
        }
    }

    fun setStreamVolume(volume: Int) {
        val am = audioManager ?: return
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val clamped = volume.coerceIn(0, max)
        runCatching { am.setStreamVolume(AudioManager.STREAM_MUSIC, clamped, 0) }
        refreshRoutesAndVolume()
    }

    fun toggleMute() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val currentlyMuted = am.isStreamMute(AudioManager.STREAM_MUSIC)
            val dir = if (currentlyMuted) AudioManager.ADJUST_UNMUTE else AudioManager.ADJUST_MUTE
            runCatching { am.adjustStreamVolume(AudioManager.STREAM_MUSIC, dir, 0) }
        }
        refreshRoutesAndVolume()
    }

    fun dispatchMediaKey(keyCode: Int) {
        val am = audioManager ?: return
        val now = SystemClock.uptimeMillis()
        runCatching {
            am.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
            am.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0))
        }
    }

    private fun upsertDevice(device: TheatreNetworkDevice) {
        _state.update { s ->
            val list = s.discoveredNetworkDevices.toMutableList()
            val idx = list.indexOfFirst {
                it.serviceName == device.serviceName && it.serviceType == device.serviceType
            }
            if (idx >= 0) list[idx] = device else list.add(device)
            s.copy(discoveredNetworkDevices = list)
        }
    }

    private fun readActiveOutputRoutes(): List<AudioOutputRoute> {
        val am = audioManager ?: return emptyList()
        return runCatching {
            am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).map { dev ->
                val typeLabel = when (dev.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth A2DP"
                    AudioDeviceInfo.TYPE_HDMI -> "HDMI"
                    AudioDeviceInfo.TYPE_HDMI_ARC -> "HDMI ARC"
                    AudioDeviceInfo.TYPE_HDMI_EARC -> "HDMI eARC"
                    AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Audio"
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Audio"
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Device Speaker"
                    else -> "Audio Output (${dev.type})"
                }
                val isTheatre = dev.type == AudioDeviceInfo.TYPE_HDMI ||
                    dev.type == AudioDeviceInfo.TYPE_HDMI_ARC ||
                    dev.type == AudioDeviceInfo.TYPE_HDMI_EARC ||
                    dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    dev.type == AudioDeviceInfo.TYPE_USB_DEVICE
                val encodings = dev.encodings.map { enc ->
                    when (enc) {
                        android.media.AudioFormat.ENCODING_PCM_16BIT -> "PCM 16"
                        android.media.AudioFormat.ENCODING_PCM_24BIT_PACKED -> "PCM 24"
                        android.media.AudioFormat.ENCODING_PCM_FLOAT -> "PCM Float"
                        android.media.AudioFormat.ENCODING_AC3 -> "Dolby AC3"
                        android.media.AudioFormat.ENCODING_E_AC3 -> "Dolby Digital+"
                        android.media.AudioFormat.ENCODING_DTS -> "DTS"
                        android.media.AudioFormat.ENCODING_DTS_HD -> "DTS-HD"
                        else -> "Fmt $enc"
                    }
                }.distinct()

                AudioOutputRoute(
                    id = dev.id,
                    productName = dev.productName?.toString()?.takeIf { it.isNotBlank() } ?: typeLabel,
                    typeLabel = typeLabel,
                    isHomeTheatreCapable = isTheatre,
                    supportedEncodings = encodings
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun isWifiConnected(): Boolean {
        val cm = connectivityManager ?: return false
        return runCatching {
            val net = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        }.getOrDefault(false)
    }
}
