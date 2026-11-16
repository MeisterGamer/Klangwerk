package com.example

import android.app.Application
import com.example.data.local.KlangwerkDatabase
import com.example.data.preferences.SettingsDataStore
import com.example.data.repository.MediaRepository
import com.example.data.repository.MediaScanner
import com.example.playback.AudioEffectsManager
import com.example.playback.PlaybackController

class AppContainer(application: Application) {
  val database: KlangwerkDatabase = KlangwerkDatabase.getInstance(application)
  val settingsDataStore: SettingsDataStore = SettingsDataStore(application)
  val mediaScanner: MediaScanner = MediaScanner(application)
  val mediaRepository: MediaRepository =
    MediaRepository(
      scanner = mediaScanner,
      playlistDao = database.playlistDao(),
      importedMediaDao = database.importedMediaDao(),
      trackMetadataDao = database.trackMetadataDao(),
    )
  val audioEffectsManager: AudioEffectsManager = AudioEffectsManager(settingsDataStore)
  val playbackController: PlaybackController = PlaybackController(application, mediaRepository)
}

class KlangwerkApp : Application() {
  lateinit var container: AppContainer
    private set

  override fun onCreate() {
    super.onCreate()
    container = AppContainer(this)
    container.playbackController.connect()
  }
}
