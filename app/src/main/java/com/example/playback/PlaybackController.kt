package com.example.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.model.MediaTrack
import com.example.data.model.PlaybackUiState
import com.example.data.model.RepeatModeState
import com.example.data.repository.MediaRepository
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackController(
  private val context: Context,
  private val mediaRepository: MediaRepository,
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  private val _uiState = MutableStateFlow(PlaybackUiState())
  val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

  private val _playerFlow = MutableStateFlow<Player?>(null)
  val playerFlow: StateFlow<Player?> = _playerFlow.asStateFlow()

  private var controllerFuture: ListenableFuture<MediaController>? = null
  private var mediaController: MediaController? = null
  private var positionJob: Job? = null
  private var queuedTracks: List<MediaTrack> = emptyList()
  private var pendingAction: ((MediaController) -> Unit)? = null

  private val playerListener =
    object : Player.Listener {
      override fun onIsPlayingChanged(isPlaying: Boolean) {
        syncStateFromPlayer()
        if (isPlaying) startPositionTicker() else stopPositionTicker()
      }

      override fun onPlaybackStateChanged(playbackState: Int) {
        syncStateFromPlayer()
      }

      override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        syncStateFromPlayer()
        val uri = mediaItem?.mediaId
        if (!uri.isNullOrBlank()) {
          scope.launch { mediaRepository.recordPlay(uri) }
        }
      }

      override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        syncStateFromPlayer()
      }

      override fun onRepeatModeChanged(repeatMode: Int) {
        syncStateFromPlayer()
      }

      override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        syncStateFromPlayer()
      }
    }

  fun connect() {
    if (controllerFuture != null) return
    val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
    val future = MediaController.Builder(context, token).buildAsync()
    controllerFuture = future
    future.addListener(
      {
        runCatching {
          val controller = future.get()
          mediaController = controller
          _playerFlow.value = controller
          controller.addListener(playerListener)
          syncStateFromPlayer()
          if (controller.isPlaying) startPositionTicker()
          pendingAction?.invoke(controller)
          pendingAction = null
        }
      },
      MoreExecutors.directExecutor(),
    )
  }

  fun playQueue(tracks: List<MediaTrack>, startIndex: Int = 0) {
    if (tracks.isEmpty()) return
    val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
    queuedTracks = tracks
    withController { controller ->
      val mediaItems = tracks.map { it.toMediaItem() }
      controller.setMediaItems(mediaItems, safeIndex, 0L)
      controller.prepare()
      controller.play()
      syncStateFromPlayer()
      scope.launch { mediaRepository.recordPlay(tracks[safeIndex].uri) }
    }
  }

  fun playNext(track: MediaTrack) {
    withController { controller ->
      if (controller.mediaItemCount == 0) {
        playQueue(listOf(track), 0)
        return@withController
      }
      val insertIdx = (controller.currentMediaItemIndex + 1).coerceAtMost(controller.mediaItemCount)
      val mutable = queuedTracks.toMutableList()
      mutable.add(insertIdx.coerceIn(0, mutable.size), track)
      queuedTracks = mutable
      controller.addMediaItem(insertIdx, track.toMediaItem())
      syncStateFromPlayer()
    }
  }

  fun addToQueue(track: MediaTrack) {
    withController { controller ->
      if (controller.mediaItemCount == 0) {
        playQueue(listOf(track), 0)
        return@withController
      }
      queuedTracks = queuedTracks + track
      controller.addMediaItem(track.toMediaItem())
      syncStateFromPlayer()
    }
  }

  fun togglePlayPause() {
    withController { controller ->
      if (controller.isPlaying) {
        controller.pause()
      } else {
        if (controller.playbackState == Player.STATE_ENDED) {
          controller.seekTo(0L)
        }
        controller.play()
      }
    }
  }

  fun seekTo(positionMs: Long) {
    withController { controller ->
      controller.seekTo(positionMs.coerceAtLeast(0L))
      _uiState.update { it.copy(positionMs = positionMs.coerceAtLeast(0L)) }
    }
  }

  fun skipNext() {
    withController { controller ->
      if (controller.hasNextMediaItem()) {
        controller.seekToNextMediaItem()
        controller.play()
      }
    }
  }

  fun skipPrevious() {
    withController { controller ->
      if (controller.currentPosition > 4000L || !controller.hasPreviousMediaItem()) {
        controller.seekTo(0L)
      } else {
        controller.seekToPreviousMediaItem()
        controller.play()
      }
    }
  }

  fun playQueueIndex(index: Int) {
    withController { controller ->
      if (index in 0 until controller.mediaItemCount) {
        controller.seekToDefaultPosition(index)
        controller.play()
      }
    }
  }

  fun removeFromQueue(index: Int) {
    withController { controller ->
      if (index in 0 until controller.mediaItemCount) {
        if (index in queuedTracks.indices) {
          queuedTracks = queuedTracks.toMutableList().apply { removeAt(index) }
        }
        controller.removeMediaItem(index)
        syncStateFromPlayer()
      }
    }
  }

  fun toggleShuffle() {
    withController { controller ->
      controller.shuffleModeEnabled = !controller.shuffleModeEnabled
      syncStateFromPlayer()
    }
  }

  fun cycleRepeatMode() {
    withController { controller ->
      controller.repeatMode =
        when (controller.repeatMode) {
          Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
          Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
          else -> Player.REPEAT_MODE_OFF
        }
      syncStateFromPlayer()
    }
  }

  fun setPlaybackSpeed(speed: Float) {
    withController { controller ->
      controller.setPlaybackSpeed(speed.coerceIn(0.5f, 2.0f))
      syncStateFromPlayer()
    }
  }

  private fun withController(action: (MediaController) -> Unit) {
    val controller = mediaController
    if (controller != null) {
      action(controller)
    } else {
      pendingAction = action
      connect()
    }
  }

  private fun syncStateFromPlayer() {
    val controller = mediaController ?: return
    val idx = controller.currentMediaItemIndex
    val currentUri = controller.currentMediaItem?.mediaId
    val currentTrack =
      queuedTracks.getOrNull(idx)
        ?: queuedTracks.firstOrNull { it.uri == currentUri }

    val duration =
      controller.duration.takeIf { it > 0L }
        ?: currentTrack?.durationMs
        ?: 0L

    _uiState.update {
      it.copy(
        currentTrack = currentTrack,
        queue = queuedTracks,
        currentIndex = idx,
        isPlaying = controller.isPlaying,
        positionMs = controller.currentPosition.coerceAtLeast(0L),
        durationMs = duration,
        shuffleEnabled = controller.shuffleModeEnabled,
        repeatMode =
          when (controller.repeatMode) {
            Player.REPEAT_MODE_ALL -> RepeatModeState.ALL
            Player.REPEAT_MODE_ONE -> RepeatModeState.ONE
            else -> RepeatModeState.OFF
          },
        playbackSpeed = controller.playbackParameters.speed,
      )
    }
  }

  private fun startPositionTicker() {
    if (positionJob?.isActive == true) return
    positionJob =
      scope.launch {
        while (isActive) {
          mediaController?.let { controller ->
            val duration =
              controller.duration.takeIf { it > 0L } ?: _uiState.value.currentTrack?.durationMs ?: 0L
            _uiState.update {
              it.copy(
                positionMs = controller.currentPosition.coerceAtLeast(0L),
                durationMs = duration,
              )
            }
          }
          delay(400L)
        }
      }
  }

  private fun stopPositionTicker() {
    positionJob?.cancel()
    positionJob = null
  }

  private fun MediaTrack.toMediaItem(): MediaItem {
    val metadata =
      MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setArtworkUri(artworkUri?.let { Uri.parse(it) })
        .build()

    return MediaItem.Builder()
      .setMediaId(uri)
      .setUri(uri)
      .setMimeType(mimeType.takeIf { it.contains("/") && !it.endsWith("/*") })
      .setMediaMetadata(metadata)
      .build()
  }
}
