package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AlbumGroup
import com.example.data.model.ArtistGroup
import com.example.data.model.FolderGroup
import com.example.data.model.LibraryFilter
import com.example.data.model.MediaTrack
import com.example.data.model.PlaylistSummary
import com.example.data.model.SortOrder
import com.example.data.model.formatDuration
import com.example.ui.components.TrackArtwork
import com.example.ui.components.TrackListItem
import com.example.ui.viewmodel.LibraryUiState

@Composable
fun LibraryScreen(
  uiState: LibraryUiState,
  searchQuery: String,
  selectedFilter: LibraryFilter,
  sortOrder: SortOrder,
  hasAudioPermission: Boolean,
  currentTrackUri: String?,
  isPlaying: Boolean,
  onSearchQueryChange: (String) -> Unit,
  onSelectFilter: (LibraryFilter) -> Unit,
  onSelectSortOrder: (SortOrder) -> Unit,
  onRequestAudioPermission: () -> Unit,
  onRescanLibrary: () -> Unit,
  onImportFiles: () -> Unit,
  onImportFolder: () -> Unit,
  onOpenSettings: () -> Unit,
  onPlayQueue: (List<MediaTrack>, Int) -> Unit,
  onPlayVideo: (MediaTrack) -> Unit,
  onOpenPlaylist: (PlaylistSummary) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onPlayNext: (MediaTrack) -> Unit,
  onAddToQueue: (MediaTrack) -> Unit,
  onAddToPlaylist: (MediaTrack) -> Unit,
  onShowDetails: (MediaTrack) -> Unit,
  onRemoveImported: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var sortMenuExpanded by remember { mutableStateOf(false) }
  var expandedAlbum by remember { mutableStateOf<String?>(null) }
  var expandedArtist by remember { mutableStateOf<String?>(null) }
  var expandedFolder by remember { mutableStateOf<String?>(null) }

  Column(modifier = modifier.fillMaxSize()) {
    // Top Header + Search Bar
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column {
          Text(
            text = "Klangwerk",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text =
              "${uiState.totalAudioCount} Titel • ${uiState.totalVideoCount} Videos",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onRescanLibrary,
            modifier = Modifier.testTag("rescan_library_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Refresh,
              contentDescription = "Medien neu scannen",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.testTag("open_settings_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Settings,
              contentDescription = "Darstellung und Bibliothekseinstellungen",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = Modifier.fillMaxWidth().testTag("library_search_input"),
        placeholder = {
          Text(
            text = "Titel, Alben, Interpreten, Playlists suchen…",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        },
        leadingIcon = {
          Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = "Suche",
            tint = MaterialTheme.colorScheme.primary,
          )
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(
              onClick = { onSearchQueryChange("") },
              modifier = Modifier.testTag("clear_search_button"),
            ) {
              Icon(Icons.Filled.Clear, contentDescription = "Suche löschen")
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
          ),
      )

      // Filter Chips & Sort Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          LibraryFilter.entries.forEach { filter ->
            FilterChip(
              selected = selectedFilter == filter,
              onClick = { onSelectFilter(filter) },
              label = { Text(filter.labelRes) },
              modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}"),
            )
          }
        }

        Box {
          IconButton(
            onClick = { sortMenuExpanded = true },
            modifier = Modifier.testTag("sort_menu_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Sort,
              contentDescription = "Sortierung wählen",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
            SortOrder.entries.forEach { order ->
              DropdownMenuItem(
                text = {
                  Text(
                    text = order.label,
                    fontWeight = if (order == sortOrder) FontWeight.Bold else FontWeight.Normal,
                    color =
                      if (order == sortOrder) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurface,
                  )
                },
                onClick = {
                  sortMenuExpanded = false
                  onSelectSortOrder(order)
                },
              )
            }
          }
        }
      }
    }

    if (uiState.isScanning) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(bottom = 104.dp),
    ) {
      // Permission Prompt Banner if not granted
      if (!hasAudioPermission) {
        item(key = "permission_banner") {
          PermissionDiscoveryCard(
            onGrantPermission = onRequestAudioPermission,
            onImportFiles = onImportFiles,
            onImportFolder = onImportFolder,
          )
        }
      }

      // Quick Action Bar when tracks exist
      if (uiState.filteredAudioTracks.isNotEmpty() && selectedFilter == LibraryFilter.ALL_TRACKS) {
        item(key = "quick_play_bar") {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Button(
              onClick = { onPlayQueue(uiState.filteredAudioTracks, 0) },
              modifier = Modifier.weight(1f).testTag("play_all_library_button"),
            ) {
              Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Alle abspielen")
            }
            OutlinedButton(
              onClick = {
                val shuffled = uiState.filteredAudioTracks.shuffled()
                onPlayQueue(shuffled, 0)
              },
              modifier = Modifier.weight(1f).testTag("shuffle_library_button"),
            ) {
              Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Zufallswiedergabe")
            }
          }
        }
      }

      // Global Search Cross-Results (Playlists & Videos) when searchQuery is active
      if (searchQuery.isNotBlank()) {
        if (uiState.matchingPlaylists.isNotEmpty()) {
          item(key = "search_playlists_header") {
            SectionHeader("Gefundene Playlists (${uiState.matchingPlaylists.size})")
          }
          item(key = "search_playlists_row") {
            LazyRow(
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              items(uiState.matchingPlaylists, key = { it.id }) { pl ->
                ElevatedCard(
                  modifier = Modifier.clickable { onOpenPlaylist(pl) },
                  colors =
                    CardDefaults.elevatedCardColors(
                      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Icon(
                      Icons.AutoMirrored.Filled.QueueMusic,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                      Text(pl.name, style = MaterialTheme.typography.titleSmall)
                      Text(
                        "${pl.tracks.size} Titel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                }
              }
            }
          }
        }

        if (uiState.matchingVideos.isNotEmpty()) {
          item(key = "search_videos_header") {
            SectionHeader("Gefundene Videos (${uiState.matchingVideos.size})")
          }
          items(uiState.matchingVideos, key = { "vid_${it.uri}" }) { video ->
            TrackListItem(
              track = video,
              isCurrentTrack = video.uri == currentTrackUri,
              isPlaying = isPlaying,
              onClick = { onPlayVideo(video) },
              onToggleFavorite = { onToggleFavorite(video.uri) },
              onPlayNext = { onPlayNext(video) },
              onAddToQueue = { onAddToQueue(video) },
              onAddToPlaylist = { onAddToPlaylist(video) },
              onShowDetails = { onShowDetails(video) },
            )
          }
        }
      }

      // Main Content by Filter
      when (selectedFilter) {
        LibraryFilter.ALL_TRACKS -> {
          if (uiState.filteredAudioTracks.isEmpty()) {
            item(key = "empty_tracks") {
              EmptyLibraryState(
                searchActive = searchQuery.isNotBlank(),
                onImportFiles = onImportFiles,
                onImportFolder = onImportFolder,
              )
            }
          } else {
            itemsIndexed(
              items = uiState.filteredAudioTracks,
              key = { _, track -> track.uri },
            ) { index, track ->
              TrackListItem(
                track = track,
                isCurrentTrack = track.uri == currentTrackUri,
                isPlaying = isPlaying,
                onClick = { onPlayQueue(uiState.filteredAudioTracks, index) },
                onToggleFavorite = { onToggleFavorite(track.uri) },
                onPlayNext = { onPlayNext(track) },
                onAddToQueue = { onAddToQueue(track) },
                onAddToPlaylist = { onAddToPlaylist(track) },
                onShowDetails = { onShowDetails(track) },
              )
            }
          }
        }

        LibraryFilter.ALBUMS -> {
          if (uiState.albums.isEmpty()) {
            item(key = "empty_albums") {
              EmptyLibraryState(
                searchActive = searchQuery.isNotBlank(),
                onImportFiles = onImportFiles,
                onImportFolder = onImportFolder,
              )
            }
          } else {
            items(uiState.albums, key = { "album_${it.name}" }) { album ->
              AlbumGroupCard(
                album = album,
                expanded = expandedAlbum == album.name,
                currentTrackUri = currentTrackUri,
                isPlaying = isPlaying,
                onToggleExpand = {
                  expandedAlbum = if (expandedAlbum == album.name) null else album.name
                },
                onPlayAlbum = { onPlayQueue(album.tracks, 0) },
                onPlayTrack = { idx -> onPlayQueue(album.tracks, idx) },
                onToggleFavorite = onToggleFavorite,
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onAddToPlaylist = onAddToPlaylist,
                onShowDetails = onShowDetails,
              )
            }
          }
        }

        LibraryFilter.ARTISTS -> {
          if (uiState.artists.isEmpty()) {
            item(key = "empty_artists") {
              EmptyLibraryState(
                searchActive = searchQuery.isNotBlank(),
                onImportFiles = onImportFiles,
                onImportFolder = onImportFolder,
              )
            }
          } else {
            items(uiState.artists, key = { "artist_${it.name}" }) { artist ->
              ArtistGroupCard(
                artist = artist,
                expanded = expandedArtist == artist.name,
                currentTrackUri = currentTrackUri,
                isPlaying = isPlaying,
                onToggleExpand = {
                  expandedArtist = if (expandedArtist == artist.name) null else artist.name
                },
                onPlayArtist = { onPlayQueue(artist.tracks, 0) },
                onPlayTrack = { idx -> onPlayQueue(artist.tracks, idx) },
                onToggleFavorite = onToggleFavorite,
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onAddToPlaylist = onAddToPlaylist,
                onShowDetails = onShowDetails,
              )
            }
          }
        }

        LibraryFilter.FOLDERS -> {
          item(key = "folders_toolbar") {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OutlinedButton(
                onClick = onImportFolder,
                modifier = Modifier.fillMaxWidth().testTag("folders_link_folder_button"),
              ) {
                Icon(
                  Icons.Filled.CreateNewFolder,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Weiteren Ordner verknüpfen")
              }
            }
          }
          if (uiState.folders.isEmpty()) {
            item(key = "empty_folders") {
              EmptyLibraryState(
                searchActive = searchQuery.isNotBlank(),
                onImportFiles = onImportFiles,
                onImportFolder = onImportFolder,
              )
            }
          } else {
            items(uiState.folders, key = { "folder_${it.name}" }) { folder ->
              FolderGroupCard(
                folder = folder,
                expanded = expandedFolder == folder.name,
                currentTrackUri = currentTrackUri,
                isPlaying = isPlaying,
                onToggleExpand = {
                  expandedFolder = if (expandedFolder == folder.name) null else folder.name
                },
                onPlayFolder = { onPlayQueue(folder.tracks, 0) },
                onPlayTrack = { idx -> onPlayQueue(folder.tracks, idx) },
                onToggleFavorite = onToggleFavorite,
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onAddToPlaylist = onAddToPlaylist,
                onShowDetails = onShowDetails,
              )
            }
          }
        }

        LibraryFilter.IMPORTED -> {
          item(key = "import_toolbar") {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Button(
                onClick = onImportFiles,
                modifier = Modifier.weight(1f).testTag("import_files_button"),
              ) {
                Icon(Icons.Filled.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Dateien")
              }
              OutlinedButton(
                onClick = onImportFolder,
                modifier = Modifier.weight(1f).testTag("import_folder_button"),
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
          if (uiState.importedTracks.isEmpty()) {
            item(key = "empty_imported") {
              EmptyLibraryState(
                searchActive = searchQuery.isNotBlank(),
                onImportFiles = onImportFiles,
                onImportFolder = onImportFolder,
              )
            }
          } else {
            itemsIndexed(
              items = uiState.importedTracks,
              key = { _, track -> "imp_${track.uri}" },
            ) { index, track ->
              TrackListItem(
                track = track,
                isCurrentTrack = track.uri == currentTrackUri,
                isPlaying = isPlaying,
                onClick = { onPlayQueue(uiState.importedTracks, index) },
                onToggleFavorite = { onToggleFavorite(track.uri) },
                onPlayNext = { onPlayNext(track) },
                onAddToQueue = { onAddToQueue(track) },
                onAddToPlaylist = { onAddToPlaylist(track) },
                onShowDetails = { onShowDetails(track) },
                extraMenuContent = { dismiss ->
                  DropdownMenuItem(
                    text = { Text("Aus Importen entfernen") },
                    leadingIcon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null) },
                    onClick = {
                      dismiss()
                      onRemoveImported(track.uri)
                    },
                  )
                },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SectionHeader(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
  )
}

@Composable
private fun PermissionDiscoveryCard(
  onGrantPermission: () -> Unit,
  onImportFiles: () -> Unit,
  onImportFolder: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    shape = RoundedCornerShape(20.dp),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Filled.LibraryMusic,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Lokale Musik entdecken",
          style = MaterialTheme.typography.titleMedium,
        )
      }
      Text(
        text =
          "Erlaube den Zugriff auf deine Audiodateien, um MP3, FLAC, AAC, OGG und WAV automatisch zu finden, oder importiere direkt Dateien und Ordner.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
          onClick = onGrantPermission,
          modifier = Modifier.testTag("grant_audio_permission_button"),
        ) {
          Text("Medien scannen")
        }
        OutlinedButton(onClick = onImportFiles) { Text("Dateien wählen") }
        TextButton(onClick = onImportFolder) { Text("Ordner") }
      }
    }
  }
}

@Composable
private fun EmptyLibraryState(
  searchActive: Boolean,
  onImportFiles: () -> Unit,
  onImportFolder: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(
      modifier =
        Modifier.size(72.dp)
          .clip(RoundedCornerShape(20.dp))
          .background(MaterialTheme.colorScheme.surfaceContainerHigh),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = if (searchActive) Icons.Filled.Search else Icons.Filled.LibraryMusic,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(34.dp),
      )
    }
    Text(
      text = if (searchActive) "Keine Treffer gefunden" else "Noch keine Audiodateien geladen",
      style = MaterialTheme.typography.titleMedium,
    )
    Text(
      text =
        if (searchActive) {
          "Prüfe deinen Suchbegriff oder wähle einen anderen Filter."
        } else {
          "Importiere einzelne Musikdateien oder verknüpfe einen lokalen Ordner, um direkt loszuhören."
        },
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (!searchActive) {
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = onImportFiles, modifier = Modifier.testTag("empty_import_files_button")) {
          Icon(Icons.Filled.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Dateien importieren")
        }
        OutlinedButton(
          onClick = onImportFolder,
          modifier = Modifier.testTag("empty_import_folder_button"),
        ) {
          Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Ordner verknüpfen")
        }
      }
    }
  }
}

@Composable
private fun AlbumGroupCard(
  album: AlbumGroup,
  expanded: Boolean,
  currentTrackUri: String?,
  isPlaying: Boolean,
  onToggleExpand: () -> Unit,
  onPlayAlbum: () -> Unit,
  onPlayTrack: (Int) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onPlayNext: (MediaTrack) -> Unit,
  onAddToQueue: (MediaTrack) -> Unit,
  onAddToPlaylist: (MediaTrack) -> Unit,
  onShowDetails: (MediaTrack) -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    tonalElevation = 2.dp,
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggleExpand).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TrackArtwork(
          artworkUri = album.artworkUri,
          contentDescription = "Album ${album.name}",
          size = 56.dp,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = album.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text =
              "${album.artist} • ${album.tracks.size} Titel • ${formatDuration(album.totalDurationMs)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        IconButton(onClick = onPlayAlbum) {
          Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "Album abspielen",
            tint = MaterialTheme.colorScheme.primary,
          )
        }
        Icon(
          imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
          contentDescription = if (expanded) "Einklappen" else "Ausklappen",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      AnimatedVisibility(visible = expanded) {
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
          album.tracks.forEachIndexed { idx, track ->
            TrackListItem(
              track = track,
              isCurrentTrack = track.uri == currentTrackUri,
              isPlaying = isPlaying,
              onClick = { onPlayTrack(idx) },
              onToggleFavorite = { onToggleFavorite(track.uri) },
              onPlayNext = { onPlayNext(track) },
              onAddToQueue = { onAddToQueue(track) },
              onAddToPlaylist = { onAddToPlaylist(track) },
              onShowDetails = { onShowDetails(track) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ArtistGroupCard(
  artist: ArtistGroup,
  expanded: Boolean,
  currentTrackUri: String?,
  isPlaying: Boolean,
  onToggleExpand: () -> Unit,
  onPlayArtist: () -> Unit,
  onPlayTrack: (Int) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onPlayNext: (MediaTrack) -> Unit,
  onAddToQueue: (MediaTrack) -> Unit,
  onAddToPlaylist: (MediaTrack) -> Unit,
  onShowDetails: (MediaTrack) -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    tonalElevation = 2.dp,
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggleExpand).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier.size(48.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = artist.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${artist.albumCount} Alben • ${artist.tracks.size} Titel",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(onClick = onPlayArtist) {
          Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "Interpret abspielen",
            tint = MaterialTheme.colorScheme.primary,
          )
        }
        Icon(
          imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
          contentDescription = if (expanded) "Einklappen" else "Ausklappen",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      AnimatedVisibility(visible = expanded) {
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
          artist.tracks.forEachIndexed { idx, track ->
            TrackListItem(
              track = track,
              isCurrentTrack = track.uri == currentTrackUri,
              isPlaying = isPlaying,
              onClick = { onPlayTrack(idx) },
              onToggleFavorite = { onToggleFavorite(track.uri) },
              onPlayNext = { onPlayNext(track) },
              onAddToQueue = { onAddToQueue(track) },
              onAddToPlaylist = { onAddToPlaylist(track) },
              onShowDetails = { onShowDetails(track) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FolderGroupCard(
  folder: FolderGroup,
  expanded: Boolean,
  currentTrackUri: String?,
  isPlaying: Boolean,
  onToggleExpand: () -> Unit,
  onPlayFolder: () -> Unit,
  onPlayTrack: (Int) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onPlayNext: (MediaTrack) -> Unit,
  onAddToQueue: (MediaTrack) -> Unit,
  onAddToPlaylist: (MediaTrack) -> Unit,
  onShowDetails: (MediaTrack) -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    tonalElevation = 2.dp,
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggleExpand).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier.size(48.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.secondaryContainer),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.FolderOpen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = folder.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${folder.tracks.size} Titel • ${formatDuration(folder.totalDurationMs)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(onClick = onPlayFolder) {
          Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "Ordner abspielen",
            tint = MaterialTheme.colorScheme.primary,
          )
        }
        Icon(
          imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
          contentDescription = if (expanded) "Einklappen" else "Ausklappen",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      AnimatedVisibility(visible = expanded) {
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
          folder.tracks.forEachIndexed { idx, track ->
            TrackListItem(
              track = track,
              isCurrentTrack = track.uri == currentTrackUri,
              isPlaying = isPlaying,
              onClick = { onPlayTrack(idx) },
              onToggleFavorite = { onToggleFavorite(track.uri) },
              onPlayNext = { onPlayNext(track) },
              onAddToQueue = { onAddToQueue(track) },
              onAddToPlaylist = { onAddToPlaylist(track) },
              onShowDetails = { onShowDetails(track) },
            )
          }
        }
      }
    }
  }
}
