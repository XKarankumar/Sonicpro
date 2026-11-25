package com.example.data.bluetooth

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.Spatializer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Build
import com.example.domain.model.EqBandState
import com.example.domain.model.EqPresetId
import com.example.domain.model.EqualizerState
import com.example.domain.model.SpatialAudioCapabilityState
import com.example.domain.model.SpatialImmersiveLevel
import kotlin.math.roundToInt

/**
 * Real Android AudioFX DSP & Android 13+ Spatializer inspector.
 * Uses android.media.audiofx.{Equalizer, BassBoost, LoudnessEnhancer, Virtualizer}
 * and android.media.Spatializer with zero simulated effects.
 */
class AudioFxDspEngine(private val context: Context) {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var virtualizer: Virtualizer? = null

    var isEqAvailable: Boolean = false
        private set
    var isBassBoostAvailable: Boolean = false
        private set
    var isVocalEnhancerAvailable: Boolean = false
        private set
    var isVirtualizerAvailable: Boolean = false
        private set

    fun initialize(): EqualizerState {
        release()
        var eqStatus = "AudioFX DSP active"
        val bands = mutableListOf<EqBandState>()

        runCatching {
            val eq = Equalizer(0, 0).apply { enabled = true }
            equalizer = eq
            isEqAvailable = true
            val count = eq.numberOfBands.toInt().coerceIn(1, 10)
            val range = eq.bandLevelRange
            val minMb = range.getOrNull(0)?.toInt() ?: -1500
            val maxMb = range.getOrNull(1)?.toInt() ?: 1500
            for (i in 0 until count) {
                val freqHz = (eq.getCenterFreq(i.toShort()) / 1000).coerceAtLeast(20)
                val level = eq.getBandLevel(i.toShort()).toInt()
                bands.add(
                    EqBandState(
                        index = i,
                        centerFreqHz = freqHz,
                        label = formatFreq(freqHz),
                        levelMb = level,
                        minMb = minMb,
                        maxMb = maxMb
                    )
                )
            }
            eqStatus = "Live AudioFX ${count}-band DSP active"
        }.onFailure { err ->
            isEqAvailable = false
            eqStatus = "Global AudioFX unavailable on this audio HAL (${err.javaClass.simpleName})"
        }

        runCatching {
            val bass = BassBoost(0, 0).apply { enabled = true }
            bassBoost = bass
            isBassBoostAvailable = bass.strengthSupported
        }.onFailure {
            isBassBoostAvailable = false
        }

        runCatching {
            val loudness = LoudnessEnhancer(0).apply { enabled = false }
            loudnessEnhancer = loudness
            isVocalEnhancerAvailable = true
        }.onFailure {
            isVocalEnhancerAvailable = false
        }

        runCatching {
            val virt = Virtualizer(0, 0).apply { enabled = true }
            virtualizer = virt
            isVirtualizerAvailable = virt.strengthSupported
        }.onFailure {
            isVirtualizerAvailable = false
        }

        return EqualizerState(
            isEngineAvailable = isEqAvailable,
            isEnabled = isEqAvailable,
            hardwareBandCount = if (bands.isNotEmpty()) bands.size else 5,
            activePreset = EqPresetId.STUDIO_REFERENCE,
            bands = if (bands.isNotEmpty()) bands else EqualizerState.defaultBands(),
            bassBoostAvailable = isBassBoostAvailable,
            bassBoostStrength = 0,
            vocalEnhancerAvailable = isVocalEnhancerAvailable,
            vocalGainMb = 0,
            virtualizerAvailable = isVirtualizerAvailable,
            virtualizerStrength = 0,
            statusMessage = eqStatus
        )
    }

    fun inspectSpatializer(): SpatialAudioCapabilityState {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && audioManager != null) {
            return runCatching {
                val spatializer = audioManager.spatializer
                val level = when (spatializer.immersiveAudioLevel) {
                    Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_NONE -> SpatialImmersiveLevel.NONE
                    Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_MULTICHANNEL -> SpatialImmersiveLevel.MULTICHANNEL
                    Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_OTHER -> SpatialImmersiveLevel.BED_PLUS_OBJECTS
                    else -> SpatialImmersiveLevel.MULTICHANNEL
                }
                val isAvail = spatializer.isAvailable && level != SpatialImmersiveLevel.NONE
                val isEnabled = spatializer.isEnabled
                val headTrackerAvail = spatializer.isHeadTrackerAvailable
                val headTrackerEnabled = if (headTrackerAvail) {
                    runCatching {
                        val method = Spatializer::class.java.getMethod("isHeadTrackerEnabled")
                        method.invoke(spatializer) as? Boolean ?: false
                    }.getOrDefault(false)
                } else false

                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()

                val stereoOk = canSpatializeMask(spatializer, attrs, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
                val ch51Ok = canSpatializeMask(spatializer, attrs, AudioFormat.CHANNEL_OUT_5POINT1, AudioFormat.ENCODING_PCM_16BIT)
                val ch71Ok = canSpatializeMask(spatializer, attrs, AudioFormat.CHANNEL_OUT_7POINT1_SURROUND, AudioFormat.ENCODING_PCM_16BIT)
                val dolbyOk = canSpatializeMask(spatializer, attrs, AudioFormat.CHANNEL_OUT_5POINT1, AudioFormat.ENCODING_E_AC3)

                val explanation = when {
                    !isAvail -> "UNAVAILABLE — Current audio output or device HAL does not support Android Spatializer."
                    !isEnabled -> "Supported by Android Spatializer, currently disabled in system Audio settings."
                    !headTrackerAvail -> "Active (${level.label}). Head tracking UNAVAILABLE (requires compatible IMU-equipped headphones)."
                    else -> "Active (${level.label}) with hardware IMU head-tracking support."
                }

                SpatialAudioCapabilityState(
                    apiSupported = true,
                    isAvailable = isAvail,
                    isEnabled = isEnabled,
                    immersiveLevel = level,
                    isHeadTrackerAvailable = headTrackerAvail,
                    isHeadTrackerEnabled = headTrackerEnabled,
                    supportsStereo = stereoOk,
                    supports5Point1 = ch51Ok,
                    supports7Point1 = ch71Ok,
                    supportsDolbyMultichannel = dolbyOk,
                    explanation = explanation
                )
            }.getOrElse {
                SpatialAudioCapabilityState(
                    apiSupported = true,
                    explanation = "Spatializer query failed: ${it.localizedMessage}"
                )
            }
        }

        return SpatialAudioCapabilityState(
            apiSupported = false,
            isAvailable = isVirtualizerAvailable,
            isEnabled = (virtualizer?.enabled == true) && isVirtualizerAvailable,
            immersiveLevel = if (isVirtualizerAvailable) SpatialImmersiveLevel.LEGACY_VIRTUALIZER else SpatialImmersiveLevel.NONE,
            isHeadTrackerAvailable = false,
            explanation = if (isVirtualizerAvailable) {
                "Android < 13 uses legacy AudioFX Virtualizer. Hardware IMU head tracking is UNAVAILABLE."
            } else {
                "UNAVAILABLE — Requires Android 13 (API 33+) Spatializer HAL."
            }
        )
    }

    private fun canSpatializeMask(
        spatializer: Spatializer,
        attrs: AudioAttributes,
        channelMask: Int,
        encoding: Int
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        return runCatching {
            val format = AudioFormat.Builder()
                .setEncoding(encoding)
                .setSampleRate(48000)
                .setChannelMask(channelMask)
                .build()
            spatializer.canBeSpatialized(attrs, format)
        }.getOrDefault(false)
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        runCatching { equalizer?.enabled = enabled }
    }

    fun mapPresetToHardwareBands(preset: EqPresetId, currentBands: List<EqBandState>): List<Int> =
        Companion.mapPresetToHardwareBands(preset, currentBands)

    companion object {
        fun mapPresetToHardwareBands(preset: EqPresetId, currentBands: List<EqBandState>): List<Int> {
            val curve = preset.targetCurveDb
            if (currentBands.isEmpty()) return emptyList()
            return currentBands.mapIndexed { idx, band ->
                val mappedPos = if (currentBands.size == 1) 0f
                else (idx.toFloat() / (currentBands.size - 1)) * (curve.size - 1)
                val lo = mappedPos.toInt().coerceIn(0, curve.lastIndex)
                val hi = (lo + 1).coerceIn(0, curve.lastIndex)
                val frac = mappedPos - lo
                val db = curve[lo] * (1f - frac) + curve[hi] * frac
                (db * 100f).roundToInt().coerceIn(band.minMb, band.maxMb)
            }
        }
    }

    fun setBandLevel(bandIndex: Int, levelMb: Int) {
        val eq = equalizer ?: return
        runCatching {
            val range = eq.bandLevelRange
            val clamped = levelMb.coerceIn(range[0].toInt(), range[1].toInt()).toShort()
            eq.setBandLevel(bandIndex.toShort(), clamped)
        }
    }

    fun applyBands(levelsMb: List<Int>) {
        val eq = equalizer ?: return
        runCatching {
            val count = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            for (i in 0 until minOf(count, levelsMb.size)) {
                val clamped = levelsMb[i].coerceIn(range[0].toInt(), range[1].toInt()).toShort()
                eq.setBandLevel(i.toShort(), clamped)
            }
        }
    }

    fun setBassBoostStrength(strength: Int) {
        val bass = bassBoost ?: return
        runCatching {
            val clamped = strength.coerceIn(0, 1000).toShort()
            bass.enabled = clamped > 0
            if (bass.strengthSupported) {
                bass.setStrength(clamped)
            }
        }
    }

    fun setVocalGainMb(gainMb: Int) {
        val enhancer = loudnessEnhancer ?: return
        runCatching {
            val clamped = gainMb.coerceIn(0, 1500)
            enhancer.enabled = clamped > 0
            enhancer.setTargetGain(clamped)
        }
    }

    fun setVirtualizerStrength(strength: Int) {
        val virt = virtualizer ?: return
        runCatching {
            val clamped = strength.coerceIn(0, 1000).toShort()
            virt.enabled = clamped > 0
            if (virt.strengthSupported) {
                virt.setStrength(clamped)
            }
        }
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { loudnessEnhancer?.release() }
        runCatching { virtualizer?.release() }
        equalizer = null
        bassBoost = null
        loudnessEnhancer = null
        virtualizer = null
    }

    private fun formatFreq(hz: Int): String = if (hz >= 1000) {
        val khz = hz / 1000f
        if (khz == khz.toInt().toFloat()) "${khz.toInt()}kHz" else String.format("%.1fkHz", khz)
    } else {
        "${hz}Hz"
    }
}
