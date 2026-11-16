package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.MediaTrack
import com.example.data.model.PlaybackUiState
import com.example.data.model.RepeatModeState
import com.example.data.model.formatDuration
import com.example.ui.components.AnimatedPlayingBars
import com.example.ui.components.TrackArtwork
import com.example.ui.theme.MonoTabularStyle

@Composable
fun NowPlayingScreenOverlay(
  playbackState: PlaybackUiState,
  onClose: () -> Unit,
  onTogglePlayPause: () -> Unit,
  onSeekTo: (Long) -> Unit,
  onSkipNext: () -> Unit,
  onSkipPrevious: () -> Unit,
  onToggleShuffle: () -> Unit,
  onCycleRepeat: () -> Unit,
  onSpeedChange: (Float) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onAddToPlaylist: (MediaTrack) -> Unit,
  onOpenEqualizer: () -> Unit,
  onPlayQueueIndex: (Int) -> Unit,
  onRemoveFromQueue: (Int) -> Unit,
) {
  BackHandler(onBack = onClose)

  val track = playbackState.currentTrack ?: return
  var showQueue by remember { mutableStateOf(false) }
  var showSpeedMenu by remember { mutableStateOf(false) }
  var isSeeking by remember { mutableStateOf(false) }
  var seekDraftMs by remember { mutableFloatStateOf(0f) }

  val displayedPosMs =
    if (isSeeking) seekDraftMs.toLong() else playbackState.positionMs
  val durationMs = playbackState.durationMs.coerceAtLeast(1L)

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background,
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors =
                listOf(
                  MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                  MaterialTheme.colorScheme.background,
                  MaterialTheme.colorScheme.background,
                )
            )
          )
          .statusBarsPadding()
          .navigationBarsPadding()
    ) {
      Column(
        modifier =
          Modifier.fillMaxSize()
            .widthIn(max = 560.dp)
            .align(Alignment.Center)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // Top Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          IconButton(onClick = onClose, modifier = Modifier.testTag("now_playing_collapse")) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Player minimieren")
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = if (showQueue) "Warteschlange (${playbackState.queue.size})" else "Aktuelle Wiedergabe",
              style = MaterialTheme.typography.titleSmall,
            )
            Text(
              text = track.formatBadge,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.tertiary,
            )
          }
          IconButton(
            onClick = { showQueue = !showQueue },
            modifier = Modifier.testTag("now_playing_queue_toggle"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.QueueMusic,
              contentDescription = "Warteschlange anzeigen",
              tint =
                if (showQueue) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        if (showQueue) {
          LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            itemsIndexed(playbackState.queue, key = { idx, item -> "${item.uri}_$idx" }) { index, item ->
              val isCurrent = index == playbackState.currentIndex
              Surface(
                modifier =
                  Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onPlayQueueIndex(index) },
                color =
                  if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                  else MaterialTheme.colorScheme.surfaceContainerLow,
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  TrackArtwork(
                    artworkUri = item.artworkUri,
                    contentDescription = item.title,
                    size = 44.dp,
                    cornerRadius = 8.dp,
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = item.title,
                      style = MaterialTheme.typography.titleSmall,
                      color =
                        if (isCurrent) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = item.artist,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                  if (isCurrent && playbackState.isPlaying) {
                    AnimatedPlayingBars(modifier = Modifier.padding(end = 6.dp))
                  }
                  if (playbackState.queue.size > 1) {
                    IconButton(onClick = { onRemoveFromQueue(index) }) {
                      Icon(
                        Icons.Filled.Close,
                        contentDescription = "Aus Warteschlange entfernen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                }
              }
            }
          }
        } else {
          Spacer(modifier = Modifier.weight(0.5f))

          TrackArtwork(
            artworkUri = track.artworkUri,
            contentDescription = "Albumcover ${track.title}",
            size = 280.dp,
            cornerRadius = 28.dp,
            modifier = Modifier.fillMaxWidth(0.78f).aspectRatio(1f),
          )

          Spacer(modifier = Modifier.weight(0.5f))
        }

        // Title + Favorite
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = track.title,
              style = MaterialTheme.typography.headlineSmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "${track.artist} • ${track.album}",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          IconButton(onClick = { onToggleFavorite(track.uri) }) {
            Icon(
              imageVector =
                if (track.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
              contentDescription = "Favorit umschalten",
              tint =
                if (track.isFavorite) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Seekbar
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
          modifier = Modifier.fillMaxWidth().testTag("now_playing_seekbar"),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = formatDuration(displayedPosMs),
            style = MonoTabularStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = formatDuration(playbackState.durationMs),
            style = MonoTabularStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Transport Controls
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
          IconButton(
            onClick = onToggleShuffle,
            modifier = Modifier.testTag("now_playing_shuffle"),
          ) {
            Icon(
              Icons.Filled.Shuffle,
              contentDescription = "Zufallswiedergabe",
              tint =
                if (playbackState.shuffleEnabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(
            onClick = onSkipPrevious,
            modifier = Modifier.size(52.dp).testTag("now_playing_prev"),
          ) {
            Icon(
              Icons.Filled.SkipPrevious,
              contentDescription = "Vorheriger Titel",
              modifier = Modifier.size(34.dp),
            )
          }
          FilledIconButton(
            onClick = onTogglePlayPause,
            modifier = Modifier.size(68.dp).testTag("now_playing_play_pause"),
            shape = CircleShape,
            colors =
              IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
              ),
          ) {
            Icon(
              imageVector =
                if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
              contentDescription = if (playbackState.isPlaying) "Pause" else "Wiedergabe",
              modifier = Modifier.size(36.dp),
            )
          }
          IconButton(
            onClick = onSkipNext,
            modifier = Modifier.size(52.dp).testTag("now_playing_next"),
          ) {
            Icon(
              Icons.Filled.SkipNext,
              contentDescription = "Nächster Titel",
              modifier = Modifier.size(34.dp),
            )
          }
          IconButton(
            onClick = onCycleRepeat,
            modifier = Modifier.testTag("now_playing_repeat"),
          ) {
            Icon(
              imageVector =
                if (playbackState.repeatMode == RepeatModeState.ONE) Icons.Filled.RepeatOne
                else Icons.Filled.Repeat,
              contentDescription = "Wiederholen",
              tint =
                if (playbackState.repeatMode != RepeatModeState.OFF)
                  MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Utility Row: Speed, Playlist, Equalizer
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box {
            TextButton(onClick = { showSpeedMenu = true }) {
              Text(
                text = String.format("%.2gx", playbackState.playbackSpeed),
                style = MonoTabularStyle,
              )
            }
            DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false }) {
              listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
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

          TextButton(onClick = { onAddToPlaylist(track) }) {
            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Playlist")
          }

          TextButton(
            onClick = {
              onClose()
              onOpenEqualizer()
            },
            modifier = Modifier.testTag("now_playing_open_eq"),
          ) {
            Icon(Icons.Filled.GraphicEq, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Equalizer")
          }
        }
      }
    }
  }
}
