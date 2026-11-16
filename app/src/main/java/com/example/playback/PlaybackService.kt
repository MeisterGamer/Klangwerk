package com.example.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.KlangwerkApp
import com.example.MainActivity

class PlaybackService : MediaSessionService() {
  private var mediaSession: MediaSession? = null

  @OptIn(UnstableApi::class)
  override fun onCreate() {
    super.onCreate()
    val app = application as KlangwerkApp
    val audioEffectsManager = app.container.audioEffectsManager

    val audioAttributes =
      AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    val player =
      ExoPlayer.Builder(this)
        .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_LOCAL)
        .build()

    player.addListener(
      object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
          audioEffectsManager.attachToSession(audioSessionId)
        }
      }
    )

    if (player.audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
      audioEffectsManager.attachToSession(player.audioSessionId)
    }

    val openAppIntent = Intent(this, MainActivity::class.java)
    val sessionActivityPendingIntent =
      PendingIntent.getActivity(
        this,
        0,
        openAppIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
      )

    mediaSession =
      MediaSession.Builder(this, player)
        .setSessionActivity(sessionActivityPendingIntent)
        .build()
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
    return mediaSession
  }

  override fun onTaskRemoved(rootIntent: Intent?) {
    val player = mediaSession?.player
    if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
      stopSelf()
    }
  }

  override fun onDestroy() {
    val app = application as? KlangwerkApp
    app?.container?.audioEffectsManager?.releaseEffects()
    mediaSession?.run {
      player.release()
      release()
      mediaSession = null
    }
    super.onDestroy()
  }
}
