package com.example.data.bluetooth

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.domain.model.EarbudSide
import com.example.domain.model.LastKnownLocationRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class FindEarbudsEngine(private val context: Context) {

    private var chimeJob: Job? = null
    private var activeTrack: AudioTrack? = null

    fun startLocatorChime(
        scope: CoroutineScope,
        targetSide: EarbudSide?,
        onStopped: () -> Unit
    ) {
        stopLocatorChime()
        chimeJob = scope.launch(Dispatchers.Default) {
            val sampleRate = 44100
            val durationMs = 260
            val sampleCount = (sampleRate * durationMs) / 1000
            val buffer = ShortArray(sampleCount * 2) // Stereo PCM16

            val leftGain = if (targetSide == null || targetSide == EarbudSide.LEFT) 1.0f else 0.0f
            val rightGain = if (targetSide == null || targetSide == EarbudSide.RIGHT) 1.0f else 0.0f

            for (i in 0 until sampleCount) {
                val t = i.toDouble() / sampleRate
                val env = when {
                    i < 400 -> i / 400.0
                    i > sampleCount - 600 -> (sampleCount - i) / 600.0
                    else -> 1.0
                }
                val tone = (sin(2.0 * PI * 2450.0 * t) * 0.65 + sin(2.0 * PI * 3200.0 * t) * 0.35) * env
                val sample = (tone * Short.MAX_VALUE * 0.7).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                buffer[i * 2] = (sample * leftGain).toInt().toShort()
                buffer[i * 2 + 1] = (sample * rightGain).toInt().toShort()
            }

            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val track = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(minBuf, buffer.size * 2))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
            }.getOrNull()

            if (track == null) {
                onStopped()
                return@launch
            }
            activeTrack = track

            try {
                track.write(buffer, 0, buffer.size)
                var repeats = 0
                while (isActive && repeats < 20) {
                    track.stop()
                    track.reloadStaticData()
                    track.play()
                    delay(650L)
                    repeats++
                }
            } finally {
                runCatching {
                    track.stop()
                    track.release()
                }
                activeTrack = null
                onStopped()
            }
        }
    }

    fun stopLocatorChime() {
        chimeJob?.cancel()
        chimeJob = null
        runCatching {
            activeTrack?.stop()
            activeTrack?.release()
        }
        activeTrack = null
    }

    fun captureCoarseLastKnownLocation(deviceName: String, deviceAddress: String): LastKnownLocationRecord? {
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasCoarse && !hasFine) return null

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
            LocationManager.GPS_PROVIDER
        )
        val best = providers.mapNotNull { provider ->
            runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time } ?: return null

        return LastKnownLocationRecord(
            deviceName = deviceName,
            deviceAddress = deviceAddress,
            latitude = best.latitude,
            longitude = best.longitude,
            accuracyMeters = best.accuracy,
            recordedAtMs = best.time.takeIf { it > 0L } ?: System.currentTimeMillis()
        )
    }

    fun vibrateTap() {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(14L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(14L)
            }
        }
    }
}

class SoniqNotificationHelper(private val context: Context) {
    companion object {
        const val CHANNEL_ALERTS = "soniq_alerts"
    }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val channel = NotificationChannel(
                CHANNEL_ALERTS,
                "Earbuds Battery & Connection Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts for low earbud battery, device connections, and app release updates."
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun canPostNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun postAlert(notificationId: Int, title: String, body: String) {
        if (!canPostNotifications()) return
        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }
}
