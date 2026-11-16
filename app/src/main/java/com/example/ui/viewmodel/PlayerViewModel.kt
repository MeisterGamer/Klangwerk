package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.Player
import com.example.data.model.EqualizerPreset
import com.example.data.model.EqualizerState
import com.example.data.model.MediaTrack
import com.example.data.model.PlaybackUiState
import com.example.playback.AudioEffectsManager
import com.example.playback.PlaybackController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlayerViewModel(
  private val playbackController: PlaybackController,
  private val audioEffectsManager: AudioEffectsManager,
) : ViewModel() {

  val playbackState: StateFlow<PlaybackUiState> = playbackController.uiState
  val playerFlow: StateFlow<Player?> = playbackController.playerFlow
  val equalizerState: StateFlow<EqualizerState> = audioEffectsManager.state

  private val _showNowPlayingSheet = MutableStateFlow(false)
  val showNowPlayingSheet: StateFlow<Boolean> = _showNowPlayingSheet.asStateFlow()

  fun playQueue(tracks: List<MediaTrack>, startIndex: Int = 0) {
    playbackController.playQueue(tracks, startIndex)
  }

  fun playNext(track: MediaTrack) {
    playbackController.playNext(track)
  }

  fun addToQueue(track: MediaTrack) {
    playbackController.addToQueue(track)
  }

  fun togglePlayPause() {
    playbackController.togglePlayPause()
  }

  fun seekTo(positionMs: Long) {
    playbackController.seekTo(positionMs)
  }

  fun skipNext() {
    playbackController.skipNext()
  }

  fun skipPrevious() {
    playbackController.skipPrevious()
  }

  fun playQueueIndex(index: Int) {
    playbackController.playQueueIndex(index)
  }

  fun removeFromQueue(index: Int) {
    playbackController.removeFromQueue(index)
  }

  fun toggleShuffle() {
    playbackController.toggleShuffle()
  }

  fun cycleRepeatMode() {
    playbackController.cycleRepeatMode()
  }

  fun setPlaybackSpeed(speed: Float) {
    playbackController.setPlaybackSpeed(speed)
  }

  fun openNowPlaying() {
    _showNowPlayingSheet.value = true
  }

  fun closeNowPlaying() {
    _showNowPlayingSheet.value = false
  }

  fun setEqEnabled(enabled: Boolean) {
    audioEffectsManager.setEnabled(enabled)
  }

  fun selectPreset(preset: EqualizerPreset) {
    audioEffectsManager.selectPreset(preset)
  }

  fun setBandLevel(bandIndex: Int, levelMb: Int) {
    audioEffectsManager.setBandLevel(bandIndex, levelMb)
  }

  fun setBassBoost(strength: Int) {
    audioEffectsManager.setBassBoost(strength)
  }

  fun setVirtualizer(strength: Int) {
    audioEffectsManager.setVirtualizer(strength)
  }

  fun resetEqualizer() {
    audioEffectsManager.reset()
  }

  class Factory(
    private val playbackController: PlaybackController,
    private val audioEffectsManager: AudioEffectsManager,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return PlayerViewModel(playbackController, audioEffectsManager) as T
    }
  }
}
