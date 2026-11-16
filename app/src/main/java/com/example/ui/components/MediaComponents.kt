package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AppThemeSettings
import com.example.data.model.MediaTrack
import com.example.data.model.MediaType
import com.example.data.model.PlaylistSummary
import com.example.data.model.formatDuration
import com.example.data.model.formatFileSize
import com.example.ui.theme.MonoTabularStyle

@Composable
fun TrackArtwork(
  artworkUri: String?,
  contentDescription: String,
  modifier: Modifier = Modifier,
  size: Dp = 52.dp,
  cornerRadius: Dp = 10.dp,
  isVideo: Boolean = false,
) {
  Box(
    modifier =
      modifier
        .size(size)
        .clip(RoundedCornerShape(cornerRadius))
        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    contentAlignment = Alignment.Center,
  ) {
    Image(
      painter = painterResource(id = R.drawable.img_default_artwork),
      contentDescription = null,
      modifier = Modifier.fillMaxSize(),
      contentScale = ContentScale.Crop,
    )
    if (!artworkUri.isNullOrBlank()) {
      AsyncImage(
        model = artworkUri,
        contentDescription = contentDescription,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
      )
    }
    if (isVideo) {
      Box(
        modifier =
          Modifier.align(Alignment.BottomEnd)
            .padding(4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
            .padding(2.dp)
      ) {
        Icon(
          imageVector = Icons.Filled.VideoFile,
          contentDescription = "Video",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(14.dp),
        )
      }
    }
  }
}

@Composable
fun AnimatedPlayingBars(modifier: Modifier = Modifier) {
  val transition = rememberInfiniteTransition(label = "playing_bars")
  val b1 by
    transition.animateFloat(
      initialValue = 0.25f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
      label = "bar1",
    )
  val b2 by
    transition.animateFloat(
      initialValue = 0.9f,
      targetValue = 0.3f,
      animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
      label = "bar2",
    )
  val b3 by
    transition.animateFloat(
      initialValue = 0.4f,
      targetValue = 0.95f,
      animationSpec = infiniteRepeatable(tween(360), RepeatMode.Reverse),
      label = "bar3",
    )

  Row(
    modifier = modifier.height(16.dp),
    horizontalArrangement = Arrangement.spacedBy(2.dp),
    verticalAlignment = Alignment.Bottom,
  ) {
    listOf(b1, b2, b3).forEach { fraction ->
      Box(
        modifier =
          Modifier.width(3.dp)
            .height((16.dp * fraction).coerceAtLeast(4.dp))
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.primary)
      )
    }
  }
}

@Composable
fun TrackListItem(
  track: MediaTrack,
  isCurrentTrack: Boolean,
  isPlaying: Boolean,
  onClick: () -> Unit,
  onToggleFavorite: () -> Unit,
  onPlayNext: () -> Unit,
  onAddToQueue: () -> Unit,
  onAddToPlaylist: () -> Unit,
  onShowDetails: () -> Unit,
  modifier: Modifier = Modifier,
  extraMenuContent: (@Composable (dismissMenu: () -> Unit) -> Unit)? = null,
) {
  var menuExpanded by remember { mutableStateOf(false) }

  Surface(
    modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
    color =
      if (isCurrentTrack) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f)
      } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0f)
      },
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(contentAlignment = Alignment.Center) {
        TrackArtwork(
          artworkUri = track.artworkUri,
          contentDescription = "Cover von ${track.title}",
          isVideo = track.mediaType == MediaType.VIDEO,
        )
        if (isCurrentTrack && isPlaying) {
          Box(
            modifier =
              Modifier.size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
          ) {
            AnimatedPlayingBars()
          }
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = track.title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = if (isCurrentTrack) FontWeight.Bold else FontWeight.Medium,
          color =
            if (isCurrentTrack) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
          ) {
            Text(
              text = track.formatBadge,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.tertiary,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "${track.artist} • ${track.album}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      Text(
        text = formatDuration(track.durationMs),
        style = MonoTabularStyle.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      IconButton(
        onClick = onToggleFavorite,
        modifier = Modifier.testTag("favorite_track_button"),
      ) {
        Icon(
          imageVector =
            if (track.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
          contentDescription =
            if (track.isFavorite) "Aus Favoriten entfernen" else "Zu Favoriten hinzufügen",
          tint =
            if (track.isFavorite) MaterialTheme.colorScheme.tertiary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Box {
        IconButton(
          onClick = { menuExpanded = true },
          modifier = Modifier.testTag("track_options_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = "Titeloptionen",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
          DropdownMenuItem(
            text = { Text("Als Nächstes abspielen") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onPlayNext()
            },
          )
          DropdownMenuItem(
            text = { Text("Zur Warteschlange hinzufügen") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onAddToQueue()
            },
          )
          DropdownMenuItem(
            text = { Text("Zur Playlist hinzufügen") },
            leadingIcon = { Icon(Icons.Filled.Album, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onAddToPlaylist()
            },
          )
          DropdownMenuItem(
            text = { Text("Dateidetails") },
            leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onShowDetails()
            },
          )
          extraMenuContent?.invoke { menuExpanded = false }
        }
      }
    }
  }
}

@Composable
fun MiniPlayerBar(
  track: MediaTrack,
  isPlaying: Boolean,
  progressFraction: Float,
  onClick: () -> Unit,
  onTogglePlayPause: () -> Unit,
  onSkipNext: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 6.dp)
        .clip(RoundedCornerShape(16.dp))
        .clickable(onClick = onClick)
        .testTag("mini_player_bar"),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tonalElevation = 6.dp,
    shadowElevation = 4.dp,
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TrackArtwork(
          artworkUri = track.artworkUri,
          contentDescription = "Aktuelles Cover",
          size = 44.dp,
          cornerRadius = 8.dp,
          isVideo = track.mediaType == MediaType.VIDEO,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = track.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${track.artist} • ${track.formatBadge}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        IconButton(
          onClick = onTogglePlayPause,
          modifier = Modifier.testTag("mini_player_play_pause"),
        ) {
          Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Wiedergabe",
            tint = MaterialTheme.colorScheme.primary,
          )
        }
        IconButton(
          onClick = onSkipNext,
          modifier = Modifier.testTag("mini_player_next"),
        ) {
          Icon(
            imageVector = Icons.Filled.SkipNext,
            contentDescription = "Nächster Titel",
            tint = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
      LinearProgressIndicator(
        progress = { progressFraction.coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth().height(2.5.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistBottomSheet(
  track: MediaTrack,
  playlists: List<PlaylistSummary>,
  onDismiss: () -> Unit,
  onSelectPlaylist: (PlaylistSummary) -> Unit,
  onCreateNewPlaylist: (String) -> Unit,
) {
  var showCreateInput by remember { mutableStateOf(false) }
  var newName by remember { mutableStateOf("") }

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
      Text(
        text = "Zur Playlist hinzufügen",
        style = MaterialTheme.typography.titleLarge,
      )
      Text(
        text = track.title,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(12.dp))

      if (showCreateInput) {
        OutlinedTextField(
          value = newName,
          onValueChange = { newName = it },
          label = { Text("Name der Wiedergabeliste") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("quick_playlist_name_input"),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = { showCreateInput = false }) { Text("Abbrechen") }
          TextButton(
            onClick = {
              if (newName.isNotBlank()) {
                onCreateNewPlaylist(newName.trim())
                newName = ""
                showCreateInput = false
              }
            },
            modifier = Modifier.testTag("quick_playlist_save_button"),
          ) {
            Text("Erstellen")
          }
        }
      } else {
        ListItem(
          headlineContent = { Text("Neue Playlist erstellen") },
          leadingContent = {
            Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          },
          modifier = Modifier.clickable { showCreateInput = true }.testTag("new_playlist_in_sheet"),
        )
      }

      HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

      if (playlists.isEmpty()) {
        Text(
          text = "Noch keine eigenen Wiedergabelisten vorhanden.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(vertical = 16.dp),
        )
      } else {
        LazyColumn(modifier = Modifier.fillMaxWidth().height(240.dp)) {
          items(playlists, key = { it.id }) { pl ->
            ListItem(
              headlineContent = { Text(pl.name) },
              supportingContent = { Text("${pl.tracks.size} Titel") },
              leadingContent = {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null)
              },
              modifier = Modifier.clickable { onSelectPlaylist(pl) },
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
fun TrackDetailsDialog(track: MediaTrack, onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Dateidetails") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DetailRow("Titel", track.title)
        DetailRow("Interpret", track.artist)
        DetailRow("Album", track.album)
        DetailRow("Format", "${track.formatBadge} (${track.mimeType})")
        DetailRow("Dauer", formatDuration(track.durationMs))
        DetailRow("Größe", formatFileSize(track.sizeBytes))
        DetailRow("Ordner", track.folderName)
      }
    },
    confirmButton = { TextButton(onClick = onDismiss) { Text("Schließen") } },
  )
}

@Composable
private fun DetailRow(label: String, value: String) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.width(12.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
  themeSettings: AppThemeSettings,
  onDynamicColorChanged: (Boolean) -> Unit,
  onAmoledBlackChanged: (Boolean) -> Unit,
  onRescanLibrary: () -> Unit,
  onImportFiles: () -> Unit,
  onImportFolder: () -> Unit,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = "Darstellung & Bibliothek",
        style = MaterialTheme.typography.titleLarge,
      )
      ListItem(
        headlineContent = { Text("Material You Dynamic Color") },
        supportingContent = { Text("Passt Akzentfarben ab Android 12 an das System an") },
        trailingContent = {
          Switch(
            checked = themeSettings.dynamicColor,
            onCheckedChange = onDynamicColorChanged,
            modifier = Modifier.testTag("switch_dynamic_color"),
          )
        },
      )
      ListItem(
        headlineContent = { Text("Reines AMOLED-Schwarz") },
        supportingContent = { Text("Tiefschwarze Flächen zur maximalen Akkuschonung") },
        trailingContent = {
          Switch(
            checked = themeSettings.amoledBlack,
            onCheckedChange = onAmoledBlackChanged,
            modifier = Modifier.testTag("switch_amoled_black"),
          )
        },
      )
      HorizontalDivider()
      ListItem(
        headlineContent = { Text("Medienbibliothek neu scannen") },
        supportingContent = { Text("Lokale Audiodateien via MediaStore aktualisieren") },
        modifier =
          Modifier.clickable {
            onDismiss()
            onRescanLibrary()
          },
      )
      ListItem(
        headlineContent = { Text("Audio- oder Videodateien importieren") },
        supportingContent = { Text("Einzelne oder mehrere Dateien manuell hinzufügen") },
        modifier =
          Modifier.clickable {
            onDismiss()
            onImportFiles()
          },
      )
      ListItem(
        headlineContent = { Text("Musik- oder Videoordner verknüpfen") },
        supportingContent = { Text("Ganzen Ordner dauerhaft einbinden") },
        modifier =
          Modifier.clickable {
            onDismiss()
            onImportFolder()
          },
      )
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
