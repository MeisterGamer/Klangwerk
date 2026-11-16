package com.example.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import androidx.media3.common.C
import com.example.data.model.EqualizerPreset
import com.example.data.model.EqualizerState
import com.example.data.preferences.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class AudioEffectsManager(private val settingsDataStore: SettingsDataStore) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  private val _state = MutableStateFlow(EqualizerState())
  val state: StateFlow<EqualizerState> = _state.asStateFlow()

  private var equalizer: Equalizer? = null
  private var bassBoost: BassBoost? = null
  private var virtualizer: Virtualizer? = null
  private var currentSessionId: Int = C.AUDIO_SESSION_ID_UNSET

  init {
    scope.launch {
      val saved = settingsDataStore.equalizerPreferences.first()
      _state.update { current ->
        current.copy(
          enabled = saved.enabled,
          presetId = saved.presetId,
          bandLevelsMb = saved.bandLevelsMb,
          bassBoostStrength = saved.bassBoostStrength,
          virtualizerStrength = saved.virtualizerStrength,
        )
      }
      applyCurrentStateToEffects()
    }
  }

  fun attachToSession(audioSessionId: Int) {
    if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId <= 0) return
    if (audioSessionId == currentSessionId && equalizer != null) {
      applyCurrentStateToEffects()
      return
    }
    releaseEffects()
    currentSessionId = audioSessionId

    var eqAttached = false
    runCatching {
      val eq = Equalizer(0, audioSessionId)
      equalizer = eq
      eqAttached = true

      val numBands = eq.numberOfBands.toInt().coerceAtLeast(1)
      val range = eq.bandLevelRange
      val minMb = range.getOrNull(0)?.toInt() ?: -1500
      val maxMb = range.getOrNull(1)?.toInt() ?: 1500

      val freqs =
        (0 until minOf(5, numBands)).map { band ->
          (eq.getCenterFreq(band.toShort()) / 1000).coerceAtLeast(20)
        }
      val paddedFreqs =
        if (freqs.size == 5) freqs else listOf(60, 230, 910, 3600, 14000)

      _state.update {
        it.copy(
          minLevelMb = minMb,
          maxLevelMb = maxMb,
          bandFrequenciesHz = paddedFreqs,
          hardwareAttached = true,
        )
      }
    }

    runCatching { bassBoost = BassBoost(0, audioSessionId) }
    runCatching { virtualizer = Virtualizer(0, audioSessionId) }

    if (!eqAttached) {
      _state.update { it.copy(hardwareAttached = false) }
    }
    applyCurrentStateToEffects()
  }

  fun setEnabled(enabled: Boolean) {
    _state.update { it.copy(enabled = enabled) }
    applyCurrentStateToEffects()
    persist()
  }

  fun selectPreset(preset: EqualizerPreset) {
    val cur = _state.value
    val levels =
      if (preset == EqualizerPreset.CUSTOM) {
        cur.bandLevelsMb
      } else {
        preset.relativeBands.map { ratio ->
          if (ratio >= 0f) {
            (ratio * cur.maxLevelMb).roundToInt()
          } else {
            (-ratio * cur.minLevelMb).roundToInt()
          }.coerceIn(cur.minLevelMb, cur.maxLevelMb)
        }
      }
    _state.update { it.copy(presetId = preset.id, bandLevelsMb = levels, enabled = true) }
    applyCurrentStateToEffects()
    persist()
  }

  fun setBandLevel(bandIndex: Int, levelMb: Int) {
    val cur = _state.value
    if (bandIndex !in 0..4) return
    val clamped = levelMb.coerceIn(cur.minLevelMb, cur.maxLevelMb)
    val updated = cur.bandLevelsMb.toMutableList().apply { this[bandIndex] = clamped }
    _state.update {
      it.copy(
        bandLevelsMb = updated,
        presetId = EqualizerPreset.CUSTOM.id,
        enabled = true,
      )
    }
    applyCurrentStateToEffects()
    persist()
  }

  fun setBassBoost(strength: Int) {
    val clamped = strength.coerceIn(0, 1000)
    _state.update { it.copy(bassBoostStrength = clamped, enabled = true) }
    applyCurrentStateToEffects()
    persist()
  }

  fun setVirtualizer(strength: Int) {
    val clamped = strength.coerceIn(0, 1000)
    _state.update { it.copy(virtualizerStrength = clamped, enabled = true) }
    applyCurrentStateToEffects()
    persist()
  }

  fun reset() {
    _state.update {
      it.copy(
        presetId = EqualizerPreset.LINEAR.id,
        bandLevelsMb = listOf(0, 0, 0, 0, 0),
        bassBoostStrength = 0,
        virtualizerStrength = 0,
      )
    }
    applyCurrentStateToEffects()
    persist()
  }

  private fun applyCurrentStateToEffects() {
    val s = _state.value
    runCatching {
      equalizer?.let { eq ->
        eq.enabled = s.enabled
        val count = minOf(eq.numberOfBands.toInt(), s.bandLevelsMb.size)
        for (i in 0 until count) {
          val clamped =
            s.bandLevelsMb[i].coerceIn(s.minLevelMb, s.maxLevelMb).toShort()
          eq.setBandLevel(i.toShort(), clamped)
        }
      }
    }
    runCatching {
      bassBoost?.let { bb ->
        bb.enabled = s.enabled && s.bassBoostStrength > 0
        if (bb.strengthSupported) {
          bb.setStrength(s.bassBoostStrength.coerceIn(0, 1000).toShort())
        }
      }
    }
    runCatching {
      virtualizer?.let { vz ->
        vz.enabled = s.enabled && s.virtualizerStrength > 0
        if (vz.strengthSupported) {
          vz.setStrength(s.virtualizerStrength.coerceIn(0, 1000).toShort())
        }
      }
    }
  }

  private fun persist() {
    val s = _state.value
    scope.launch {
      settingsDataStore.saveEqualizerConfig(
        enabled = s.enabled,
        presetId = s.presetId,
        bandLevelsMb = s.bandLevelsMb,
        bassBoostStrength = s.bassBoostStrength,
        virtualizerStrength = s.virtualizerStrength,
      )
    }
  }

  fun releaseEffects() {
    runCatching { equalizer?.release() }
    runCatching { bassBoost?.release() }
    runCatching { virtualizer?.release() }
    equalizer = null
    bassBoost = null
    virtualizer = null
    currentSessionId = C.AUDIO_SESSION_ID_UNSET
  }
}
