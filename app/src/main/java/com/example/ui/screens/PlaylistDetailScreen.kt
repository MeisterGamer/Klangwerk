package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.MediaTrack
import com.example.data.model.formatDuration
import com.example.ui.components.TrackArtwork
import com.example.ui.components.TrackListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
  title: String,
  tracks: List<MediaTrack>,
  isCustomPlaylist: Boolean,
  currentTrackUri: String?,
  isPlaying: Boolean,
  onNavigateBack: () -> Unit,
  onPlayQueue: (List<MediaTrack>, Int) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onPlayNext: (MediaTrack) -> Unit,
  onAddToQueue: (MediaTrack) -> Unit,
  onAddToPlaylist: (MediaTrack) -> Unit,
  onShowDetails: (MediaTrack) -> Unit,
  onMoveTrack: (Int, Int) -> Unit,
  onRemoveFromPlaylist: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text(title) },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("playlist_detail_back"),
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
          }
        },
      )
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentPadding = PaddingValues(bottom = 110.dp),
    ) {
      item(key = "playlist_header") {
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          TrackArtwork(
            artworkUri = tracks.firstOrNull { it.artworkUri != null }?.artworkUri,
            contentDescription = title,
            size = 132.dp,
            cornerRadius = 20.dp,
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(text = title, style = MaterialTheme.typography.headlineSmall)
          Text(
            text = "${tracks.size} Titel • ${formatDuration(tracks.sumOf { it.durationMs })}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(14.dp))
          if (tracks.isNotEmpty()) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Button(
                onClick = { onPlayQueue(tracks, 0) },
                modifier = Modifier.weight(1f).testTag("playlist_play_all_button"),
              ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Alle abspielen")
              }
              OutlinedButton(
                onClick = { onPlayQueue(tracks.shuffled(), 0) },
                modifier = Modifier.weight(1f).testTag("playlist_shuffle_button"),
              ) {
                Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Zufall")
              }
            }
          }
        }
      }

      if (tracks.isEmpty()) {
        item(key = "empty_playlist_detail") {
          Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text(
              text = "Diese Playlist enthält noch keine Titel.",
              style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Füge Titel über das Drei-Punkte-Menü in der Bibliothek hinzu.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      } else {
        itemsIndexed(tracks, key = { idx, t -> "${t.uri}_$idx" }) { index, track ->
          TrackListItem(
            track = track,
            isCurrentTrack = track.uri == currentTrackUri,
            isPlaying = isPlaying,
            onClick = { onPlayQueue(tracks, index) },
            onToggleFavorite = { onToggleFavorite(track.uri) },
            onPlayNext = { onPlayNext(track) },
            onAddToQueue = { onAddToQueue(track) },
            onAddToPlaylist = { onAddToPlaylist(track) },
            onShowDetails = { onShowDetails(track) },
            extraMenuContent =
              if (isCustomPlaylist) {
                { dismiss ->
                  if (index > 0) {
                    DropdownMenuItem(
                      text = { Text("Nach oben") },
                      leadingIcon = { Icon(Icons.Filled.ArrowUpward, contentDescription = null) },
                      onClick = {
                        dismiss()
                        onMoveTrack(index, index - 1)
                      },
                    )
                  }
                  if (index < tracks.lastIndex) {
                    DropdownMenuItem(
                      text = { Text("Nach unten") },
                      leadingIcon = { Icon(Icons.Filled.ArrowDownward, contentDescription = null) },
                      onClick = {
                        dismiss()
                        onMoveTrack(index, index + 1)
                      },
                    )
                  }
                  DropdownMenuItem(
                    text = { Text("Aus Playlist entfernen") },
                    leadingIcon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null) },
                    onClick = {
                      dismiss()
                      onRemoveFromPlaylist(track.uri)
                    },
                  )
                }
              } else {
                null
              },
          )
        }
      }
    }
  }
}
