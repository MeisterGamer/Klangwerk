package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppThemeSettings
import com.example.data.model.EqualizerPreset
import com.example.data.model.EqualizerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "klangwerk_prefs")

class SettingsDataStore(private val context: Context) {

  private object Keys {
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val AMOLED_BLACK = booleanPreferencesKey("amoled_black")
    val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
    val EQ_PRESET_ID = stringPreferencesKey("eq_preset_id")
    val EQ_BANDS = stringPreferencesKey("eq_bands_mb")
    val EQ_BASS_BOOST = intPreferencesKey("eq_bass_boost")
    val EQ_VIRTUALIZER = intPreferencesKey("eq_virtualizer")
  }

  val themeSettings: Flow<AppThemeSettings> =
    context.dataStore.data.map { prefs ->
      AppThemeSettings(
        dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
        amoledBlack = prefs[Keys.AMOLED_BLACK] ?: false,
      )
    }

  val equalizerPreferences: Flow<EqualizerState> =
    context.dataStore.data.map { prefs ->
      val parsedBands =
        prefs[Keys.EQ_BANDS]
          ?.split(",")
          ?.mapNotNull { it.trim().toIntOrNull() }
          ?.takeIf { it.size == 5 }
          ?: listOf(0, 0, 0, 0, 0)

      EqualizerState(
        enabled = prefs[Keys.EQ_ENABLED] ?: false,
        presetId = prefs[Keys.EQ_PRESET_ID] ?: EqualizerPreset.LINEAR.id,
        bandLevelsMb = parsedBands,
        bassBoostStrength = prefs[Keys.EQ_BASS_BOOST] ?: 0,
        virtualizerStrength = prefs[Keys.EQ_VIRTUALIZER] ?: 0,
      )
    }

  suspend fun setDynamicColor(enabled: Boolean) {
    context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
  }

  suspend fun setAmoledBlack(enabled: Boolean) {
    context.dataStore.edit { it[Keys.AMOLED_BLACK] = enabled }
  }

  suspend fun saveEqualizerConfig(
    enabled: Boolean,
    presetId: String,
    bandLevelsMb: List<Int>,
    bassBoostStrength: Int,
    virtualizerStrength: Int,
  ) {
    context.dataStore.edit { prefs ->
      prefs[Keys.EQ_ENABLED] = enabled
      prefs[Keys.EQ_PRESET_ID] = presetId
      prefs[Keys.EQ_BANDS] = bandLevelsMb.joinToString(",")
      prefs[Keys.EQ_BASS_BOOST] = bassBoostStrength.coerceIn(0, 1000)
      prefs[Keys.EQ_VIRTUALIZER] = virtualizerStrength.coerceIn(0, 1000)
    }
  }
}
