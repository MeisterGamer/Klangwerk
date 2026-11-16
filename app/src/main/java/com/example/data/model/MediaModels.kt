package com.example.data.model

enum class MediaType {
  AUDIO,
  VIDEO,
}

enum class MediaSource {
  MEDIA_STORE,
  IMPORTED,
}

enum class LibraryFilter(val labelRes: String) {
  ALL_TRACKS("Titel"),
  ALBUMS("Alben"),
  ARTISTS("Interpreten"),
  IMPORTED("Importiert"),
}

enum class SortOrder(val label: String) {
  TITLE("Titel"),
  ARTIST("Interpret"),
  DURATION("Dauer"),
  RECENT("Zuletzt hinzugefügt"),
}

data class MediaTrack(
  val uri: String,
  val title: String,
  val artist: String,
  val album: String,
  val durationMs: Long,
  val mimeType: String,
  val formatBadge: String,
  val artworkUri: String? = null,
  val folderName: String = "Gerät",
  val mediaType: MediaType = MediaType.AUDIO,
  val source: MediaSource = MediaSource.MEDIA_STORE,
  val dateAddedSec: Long = 0L,
  val sizeBytes: Long = 0L,
  val isFavorite: Boolean = false,
  val lastPlayedAt: Long = 0L,
)

data class AlbumGroup(
  val name: String,
  val artist: String,
  val artworkUri: String?,
  val tracks: List<MediaTrack>,
) {
  val totalDurationMs: Long
    get() = tracks.sumOf { it.durationMs }
}

data class ArtistGroup(
  val name: String,
  val albumCount: Int,
  val tracks: List<MediaTrack>,
)

data class PlaylistSummary(
  val id: Long,
  val name: String,
  val createdAt: Long,
  val tracks: List<MediaTrack>,
) {
  val totalDurationMs: Long
    get() = tracks.sumOf { it.durationMs }
}

enum class EqualizerPreset(
  val id: String,
  val label: String,
  val relativeBands: List<Float>, // -1.0f .. +1.0f normalized across min..max range
) {
  LINEAR("linear", "Linear", listOf(0f, 0f, 0f, 0f, 0f)),
  BASS_BOOST("bass", "Bass-Boost", listOf(0.75f, 0.45f, 0.05f, 0.0f, 0.1f)),
  ROCK("rock", "Rock", listOf(0.55f, 0.25f, -0.15f, 0.35f, 0.6f)),
  POP("pop", "Pop", listOf(-0.15f, 0.3f, 0.55f, 0.35f, -0.1f)),
  JAZZ("jazz", "Jazz", listOf(0.4f, 0.2f, -0.2f, 0.3f, 0.45f)),
  KLASSIK("klassik", "Klassik", listOf(0.45f, 0.25f, -0.1f, 0.2f, 0.5f)),
  SPRACHE("sprache", "Sprache", listOf(-0.4f, -0.1f, 0.6f, 0.5f, 0.15f)),
  CUSTOM("custom", "Benutzerdefiniert", listOf(0f, 0f, 0f, 0f, 0f));

  companion object {
    fun fromId(id: String): EqualizerPreset =
      entries.firstOrNull { it.id == id } ?: LINEAR
  }
}

data class EqualizerState(
  val enabled: Boolean = false,
  val presetId: String = EqualizerPreset.LINEAR.id,
  val bandLevelsMb: List<Int> = listOf(0, 0, 0, 0, 0),
  val bandFrequenciesHz: List<Int> = listOf(60, 230, 910, 3600, 14000),
  val minLevelMb: Int = -1500,
  val maxLevelMb: Int = 1500,
  val bassBoostStrength: Int = 0, // 0..1000
  val virtualizerStrength: Int = 0, // 0..1000
  val hardwareAttached: Boolean = false,
)

data class AppThemeSettings(
  val dynamicColor: Boolean = true,
  val amoledBlack: Boolean = false,
)

enum class RepeatModeState {
  OFF,
  ALL,
  ONE,
}

data class PlaybackUiState(
  val currentTrack: MediaTrack? = null,
  val queue: List<MediaTrack> = emptyList(),
  val currentIndex: Int = -1,
  val isPlaying: Boolean = false,
  val positionMs: Long = 0L,
  val durationMs: Long = 0L,
  val shuffleEnabled: Boolean = false,
  val repeatMode: RepeatModeState = RepeatModeState.OFF,
  val playbackSpeed: Float = 1.0f,
  val audioSessionId: Int = 0,
)

fun formatDuration(ms: Long): String {
  val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
  val hours = totalSeconds / 3600L
  val minutes = (totalSeconds % 3600L) / 60L
  val seconds = totalSeconds % 60L
  return if (hours > 0) {
    String.format("%d:%02d:%02d", hours, minutes, seconds)
  } else {
    String.format("%d:%02d", minutes, seconds)
  }
}

fun formatFileSize(bytes: Long): String {
  if (bytes <= 0L) return "Unbekannt"
  val mb = bytes / (1024.0 * 1024.0)
  return if (mb >= 1024.0) {
    String.format("%.2f GB", mb / 1024.0)
  } else {
    String.format("%.1f MB", mb)
  }
}

fun formatFrequencyHz(hz: Int): String {
  return if (hz >= 1000) {
    val khz = hz / 1000.0
    if (khz % 1.0 == 0.0) "${khz.toInt()} kHz" else String.format("%.1f kHz", khz)
  } else {
    "$hz Hz"
  }
}
