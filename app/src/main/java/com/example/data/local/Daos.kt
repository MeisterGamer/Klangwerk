package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
  @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
  fun observePlaylists(): Flow<List<PlaylistEntity>>

  @Query("SELECT * FROM playlist_tracks ORDER BY playlistId ASC, position ASC")
  fun observeAllPlaylistTracks(): Flow<List<PlaylistTrackEntity>>

  @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
  suspend fun getTracksForPlaylist(playlistId: Long): List<PlaylistTrackEntity>

  @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_tracks WHERE playlistId = :playlistId")
  suspend fun getNextPosition(playlistId: Long): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPlaylist(playlist: PlaylistEntity): Long

  @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
  suspend fun renamePlaylist(playlistId: Long, newName: String)

  @Query("DELETE FROM playlists WHERE id = :playlistId")
  suspend fun deletePlaylist(playlistId: Long)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertPlaylistTrack(entry: PlaylistTrackEntity)

  @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackUri = :trackUri")
  suspend fun removeTrackFromPlaylist(playlistId: Long, trackUri: String)

  @Query("UPDATE playlist_tracks SET position = :position WHERE playlistId = :playlistId AND trackUri = :trackUri")
  suspend fun updateTrackPosition(playlistId: Long, trackUri: String, position: Int)

  @Transaction
  suspend fun swapTrackPositions(playlistId: Long, uriA: String, posA: Int, uriB: String, posB: Int) {
    updateTrackPosition(playlistId, uriA, posB)
    updateTrackPosition(playlistId, uriB, posA)
  }
}

@Dao
interface ImportedMediaDao {
  @Query("SELECT * FROM imported_media ORDER BY addedAt DESC")
  fun observeImportedMedia(): Flow<List<ImportedMediaEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertImportedMedia(items: List<ImportedMediaEntity>)

  @Query("DELETE FROM imported_media WHERE uri = :uri")
  suspend fun deleteImportedMedia(uri: String)
}

@Dao
interface TrackMetadataDao {
  @Query("SELECT * FROM track_metadata")
  fun observeAllMetadata(): Flow<List<TrackMetadataEntity>>

  @Query("SELECT * FROM track_metadata WHERE uri = :uri LIMIT 1")
  suspend fun getMetadata(uri: String): TrackMetadataEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertMetadata(metadata: TrackMetadataEntity)
}
