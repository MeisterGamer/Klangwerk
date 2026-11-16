package com.example.data.repository

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.example.data.local.ImportedMediaEntity
import com.example.data.model.MediaSource
import com.example.data.model.MediaTrack
import com.example.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaScanner(private val context: Context) {

  fun hasAudioScanPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
        PackageManager.PERMISSION_GRANTED
    }
  }

  suspend fun scanMediaStoreAudio(): List<MediaTrack> =
    withContext(Dispatchers.IO) {
      if (!hasAudioScanPermission()) return@withContext emptyList()

      val tracks = mutableListOf<MediaTrack>()
      val projection =
        arrayOf(
          MediaStore.Audio.Media._ID,
          MediaStore.Audio.Media.TITLE,
          MediaStore.Audio.Media.ARTIST,
          MediaStore.Audio.Media.ALBUM,
          MediaStore.Audio.Media.ALBUM_ID,
          MediaStore.Audio.Media.DURATION,
          MediaStore.Audio.Media.MIME_TYPE,
          MediaStore.Audio.Media.DISPLAY_NAME,
          MediaStore.Audio.Media.DATE_ADDED,
          MediaStore.Audio.Media.SIZE,
          MediaStore.Audio.Media.BUCKET_DISPLAY_NAME,
        )

      val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%'"
      val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

      runCatching {
        context.contentResolver
          .query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder,
          )
          ?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val bucketCol = cursor.getColumnIndex(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
              val id = cursor.getLong(idCol)
              val duration = cursor.getLong(durationCol)
              if (duration <= 0L) continue

              val contentUri =
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
              val albumId = cursor.getLong(albumIdCol)
              val artworkUri =
                if (albumId > 0) {
                  ContentUris.withAppendedId(
                      Uri.parse("content://media/external/audio/albumart"),
                      albumId,
                    )
                    .toString()
                } else {
                  null
                }
              val displayName = cursor.getString(nameCol).orEmpty()
              val rawTitle = cursor.getString(titleCol).orEmpty()
              val title = rawTitle.ifBlank { displayName.substringBeforeLast('.') }.ifBlank { "Unbekannter Titel" }
              val artist =
                cursor.getString(artistCol).orEmpty().let {
                  if (it.isBlank() || it == "<unknown>") "Unbekannter Interpret" else it
                }
              val album =
                cursor.getString(albumCol).orEmpty().let {
                  if (it.isBlank() || it == "<unknown>") "Unbekanntes Album" else it
                }
              val mimeType = cursor.getString(mimeCol).orEmpty().ifBlank { "audio/*" }
              val folderName =
                if (bucketCol >= 0) cursor.getString(bucketCol).orEmpty().ifBlank { "Musik" }
                else "Musik"

              tracks.add(
                MediaTrack(
                  uri = contentUri.toString(),
                  title = title,
                  artist = artist,
                  album = album,
                  durationMs = duration,
                  mimeType = mimeType,
                  formatBadge = extractFormatBadge(mimeType, displayName),
                  artworkUri = artworkUri,
                  folderName = folderName,
                  mediaType = MediaType.AUDIO,
                  source = MediaSource.MEDIA_STORE,
                  dateAddedSec = cursor.getLong(dateCol),
                  sizeBytes = cursor.getLong(sizeCol),
                )
              )
            }
          }
      }
      tracks
    }

  suspend fun scanLegacyMediaStoreVideos(): List<MediaTrack> =
    withContext(Dispatchers.IO) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return@withContext emptyList()
      if (
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) !=
          PackageManager.PERMISSION_GRANTED
      ) {
        return@withContext emptyList()
      }

      val videos = mutableListOf<MediaTrack>()
      val projection =
        arrayOf(
          MediaStore.Video.Media._ID,
          MediaStore.Video.Media.TITLE,
          MediaStore.Video.Media.DURATION,
          MediaStore.Video.Media.MIME_TYPE,
          MediaStore.Video.Media.DISPLAY_NAME,
          MediaStore.Video.Media.DATE_ADDED,
          MediaStore.Video.Media.SIZE,
          MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        )

      runCatching {
        context.contentResolver
          .query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC",
          )
          ?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
              val id = cursor.getLong(idCol)
              val duration = cursor.getLong(durationCol)
              if (duration <= 0L) continue
              val contentUri =
                ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
              val displayName = cursor.getString(nameCol).orEmpty()
              val title = cursor.getString(titleCol).orEmpty().ifBlank { displayName.substringBeforeLast('.') }
              val mimeType = cursor.getString(mimeCol).orEmpty().ifBlank { "video/*" }
              val folderName =
                if (bucketCol >= 0) cursor.getString(bucketCol).orEmpty().ifBlank { "Videos" }
                else "Videos"

              videos.add(
                MediaTrack(
                  uri = contentUri.toString(),
                  title = title.ifBlank { "Video" },
                  artist = folderName,
                  album = "Lokale Videos",
                  durationMs = duration,
                  mimeType = mimeType,
                  formatBadge = extractFormatBadge(mimeType, displayName),
                  artworkUri = contentUri.toString(),
                  folderName = folderName,
                  mediaType = MediaType.VIDEO,
                  source = MediaSource.MEDIA_STORE,
                  dateAddedSec = cursor.getLong(dateCol),
                  sizeBytes = cursor.getLong(sizeCol),
                )
              )
            }
          }
      }
      videos
    }

  suspend fun extractImportedMedia(uris: List<Uri>, folderLabel: String? = null): List<ImportedMediaEntity> =
    withContext(Dispatchers.IO) {
      uris.mapNotNull { uri ->
        takePersistablePermission(uri)
        inspectSingleUri(uri, folderLabel)
      }
    }

  suspend fun extractFromFolderTree(treeUri: Uri): List<ImportedMediaEntity> =
    withContext(Dispatchers.IO) {
      takePersistablePermission(treeUri)
      val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
      val folderName = rootDoc.name ?: "Importierter Ordner"
      val collected = mutableListOf<ImportedMediaEntity>()
      collectDocumentFiles(rootDoc, folderName, collected, depth = 0)
      collected
    }

  private fun collectDocumentFiles(
    dir: DocumentFile,
    folderName: String,
    out: MutableList<ImportedMediaEntity>,
    depth: Int,
  ) {
    if (depth > 4) return
    val files = runCatching { dir.listFiles() }.getOrNull() ?: return
    for (doc in files) {
      if (doc.isDirectory) {
        collectDocumentFiles(doc, doc.name ?: folderName, out, depth + 1)
      } else if (doc.isFile) {
        val mime = doc.type ?: guessMimeFromName(doc.name.orEmpty())
        if (mime.startsWith("audio/") || mime.startsWith("video/")) {
          inspectSingleUri(doc.uri, folderName, doc.name, mime, doc.length())?.let { out.add(it) }
        }
      }
    }
  }

  private fun inspectSingleUri(
    uri: Uri,
    folderLabel: String? = null,
    knownName: String? = null,
    knownMime: String? = null,
    knownSize: Long = 0L,
  ): ImportedMediaEntity? {
    val resolver = context.contentResolver
    val docFile = runCatching { DocumentFile.fromSingleUri(context, uri) }.getOrNull()
    val displayName = knownName ?: docFile?.name ?: uri.lastPathSegment ?: "Mediendatei"
    val mimeType =
      knownMime
        ?: resolver.getType(uri)
        ?: docFile?.type
        ?: guessMimeFromName(displayName)
    val sizeBytes = if (knownSize > 0L) knownSize else (docFile?.length() ?: 0L)
    val isVideo = mimeType.startsWith("video/")
    val isAudio = mimeType.startsWith("audio/") || mimeType == "application/ogg"
    if (!isVideo && !isAudio) return null

    var title = displayName.substringBeforeLast('.').ifBlank { displayName }
    var artist = if (isVideo) "Lokales Video" else "Unbekannter Interpret"
    var album = if (isVideo) "Videos" else "Importiert"
    var durationMs = 0L

    val retriever = MediaMetadataRetriever()
    try {
      retriever.setDataSource(context, uri)
      retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }?.let {
        title = it
      }
      retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }?.let {
        artist = it
      }
      retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() }?.let {
        album = it
      }
      retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let {
        durationMs = it
      }
    } catch (_: Exception) {
      // Fallback to DocumentFile metadata when retriever cannot read header
    } finally {
      runCatching { retriever.release() }
    }

    return ImportedMediaEntity(
      uri = uri.toString(),
      title = title,
      artist = artist,
      album = album,
      durationMs = durationMs.coerceAtLeast(0L),
      mimeType = mimeType,
      formatBadge = extractFormatBadge(mimeType, displayName),
      artworkUri = if (isVideo) uri.toString() else null,
      folderName = folderLabel ?: "Importiert",
      mediaType = if (isVideo) MediaType.VIDEO.name else MediaType.AUDIO.name,
      sizeBytes = sizeBytes,
    )
  }

  private fun takePersistablePermission(uri: Uri) {
    runCatching {
      context.contentResolver.takePersistableUriPermission(
        uri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION,
      )
    }
  }

  private fun guessMimeFromName(name: String): String {
    val ext = name.substringAfterLast('.', "").lowercase()
    return when (ext) {
      "mp3" -> "audio/mpeg"
      "flac" -> "audio/flac"
      "aac", "m4a" -> "audio/mp4"
      "ogg", "oga" -> "audio/ogg"
      "opus" -> "audio/opus"
      "wav" -> "audio/wav"
      "alac" -> "audio/alac"
      "wma" -> "audio/x-ms-wma"
      "mp4", "m4v" -> "video/mp4"
      "mkv" -> "video/x-matroska"
      "webm" -> "video/webm"
      "avi" -> "video/x-msvideo"
      "mov" -> "video/quicktime"
      else -> "audio/*"
    }
  }

  private fun extractFormatBadge(mimeType: String, fileName: String): String {
    val ext = fileName.substringAfterLast('.', "").uppercase()
    if (ext.length in 2..4 && ext.all { it.isLetterOrDigit() }) {
      return ext
    }
    return when {
      mimeType.contains("flac") -> "FLAC"
      mimeType.contains("mpeg") -> "MP3"
      mimeType.contains("opus") -> "OPUS"
      mimeType.contains("ogg") -> "OGG"
      mimeType.contains("wav") -> "WAV"
      mimeType.contains("aac") || mimeType.contains("mp4") ->
        if (mimeType.startsWith("video/")) "MP4" else "AAC"
      mimeType.contains("matroska") -> "MKV"
      mimeType.contains("webm") -> "WEBM"
      else -> "AUDIO"
    }
  }
}
