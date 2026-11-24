package com.example.data.bluetooth

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import com.example.domain.model.EqBandState
import com.example.domain.model.EqPresetId
import com.example.domain.model.EqualizerState

/**
 * Real Android AudioFX DSP engine wrapping android.media.audiofx.{Equalizer, BassBoost, Virtualizer}.
 * Attaches safely to output mix session 0 and honestly reports availability if blocked by HAL/OEM.
 */
class AudioFxDspEngine {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    var isEqAvailable: Boolean = false
        private set
    var isBassBoostAvailable: Boolean = false
        private set
    var isVirtualizerAvailable: Boolean = false
        private set

    fun initialize(): EqualizerState {
        release()
        var eqStatus = "System AudioFX DSP ready"
        val bands = mutableListOf<EqBandState>()

        runCatching {
            val eq = Equalizer(0, 0).apply { enabled = true }
            equalizer = eq
            isEqAvailable = true
            val count = eq.numberOfBands.toInt().coerceAtMost(5)
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
        }.onFailure { err ->
            isEqAvailable = false
            eqStatus = "Global AudioFX unavailable on this device (${err.javaClass.simpleName})"
        }

        runCatching {
            val bass = BassBoost(0, 0).apply { enabled = true }
            bassBoost = bass
            isBassBoostAvailable = bass.strengthSupported
        }.onFailure {
            isBassBoostAvailable = false
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
            activePreset = EqPresetId.STUDIO_REFERENCE,
            bands = if (bands.isNotEmpty()) bands else EqualizerState.defaultBands(),
            bassBoostAvailable = isBassBoostAvailable,
            bassBoostStrength = 0,
            virtualizerAvailable = isVirtualizerAvailable,
            virtualizerStrength = 0,
            statusMessage = eqStatus
        )
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        runCatching { equalizer?.enabled = enabled }
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
        runCatching { virtualizer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
    }

    private fun formatFreq(hz: Int): String = if (hz >= 1000) {
        val khz = hz / 1000f
        if (khz == khz.toInt().toFloat()) "${khz.toInt()}kHz" else String.format("%.1fkHz", khz)
    } else {
        "${hz}Hz"
    }
}
