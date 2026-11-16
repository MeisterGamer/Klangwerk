package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.MediaSource
import com.example.data.model.MediaTrack
import com.example.data.model.formatDuration
import com.example.data.model.formatFileSize
import com.example.ui.components.TrackArtwork
import com.example.ui.theme.MonoTabularStyle

@Composable
fun VideosScreen(
  videos: List<MediaTrack>,
  onPickVideos: () -> Unit,
  onImportFolder: () -> Unit,
  onPlayVideo: (List<MediaTrack>, Int) -> Unit,
  onRemoveImported: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item(key = "videos_header") {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "Lokale Videos",
          style = MaterialTheme.typography.headlineMedium,
        )
        Text(
          text = "Vollbild-Wiedergabe inklusive Audio-Equalizer für deine lokalen Clips",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    item(key = "video_import_actions") {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Button(
          onClick = onPickVideos,
          modifier = Modifier.weight(1f).testTag("pick_videos_button"),
        ) {
          Icon(Icons.Filled.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Videos auswählen")
        }
        OutlinedButton(
          onClick = onImportFolder,
          modifier = Modifier.weight(1f).testTag("import_video_folder_button"),
        ) {
          Icon(
            Icons.Filled.CreateNewFolder,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Ordner")
        }
      }
    }

    if (videos.isEmpty()) {
      item(key = "empty_videos") {
        Card(
          colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Box(
              modifier =
                Modifier.size(64.dp)
                  .clip(RoundedCornerShape(18.dp))
                  .background(MaterialTheme.colorScheme.surfaceContainerHigh),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                Icons.Filled.VideoLibrary,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
              )
            }
            Text(text = "Noch keine Videos hinzugefügt", style = MaterialTheme.typography.titleMedium)
            Text(
              text =
                "Wähle einzelne Videodateien über den Android-Medienpicker oder verknüpfe einen Videoordner.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    } else {
      itemsIndexed(videos, key = { _, v -> v.uri }) { index, video ->
        Surface(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .clickable { onPlayVideo(videos, index) },
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          tonalElevation = 2.dp,
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(contentAlignment = Alignment.Center) {
              TrackArtwork(
                artworkUri = video.artworkUri,
                contentDescription = video.title,
                size = 72.dp,
                cornerRadius = 12.dp,
                isVideo = true,
              )
              Box(
                modifier =
                  Modifier.size(30.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  Icons.Filled.PlayArrow,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = video.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                  Text(
                    text = video.formatBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                  )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text =
                    "${formatDuration(video.durationMs)} • ${formatFileSize(video.sizeBytes)}",
                  style = MonoTabularStyle.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            if (video.source == MediaSource.IMPORTED) {
              IconButton(onClick = { onRemoveImported(video.uri) }) {
                Icon(
                  Icons.Filled.DeleteOutline,
                  contentDescription = "Video entfernen",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    }
  }
}
