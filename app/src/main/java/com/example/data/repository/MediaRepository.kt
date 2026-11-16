package com.example.data.repository

import android.net.Uri
import com.example.data.local.ImportedMediaDao
import com.example.data.local.ImportedMediaEntity
import com.example.data.local.PlaylistDao
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackEntity
import com.example.data.local.TrackMetadataDao
import com.example.data.local.TrackMetadataEntity
import com.example.data.model.MediaSource
import com.example.data.model.MediaTrack
import com.example.data.model.MediaType
import com.example.data.model.PlaylistSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class MediaRepository(
  private val scanner: MediaScanner,
  private val playlistDao: PlaylistDao,
  private val importedMediaDao: ImportedMediaDao,
  private val trackMetadataDao: TrackMetadataDao,
) {
  private val mediaStoreAudioFlow = MutableStateFlow<List<MediaTrack>>(emptyList())
  private val mediaStoreVideoFlow = MutableStateFlow<List<MediaTrack>>(emptyList())

  private val _isScanning = MutableStateFlow(false)
  val isScanning: Flow<Boolean> = _isScanning

  val allTracks: Flow<List<MediaTrack>> =
    combine(
      mediaStoreAudioFlow,
      mediaStoreVideoFlow,
      importedMediaDao.observeImportedMedia(),
      trackMetadataDao.observeAllMetadata(),
    ) { storeAudio, storeVideos, imported, metadataList ->
      val metaMap = metadataList.associateBy { it.uri }
      val importedModels = imported.map { it.toDomain() }

      val mergedByUri = linkedMapOf<String, MediaTrack>()
      // MediaStore items first, imported items append/override
      for (track in storeAudio + storeVideos + importedModels) {
        val meta = metaMap[track.uri]
        mergedByUri[track.uri] =
          track.copy(
            isFavorite = meta?.isFavorite ?: false,
            lastPlayedAt = meta?.lastPlayedAt ?: 0L,
          )
      }
      mergedByUri.values.toList()
    }

  val audioTracks: Flow<List<MediaTrack>> =
    allTracks.map { tracks -> tracks.filter { it.mediaType == MediaType.AUDIO } }

  val videoTracks: Flow<List<MediaTrack>> =
    allTracks.map { tracks -> tracks.filter { it.mediaType == MediaType.VIDEO } }

  val favoriteTracks: Flow<List<MediaTrack>> =
    allTracks.map { tracks -> tracks.filter { it.isFavorite } }

  val recentTracks: Flow<List<MediaTrack>> =
    allTracks.map { tracks ->
      tracks.filter { it.lastPlayedAt > 0L }.sortedByDescending { it.lastPlayedAt }.take(40)
    }

  val playlists: Flow<List<PlaylistSummary>> =
    combine(
      playlistDao.observePlaylists(),
      playlistDao.observeAllPlaylistTracks(),
      allTracks,
    ) { playlistEntities, mappings, tracks ->
      val trackMap = tracks.associateBy { it.uri }
      val grouped = mappings.groupBy { it.playlistId }
      playlistEntities.map { pl ->
        val orderedTracks =
          grouped[pl.id].orEmpty().sortedBy { it.position }.mapNotNull { trackMap[it.trackUri] }
        PlaylistSummary(
          id = pl.id,
          name = pl.name,
          createdAt = pl.createdAt,
          tracks = orderedTracks,
        )
      }
    }

  fun hasAudioPermission(): Boolean = scanner.hasAudioScanPermission()

  suspend fun scanDeviceMedia() {
    _isScanning.value = true
    try {
      mediaStoreAudioFlow.value = scanner.scanMediaStoreAudio()
      mediaStoreVideoFlow.value = scanner.scanLegacyMediaStoreVideos()
    } finally {
      _isScanning.value = false
    }
  }

  suspend fun importFiles(uris: List<Uri>): Int {
    if (uris.isEmpty()) return 0
    _isScanning.value = true
    return try {
      val items = scanner.extractImportedMedia(uris)
      if (items.isNotEmpty()) {
        importedMediaDao.upsertImportedMedia(items)
      }
      items.size
    } finally {
      _isScanning.value = false
    }
  }

  suspend fun importFolder(treeUri: Uri): Int {
    _isScanning.value = true
    return try {
      val items = scanner.extractFromFolderTree(treeUri)
      if (items.isNotEmpty()) {
        importedMediaDao.upsertImportedMedia(items)
      }
      items.size
    } finally {
      _isScanning.value = false
    }
  }

  suspend fun removeImportedMedia(uri: String) {
    importedMediaDao.deleteImportedMedia(uri)
  }

  suspend fun toggleFavorite(uri: String) {
    val existing = trackMetadataDao.getMetadata(uri)
    val updated =
      existing?.copy(isFavorite = !existing.isFavorite)
        ?: TrackMetadataEntity(uri = uri, isFavorite = true)
    trackMetadataDao.upsertMetadata(updated)
  }

  suspend fun recordPlay(uri: String) {
    val existing = trackMetadataDao.getMetadata(uri)
    val now = System.currentTimeMillis()
    val updated =
      existing?.copy(lastPlayedAt = now, playCount = existing.playCount + 1)
        ?: TrackMetadataEntity(uri = uri, isFavorite = false, lastPlayedAt = now, playCount = 1)
    trackMetadataDao.upsertMetadata(updated)
  }

  suspend fun createPlaylist(name: String): Long {
    val clean = name.trim()
    if (clean.isEmpty()) return -1L
    return playlistDao.insertPlaylist(PlaylistEntity(name = clean))
  }

  suspend fun renamePlaylist(playlistId: Long, newName: String) {
    val clean = newName.trim()
    if (clean.isNotEmpty()) {
      playlistDao.renamePlaylist(playlistId, clean)
    }
  }

  suspend fun deletePlaylist(playlistId: Long) {
    playlistDao.deletePlaylist(playlistId)
  }

  suspend fun addTrackToPlaylist(playlistId: Long, trackUri: String) {
    val nextPos = playlistDao.getNextPosition(playlistId)
    playlistDao.insertPlaylistTrack(
      PlaylistTrackEntity(playlistId = playlistId, trackUri = trackUri, position = nextPos)
    )
  }

  suspend fun removeTrackFromPlaylist(playlistId: Long, trackUri: String) {
    playlistDao.removeTrackFromPlaylist(playlistId, trackUri)
  }

  suspend fun moveTrackInPlaylist(playlistId: Long, fromIndex: Int, toIndex: Int) {
    val entries = playlistDao.getTracksForPlaylist(playlistId)
    if (fromIndex !in entries.indices || toIndex !in entries.indices || fromIndex == toIndex) return
    val a = entries[fromIndex]
    val b = entries[toIndex]
    playlistDao.swapTrackPositions(
      playlistId = playlistId,
      uriA = a.trackUri,
      posA = a.position,
      uriB = b.trackUri,
      posB = b.position,
    )
  }

  private fun ImportedMediaEntity.toDomain(): MediaTrack {
    return MediaTrack(
      uri = uri,
      title = title,
      artist = artist,
      album = album,
      durationMs = durationMs,
      mimeType = mimeType,
      formatBadge = formatBadge,
      artworkUri = artworkUri,
      folderName = folderName,
      mediaType = if (mediaType == MediaType.VIDEO.name) MediaType.VIDEO else MediaType.AUDIO,
      source = MediaSource.IMPORTED,
      dateAddedSec = addedAt / 1000L,
      sizeBytes = sizeBytes,
    )
  }
}
