package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
  tableName = "playlist_tracks",
  primaryKeys = ["playlistId", "trackUri"],
  foreignKeys = [
    ForeignKey(
      entity = PlaylistEntity::class,
      parentColumns = ["id"],
      childColumns = ["playlistId"],
      onDelete = ForeignKey.CASCADE,
    )
  ],
  indices = [Index(value = ["playlistId"]), Index(value = ["trackUri"])],
)
data class PlaylistTrackEntity(
  val playlistId: Long,
  val trackUri: String,
  val position: Int,
  val addedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "imported_media")
data class ImportedMediaEntity(
  @PrimaryKey val uri: String,
  val title: String,
  val artist: String,
  val album: String,
  val durationMs: Long,
  val mimeType: String,
  val formatBadge: String,
  val artworkUri: String?,
  val folderName: String,
  val mediaType: String, // "AUDIO" or "VIDEO"
  val sizeBytes: Long,
  val addedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "track_metadata")
data class TrackMetadataEntity(
  @PrimaryKey val uri: String,
  val isFavorite: Boolean = false,
  val lastPlayedAt: Long = 0L,
  val playCount: Int = 0,
)
