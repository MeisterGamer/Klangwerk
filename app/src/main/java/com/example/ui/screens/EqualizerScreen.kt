package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.EqualizerPreset
import com.example.data.model.EqualizerState
import com.example.data.model.formatFrequencyHz
import com.example.ui.theme.MonoTabularStyle
import kotlin.math.roundToInt

@Composable
fun EqualizerScreen(
  eqState: EqualizerState,
  onToggleEnabled: (Boolean) -> Unit,
  onSelectPreset: (EqualizerPreset) -> Unit,
  onBandLevelChange: (Int, Int) -> Unit,
  onBassBoostChange: (Int) -> Unit,
  onVirtualizerChange: (Int) -> Unit,
  onReset: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item(key = "eq_header") {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Klangstudio & Equalizer",
            style = MaterialTheme.typography.headlineMedium,
          )
          Text(
            text =
              if (eqState.hardwareAttached) "AudioFX aktiv mit laufender Wiedergabe gekoppelt"
              else "Voreinstellungen gespeichert • wird beim Abspielen aktiv",
            style = MaterialTheme.typography.bodySmall,
            color =
              if (eqState.hardwareAttached) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Switch(
          checked = eqState.enabled,
          onCheckedChange = onToggleEnabled,
          modifier = Modifier.testTag("eq_master_switch"),
        )
      }
    }

    item(key = "eq_presets") {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "Klangprofile",
            style = MaterialTheme.typography.titleMedium,
          )
          OutlinedButton(
            onClick = onReset,
            modifier = Modifier.testTag("eq_reset_button"),
          ) {
            Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Zurücksetzen")
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          EqualizerPreset.entries.forEach { preset ->
            FilterChip(
              selected = eqState.presetId == preset.id,
              onClick = { onSelectPreset(preset) },
              label = { Text(preset.label) },
              modifier = Modifier.testTag("eq_preset_${preset.id}"),
            )
          }
        }
      }
    }

    item(key = "eq_bands_card") {
      Card(
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Filled.GraphicEq,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("5-Band-Frequenzgang", style = MaterialTheme.typography.titleMedium)
            }
            Text(
              text =
                "${eqState.minLevelMb / 100} bis +${eqState.maxLevelMb / 100} dB",
              style = MonoTabularStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Box(modifier = Modifier.fillMaxWidth().height(230.dp)) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val tertiaryColor = MaterialTheme.colorScheme.tertiary
            val outlineColor = MaterialTheme.colorScheme.outlineVariant

            // Frequency response curve background
            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp)) {
              val w = size.width
              val h = size.height
              val midY = h / 2f

              // Grid lines (+max, 0dB, -min)
              drawLine(
                color = outlineColor.copy(alpha = 0.45f),
                start = Offset(0f, 0f),
                end = Offset(w, 0f),
                strokeWidth = 1.dp.toPx(),
              )
              drawLine(
                color = outlineColor,
                start = Offset(0f, midY),
                end = Offset(w, midY),
                strokeWidth = 1.5.dp.toPx(),
              )
              drawLine(
                color = outlineColor.copy(alpha = 0.45f),
                start = Offset(0f, h),
                end = Offset(w, h),
                strokeWidth = 1.dp.toPx(),
              )

              val range = (eqState.maxLevelMb - eqState.minLevelMb).coerceAtLeast(1)
              val points =
                eqState.bandLevelsMb.mapIndexed { idx, level ->
                  val x = if (eqState.bandLevelsMb.size > 1) w * idx / (eqState.bandLevelsMb.size - 1) else w / 2f
                  val norm = ((level - eqState.minLevelMb).toFloat() / range).coerceIn(0f, 1f)
                  val y = h * (1f - norm)
                  Offset(x, y)
                }

              if (points.size >= 2) {
                val curvePath = Path().apply {
                  moveTo(points.first().x, points.first().y)
                  for (i in 0 until points.lastIndex) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val cx = (p0.x + p1.x) / 2f
                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                  }
                }
                val activeColor = if (eqState.enabled) primaryColor else outlineColor
                drawPath(
                  path = curvePath,
                  brush = Brush.horizontalGradient(listOf(activeColor, tertiaryColor.copy(alpha = if (eqState.enabled) 0.9f else 0.3f))),
                  style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )
              }
            }

            // 5 vertical faders
            Row(
              modifier = Modifier.fillMaxSize(),
              horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
              for (bandIndex in 0 until 5) {
                val levelMb = eqState.bandLevelsMb.getOrElse(bandIndex) { 0 }
                val freqHz = eqState.bandFrequenciesHz.getOrElse(bandIndex) { 1000 }
                VerticalBandFader(
                  bandIndex = bandIndex,
                  frequencyLabel = formatFrequencyHz(freqHz),
                  levelMb = levelMb,
                  minMb = eqState.minLevelMb,
                  maxMb = eqState.maxLevelMb,
                  enabled = eqState.enabled,
                  onLevelChange = { newLevel -> onBandLevelChange(bandIndex, newLevel) },
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }
        }
      }
    }

    item(key = "eq_effects_row") {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EffectSliderCard(
          title = "Bass-Boost",
          subtitle = "Tiefton-Anhebung für satten Druck",
          icon = {
            Icon(
              Icons.Filled.Speaker,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.tertiary,
            )
          },
          strength = eqState.bassBoostStrength,
          enabled = eqState.enabled,
          onStrengthChange = onBassBoostChange,
          testTag = "eq_bass_boost_slider",
        )

        EffectSliderCard(
          title = "3D-Virtualizer",
          subtitle = "Erweitert die räumliche Stereobühne",
          icon = {
            Icon(
              Icons.Filled.SurroundSound,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
          },
          strength = eqState.virtualizerStrength,
          enabled = eqState.enabled,
          onStrengthChange = onVirtualizerChange,
          testTag = "eq_virtualizer_slider",
        )
      }
    }
  }
}

@Composable
private fun VerticalBandFader(
  bandIndex: Int,
  frequencyLabel: String,
  levelMb: Int,
  minMb: Int,
  maxMb: Int,
  enabled: Boolean,
  onLevelChange: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val dbValue = levelMb / 100f
  val dbFormatted = if (dbValue >= 0f) String.format("+%.1f", dbValue) else String.format("%.1f", dbValue)
  val range = (maxMb - minMb).coerceAtLeast(1)
  val fraction = ((levelMb - minMb).toFloat() / range).coerceIn(0f, 1f)

  val activeColor =
    if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
  val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
  val thumbRingColor = MaterialTheme.colorScheme.surface

  var trackHeightPx by remember { mutableFloatStateOf(1f) }

  Column(
    modifier = modifier.fillMaxHeight(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = "${dbFormatted}dB",
      style = MonoTabularStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
      color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Box(
      modifier =
        Modifier.weight(1f)
          .width(48.dp)
          .padding(vertical = 8.dp)
          .onSizeChanged { trackHeightPx = it.height.toFloat().coerceAtLeast(1f) }
          .pointerInput(minMb, maxMb) {
            detectTapGestures { offset ->
              val f = (1f - (offset.y / trackHeightPx)).coerceIn(0f, 1f)
              val nextLevel = (minMb + f * range).roundToInt()
              onLevelChange(nextLevel)
            }
          }
          .pointerInput(minMb, maxMb) {
            detectVerticalDragGestures { change, _ ->
              change.consume()
              val f = (1f - (change.position.y / trackHeightPx)).coerceIn(0f, 1f)
              val nextLevel = (minMb + f * range).roundToInt()
              onLevelChange(nextLevel)
            }
          }
          .testTag("eq_band_fader_$bandIndex"),
      contentAlignment = Alignment.Center,
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val barWidth = 6.dp.toPx()
        val pad = 10.dp.toPx()
        val usableH = (size.height - pad * 2).coerceAtLeast(1f)
        val thumbY = pad + usableH * (1f - fraction)
        val zeroFraction = ((0 - minMb).toFloat() / range).coerceIn(0f, 1f)
        val zeroY = pad + usableH * (1f - zeroFraction)

        // Track
        drawRoundRect(
          color = trackColor,
          topLeft = Offset(centerX - barWidth / 2f, pad),
          size = Size(barWidth, usableH),
          cornerRadius = CornerRadius(barWidth / 2f),
        )

        // Active segment from 0dB center to thumb
        val topY = minOf(thumbY, zeroY)
        val segH = kotlin.math.abs(thumbY - zeroY).coerceAtLeast(2.dp.toPx())
        drawRoundRect(
          color = activeColor.copy(alpha = if (enabled) 0.9f else 0.35f),
          topLeft = Offset(centerX - barWidth / 2f, topY),
          size = Size(barWidth, segH),
          cornerRadius = CornerRadius(barWidth / 2f),
        )

        // Thumb
        drawCircle(
          color = thumbRingColor,
          radius = 11.dp.toPx(),
          center = Offset(centerX, thumbY),
        )
        drawCircle(
          color = activeColor,
          radius = 8.dp.toPx(),
          center = Offset(centerX, thumbY),
        )
      }
    }

    Text(
      text = frequencyLabel,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun EffectSliderCard(
  title: String,
  subtitle: String,
  icon: @Composable () -> Unit,
  strength: Int,
  enabled: Boolean,
  onStrengthChange: (Int) -> Unit,
  testTag: String,
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    tonalElevation = 2.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        icon()
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(text = title, style = MaterialTheme.typography.titleSmall)
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
          text = "${(strength / 10)}%",
          style = MonoTabularStyle.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize),
          color =
            if (enabled && strength > 0) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Slider(
        value = strength.toFloat(),
        onValueChange = { onStrengthChange(it.roundToInt()) },
        valueRange = 0f..1000f,
        modifier = Modifier.fillMaxWidth().testTag(testTag),
      )
    }
  }
}
