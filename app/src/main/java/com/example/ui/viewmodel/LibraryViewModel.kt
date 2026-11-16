package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AlbumGroup
import com.example.data.model.AppThemeSettings
import com.example.data.model.ArtistGroup
import com.example.data.model.LibraryFilter
import com.example.data.model.MediaSource
import com.example.data.model.MediaTrack
import com.example.data.model.PlaylistSummary
import com.example.data.model.SortOrder
import com.example.data.preferences.SettingsDataStore
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
  val filteredAudioTracks: List<MediaTrack> = emptyList(),
  val albums: List<AlbumGroup> = emptyList(),
  val artists: List<ArtistGroup> = emptyList(),
  val importedTracks: List<MediaTrack> = emptyList(),
  val matchingVideos: List<MediaTrack> = emptyList(),
  val matchingPlaylists: List<PlaylistSummary> = emptyList(),
  val totalAudioCount: Int = 0,
  val totalVideoCount: Int = 0,
  val isScanning: Boolean = false,
)

class LibraryViewModel(
  private val repository: MediaRepository,
  private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _selectedFilter = MutableStateFlow(LibraryFilter.ALL_TRACKS)
  val selectedFilter: StateFlow<LibraryFilter> = _selectedFilter.asStateFlow()

  private val _sortOrder = MutableStateFlow(SortOrder.TITLE)
  val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

  private val _hasAudioPermission = MutableStateFlow(repository.hasAudioPermission())
  val hasAudioPermission: StateFlow<Boolean> = _hasAudioPermission.asStateFlow()

  private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
  val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

  val playlists: StateFlow<List<PlaylistSummary>> =
    repository.playlists.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList(),
    )

  val favoriteTracks: StateFlow<List<MediaTrack>> =
    repository.favoriteTracks.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList(),
    )

  val recentTracks: StateFlow<List<MediaTrack>> =
    repository.recentTracks.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList(),
    )

  val videoTracks: StateFlow<List<MediaTrack>> =
    repository.videoTracks.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList(),
    )

  val themeSettings: StateFlow<AppThemeSettings> =
    settingsDataStore.themeSettings.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = AppThemeSettings(),
    )

  val libraryUiState: StateFlow<LibraryUiState> =
    combine(
        repository.audioTracks,
        repository.videoTracks,
        repository.playlists,
        repository.isScanning,
        combine(_searchQuery, _selectedFilter, _sortOrder) { q, f, s -> Triple(q, f, s) },
      ) { audioList, videoList, allPlaylists, scanning, filterTriple ->
        val (query, _, sort) = filterTriple
        val trimmed = query.trim()

        val sortedAudio =
          when (sort) {
            SortOrder.TITLE -> audioList.sortedBy { it.title.lowercase() }
            SortOrder.ARTIST -> audioList.sortedBy { it.artist.lowercase() }
            SortOrder.DURATION -> audioList.sortedByDescending { it.durationMs }
            SortOrder.RECENT -> audioList.sortedByDescending { it.dateAddedSec }
          }

        val matchingAudio =
          if (trimmed.isEmpty()) {
            sortedAudio
          } else {
            sortedAudio.filter {
              it.title.contains(trimmed, ignoreCase = true) ||
                it.artist.contains(trimmed, ignoreCase = true) ||
                it.album.contains(trimmed, ignoreCase = true) ||
                it.formatBadge.contains(trimmed, ignoreCase = true)
            }
          }

        val albums =
          matchingAudio
            .groupBy { it.album }
            .map { (albumName, tracks) ->
              AlbumGroup(
                name = albumName,
                artist = tracks.firstOrNull()?.artist ?: "Unbekannt",
                artworkUri = tracks.firstOrNull { it.artworkUri != null }?.artworkUri,
                tracks = tracks,
              )
            }
            .sortedBy { it.name.lowercase() }

        val artists =
          matchingAudio
            .groupBy { it.artist }
            .map { (artistName, tracks) ->
              ArtistGroup(
                name = artistName,
                albumCount = tracks.map { it.album }.distinct().size,
                tracks = tracks,
              )
            }
            .sortedBy { it.name.lowercase() }

        val imported = matchingAudio.filter { it.source == MediaSource.IMPORTED }

        val matchingVideos =
          if (trimmed.isEmpty()) {
            videoList
          } else {
            videoList.filter {
              it.title.contains(trimmed, ignoreCase = true) ||
                it.folderName.contains(trimmed, ignoreCase = true) ||
                it.formatBadge.contains(trimmed, ignoreCase = true)
            }
          }

        val matchingPlaylists =
          if (trimmed.isEmpty()) {
            allPlaylists
          } else {
            allPlaylists.filter { it.name.contains(trimmed, ignoreCase = true) }
          }

        LibraryUiState(
          filteredAudioTracks = matchingAudio,
          albums = albums,
          artists = artists,
          importedTracks = imported,
          matchingVideos = matchingVideos,
          matchingPlaylists = matchingPlaylists,
          totalAudioCount = audioList.size,
          totalVideoCount = videoList.size,
          isScanning = scanning,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState(),
      )

  init {
    refreshPermissionAndScan()
  }

  fun refreshPermissionAndScan() {
    val granted = repository.hasAudioPermission()
    _hasAudioPermission.value = granted
    viewModelScope.launch { repository.scanDeviceMedia() }
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setFilter(filter: LibraryFilter) {
    _selectedFilter.value = filter
  }

  fun setSortOrder(order: SortOrder) {
    _sortOrder.value = order
  }

  fun importFiles(uris: List<Uri>) {
    if (uris.isEmpty()) return
    viewModelScope.launch {
      val count = repository.importFiles(uris)
      if (count > 0) {
        _snackbarMessages.tryEmit("$count Mediendatei(en) importiert")
      } else {
        _snackbarMessages.tryEmit("Keine unterstützten Audio- oder Videodateien gefunden")
      }
    }
  }

  fun importFolder(treeUri: Uri?) {
    if (treeUri == null) return
    viewModelScope.launch {
      val count = repository.importFolder(treeUri)
      if (count > 0) {
        _snackbarMessages.tryEmit("$count Datei(en) aus Ordner verknüpft")
      } else {
        _snackbarMessages.tryEmit("Keine Mediendateien im gewählten Ordner")
      }
    }
  }

  fun removeImportedMedia(uri: String) {
    viewModelScope.launch {
      repository.removeImportedMedia(uri)
      _snackbarMessages.tryEmit("Importierter Eintrag entfernt")
    }
  }

  fun toggleFavorite(uri: String) {
    viewModelScope.launch { repository.toggleFavorite(uri) }
  }

  fun createPlaylist(name: String) {
    viewModelScope.launch {
      val id = repository.createPlaylist(name)
      if (id > 0L) {
        _snackbarMessages.tryEmit("Playlist „${name.trim()}“ erstellt")
      }
    }
  }

  fun renamePlaylist(playlistId: Long, newName: String) {
    viewModelScope.launch {
      repository.renamePlaylist(playlistId, newName)
      _snackbarMessages.tryEmit("Playlist umbenannt")
    }
  }

  fun deletePlaylist(playlistId: Long) {
    viewModelScope.launch {
      repository.deletePlaylist(playlistId)
      _snackbarMessages.tryEmit("Playlist gelöscht")
    }
  }

  fun addTrackToPlaylist(playlistId: Long, track: MediaTrack, playlistName: String) {
    viewModelScope.launch {
      repository.addTrackToPlaylist(playlistId, track.uri)
      _snackbarMessages.tryEmit("„${track.title}“ zu „$playlistName“ hinzugefügt")
    }
  }

  fun removeTrackFromPlaylist(playlistId: Long, trackUri: String) {
    viewModelScope.launch { repository.removeTrackFromPlaylist(playlistId, trackUri) }
  }

  fun moveTrackInPlaylist(playlistId: Long, fromIndex: Int, toIndex: Int) {
    viewModelScope.launch { repository.moveTrackInPlaylist(playlistId, fromIndex, toIndex) }
  }

  fun setDynamicColor(enabled: Boolean) {
    viewModelScope.launch { settingsDataStore.setDynamicColor(enabled) }
  }

  fun setAmoledBlack(enabled: Boolean) {
    viewModelScope.launch { settingsDataStore.setAmoledBlack(enabled) }
  }

  class Factory(
    private val repository: MediaRepository,
    private val settingsDataStore: SettingsDataStore,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return LibraryViewModel(repository, settingsDataStore) as T
    }
  }
}
