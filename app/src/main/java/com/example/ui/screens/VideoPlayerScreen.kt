package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.PlaybackUiState
import com.example.data.model.formatDuration
import com.example.ui.theme.MonoTabularStyle
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
  player: Player?,
  playbackState: PlaybackUiState,
  onNavigateBack: () -> Unit,
  onTogglePlayPause: () -> Unit,
  onSeekTo: (Long) -> Unit,
  onSpeedChange: (Float) -> Unit,
  onOpenEqualizer: () -> Unit,
) {
  BackHandler(onBack = onNavigateBack)

  val context = LocalContext.current
  val activity = context as? Activity
  var controlsVisible by remember { mutableStateOf(true) }
  var isLandscapeFullscreen by remember { mutableStateOf(false) }
  var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
  var showSpeedMenu by remember { mutableStateOf(false) }
  var isSeeking by remember { mutableStateOf(false) }
  var seekDraftMs by remember { mutableFloatStateOf(0f) }

  // Restore orientation & system bars on exit
  DisposableEffect(Unit) {
    onDispose {
      activity?.let { act ->
        act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val controller = WindowCompat.getInsetsController(act.window, act.window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
      }
    }
  }

  LaunchedEffect(isLandscapeFullscreen) {
    activity?.let { act ->
      act.requestedOrientation =
        if (isLandscapeFullscreen) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
      val controller = WindowCompat.getInsetsController(act.window, act.window.decorView)
      if (isLandscapeFullscreen) {
        controller.systemBarsBehavior =
          WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
      } else {
        controller.show(WindowInsetsCompat.Type.systemBars())
      }
    }
  }

  LaunchedEffect(controlsVisible, playbackState.isPlaying, isSeeking) {
    if (controlsVisible && playbackState.isPlaying && !isSeeking) {
      delay(3500L)
      controlsVisible = false
    }
  }

  val displayedPosMs = if (isSeeking) seekDraftMs.toLong() else playbackState.positionMs
  val durationMs = playbackState.durationMs.coerceAtLeast(1L)

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color.Black)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
        ) {
          controlsVisible = !controlsVisible
        }
  ) {
    AndroidView(
      factory = { ctx ->
        PlayerView(ctx).apply {
          useController = false
          keepScreenOn = true
          layoutParams =
            ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
      },
      update = { view ->
        if (view.player !== player) {
          view.player = player
        }
        view.resizeMode = resizeMode
      },
      onRelease = { view -> view.player = null },
      modifier = Modifier.fillMaxSize(),
    )

    AnimatedVisibility(
      visible = controlsVisible,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.fillMaxSize(),
    ) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(
              Brush.verticalGradient(
                listOf(
                  Color.Black.copy(alpha = 0.72f),
                  Color.Black.copy(alpha = 0.15f),
                  Color.Black.copy(alpha = 0.78f),
                )
              )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
      ) {
        // Top Bar
        Row(
          modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(horizontal = 8.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("video_player_back"),
          ) {
            Icon(
              Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Zurück",
              tint = Color.White,
            )
          }
          Text(
            text = playbackState.currentTrack?.title ?: "Video",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          IconButton(onClick = onOpenEqualizer, modifier = Modifier.testTag("video_eq_button")) {
            Icon(Icons.Filled.GraphicEq, contentDescription = "Equalizer", tint = Color.White)
          }
          IconButton(
            onClick = {
              resizeMode =
                if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT)
                  AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                else AspectRatioFrameLayout.RESIZE_MODE_FIT
            },
          ) {
            Icon(
              Icons.Filled.AspectRatio,
              contentDescription = "Seitenverhältnis wechseln",
              tint = Color.White,
            )
          }
          IconButton(
            onClick = { isLandscapeFullscreen = !isLandscapeFullscreen },
            modifier = Modifier.testTag("video_fullscreen_toggle"),
          ) {
            Icon(
              if (isLandscapeFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
              contentDescription = "Vollbild Querformat",
              tint = Color.White,
            )
          }
        }

        // Center Transport
        Row(
          modifier = Modifier.align(Alignment.Center),
          horizontalArrangement = Arrangement.spacedBy(28.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
            onClick = { onSeekTo((playbackState.positionMs - 10_000L).coerceAtLeast(0L)) },
            modifier = Modifier.size(56.dp),
          ) {
            Icon(
              Icons.Filled.Replay10,
              contentDescription = "10 Sekunden zurück",
              tint = Color.White,
              modifier = Modifier.size(34.dp),
            )
          }

          FilledIconButton(
            onClick = onTogglePlayPause,
            shape = CircleShape,
            modifier = Modifier.size(68.dp).testTag("video_play_pause_button"),
            colors =
              IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
              ),
          ) {
            Icon(
              if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
              contentDescription = if (playbackState.isPlaying) "Pause" else "Wiedergabe",
              modifier = Modifier.size(36.dp),
            )
          }

          IconButton(
            onClick = {
              onSeekTo((playbackState.positionMs + 10_000L).coerceAtMost(durationMs))
            },
            modifier = Modifier.size(56.dp),
          ) {
            Icon(
              Icons.Filled.Forward10,
              contentDescription = "10 Sekunden vor",
              tint = Color.White,
              modifier = Modifier.size(34.dp),
            )
          }
        }

        // Bottom Seekbar & Speed
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .align(Alignment.BottomCenter)
              .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
          Slider(
            value = displayedPosMs.toFloat().coerceIn(0f, durationMs.toFloat()),
            onValueChange = {
              isSeeking = true
              seekDraftMs = it
            },
            onValueChangeFinished = {
              isSeeking = false
              onSeekTo(seekDraftMs.toLong())
            },
            valueRange = 0f..durationMs.toFloat(),
            modifier = Modifier.fillMaxWidth(),
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "${formatDuration(displayedPosMs)} / ${formatDuration(playbackState.durationMs)}",
              style = MonoTabularStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
              color = Color.White,
            )
            Box {
              TextButton(onClick = { showSpeedMenu = true }) {
                Text(
                  text = String.format("%.2gx", playbackState.playbackSpeed),
                  style = MonoTabularStyle,
                  color = Color.White,
                )
              }
              DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false }) {
                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                  DropdownMenuItem(
                    text = { Text("${speed}x") },
                    onClick = {
                      showSpeedMenu = false
                      onSpeedChange(speed)
                    },
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
