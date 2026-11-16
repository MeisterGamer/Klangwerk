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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.MediaTrack
import com.example.data.model.PlaylistSummary
import com.example.data.model.formatDuration
import com.example.ui.components.TrackArtwork

@Composable
fun PlaylistsScreen(
  playlists: List<PlaylistSummary>,
  favoriteTracks: List<MediaTrack>,
  recentTracks: List<MediaTrack>,
  onOpenSystemPlaylist: (String) -> Unit,
  onOpenPlaylist: (Long) -> Unit,
  onPlayTracks: (List<MediaTrack>) -> Unit,
  onCreatePlaylist: (String) -> Unit,
  onRenamePlaylist: (Long, String) -> Unit,
  onDeletePlaylist: (Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showCreateDialog by remember { mutableStateOf(false) }
  var renamingPlaylist by remember { mutableStateOf<PlaylistSummary?>(null) }
  var deletingPlaylist by remember { mutableStateOf<PlaylistSummary?>(null) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = { showCreateDialog = true },
        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
        text = { Text("Neue Playlist") },
        modifier = Modifier.padding(bottom = 68.dp).testTag("create_playlist_fab"),
      )
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item(key = "playlists_header") {
        Column {
          Text(
            text = "Wiedergabelisten",
            style = MaterialTheme.typography.headlineMedium,
          )
          Text(
            text = "Organisiere deine Lieblingstitel und Mixe lokal auf dem Gerät",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      item(key = "system_playlists_row") {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          SystemPlaylistCard(
            title = "Favoriten",
            subtitle = "${favoriteTracks.size} Titel",
            icon = {
              Icon(
                Icons.Filled.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
              )
            },
            onClick = { onOpenSystemPlaylist("favorites") },
            modifier = Modifier.weight(1f).testTag("system_playlist_favorites"),
          )
          SystemPlaylistCard(
            title = "Zuletzt gespielt",
            subtitle = "${recentTracks.size} Titel",
            icon = {
              Icon(
                Icons.Filled.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
            },
            onClick = { onOpenSystemPlaylist("recent") },
            modifier = Modifier.weight(1f).testTag("system_playlist_recent"),
          )
        }
      }

      item(key = "custom_playlists_title") {
        Text(
          text = "Eigene Playlists (${playlists.size})",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(top = 8.dp),
        )
      }

      if (playlists.isEmpty()) {
        item(key = "empty_playlists") {
          Card(
            colors =
              CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
              )
              Text(
                text = "Noch keine eigenen Wiedergabelisten",
                style = MaterialTheme.typography.titleMedium,
              )
              Text(
                text =
                  "Erstelle Playlists für Fokus, Training oder lange Fahrten und füge Titel direkt aus der Bibliothek hinzu.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Button(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Erste Playlist anlegen")
              }
            }
          }
        }
      } else {
        items(playlists, key = { it.id }) { playlist ->
          CustomPlaylistRow(
            playlist = playlist,
            onClick = { onOpenPlaylist(playlist.id) },
            onPlayAll = { if (playlist.tracks.isNotEmpty()) onPlayTracks(playlist.tracks) },
            onRename = { renamingPlaylist = playlist },
            onDelete = { deletingPlaylist = playlist },
          )
        }
      }
    }
  }

  if (showCreateDialog) {
    PlaylistNameDialog(
      title = "Neue Playlist erstellen",
      initialName = "",
      confirmLabel = "Erstellen",
      onDismiss = { showCreateDialog = false },
      onConfirm = { name ->
        onCreatePlaylist(name)
        showCreateDialog = false
      },
    )
  }

  renamingPlaylist?.let { pl ->
    PlaylistNameDialog(
      title = "Playlist umbenennen",
      initialName = pl.name,
      confirmLabel = "Speichern",
      onDismiss = { renamingPlaylist = null },
      onConfirm = { newName ->
        onRenamePlaylist(pl.id, newName)
        renamingPlaylist = null
      },
    )
  }

  deletingPlaylist?.let { pl ->
    AlertDialog(
      onDismissRequest = { deletingPlaylist = null },
      title = { Text("Playlist löschen?") },
      text = { Text("Möchtest du „${pl.name}“ wirklich löschen? Die Audiodateien bleiben auf dem Gerät erhalten.") },
      confirmButton = {
        TextButton(
          onClick = {
            onDeletePlaylist(pl.id)
            deletingPlaylist = null
          }
        ) {
          Text("Löschen", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingPlaylist = null }) { Text("Abbrechen") }
      },
    )
  }
}

@Composable
private fun SystemPlaylistCard(
  title: String,
  subtitle: String,
  icon: @Composable () -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    shape = RoundedCornerShape(18.dp),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Box(
        modifier =
          Modifier.size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center,
      ) {
        icon()
      }
      Text(text = title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun CustomPlaylistRow(
  playlist: PlaylistSummary,
  onClick: () -> Unit,
  onPlayAll: () -> Unit,
  onRename: () -> Unit,
  onDelete: () -> Unit,
) {
  var menuOpen by remember { mutableStateOf(false) }

  Surface(
    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    tonalElevation = 2.dp,
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      TrackArtwork(
        artworkUri = playlist.tracks.firstOrNull { it.artworkUri != null }?.artworkUri,
        contentDescription = "Playlist ${playlist.name}",
        size = 56.dp,
        cornerRadius = 12.dp,
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = playlist.name,
          style = MaterialTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${playlist.tracks.size} Titel • ${formatDuration(playlist.totalDurationMs)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (playlist.tracks.isNotEmpty()) {
        IconButton(onClick = onPlayAll) {
          Icon(
            Icons.Filled.PlayArrow,
            contentDescription = "Playlist abspielen",
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }
      Box {
        IconButton(onClick = { menuOpen = true }) {
          Icon(Icons.Filled.MoreVert, contentDescription = "Playlist-Optionen")
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
          DropdownMenuItem(
            text = { Text("Umbenennen") },
            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
            onClick = {
              menuOpen = false
              onRename()
            },
          )
          DropdownMenuItem(
            text = { Text("Löschen") },
            leadingIcon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null) },
            onClick = {
              menuOpen = false
              onDelete()
            },
          )
        }
      }
    }
  }
}

@Composable
private fun PlaylistNameDialog(
  title: String,
  initialName: String,
  confirmLabel: String,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var name by remember { mutableStateOf(initialName) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Name der Wiedergabeliste") },
        placeholder = { Text("z. B. Fokus, Nachtfahrt, Workout") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("playlist_name_dialog_input"),
      )
    },
    confirmButton = {
      TextButton(
        onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
        enabled = name.isNotBlank(),
        modifier = Modifier.testTag("playlist_name_dialog_confirm"),
      ) {
        Text(confirmLabel)
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
  )
}
