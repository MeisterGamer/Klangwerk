package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.model.MediaTrack
import com.example.data.model.MediaType
import com.example.ui.components.AddToPlaylistBottomSheet
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.SettingsBottomSheet
import com.example.ui.components.TrackDetailsDialog
import com.example.ui.navigation.EqualizerRoute
import com.example.ui.navigation.LibraryRoute
import com.example.ui.navigation.PlaylistDetailRoute
import com.example.ui.navigation.PlaylistsRoute
import com.example.ui.navigation.VideoPlayerRoute
import com.example.ui.navigation.VideosRoute
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NowPlayingScreenOverlay
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.screens.VideosScreen
import com.example.ui.theme.KlangwerkTheme
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

private data class TopNavItem(
  val label: String,
  val route: Any,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

class MainActivity : ComponentActivity() {
  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val container = (application as KlangwerkApp).container

    setContent {
      val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass
      val libraryViewModel: LibraryViewModel =
        viewModel(
          factory =
            LibraryViewModel.Factory(container.mediaRepository, container.settingsDataStore)
        )
      val playerViewModel: PlayerViewModel =
        viewModel(
          factory =
            PlayerViewModel.Factory(container.playbackController, container.audioEffectsManager)
        )

      val themeSettings by libraryViewModel.themeSettings.collectAsStateWithLifecycle()

      KlangwerkTheme(
        dynamicColor = themeSettings.dynamicColor,
        amoledBlack = themeSettings.amoledBlack,
      ) {
        KlangwerkMainScreen(
          widthSizeClass = widthSizeClass,
          libraryViewModel = libraryViewModel,
          playerViewModel = playerViewModel,
        )
      }
    }
  }
}

@Composable
private fun KlangwerkMainScreen(
  widthSizeClass: WindowWidthSizeClass,
  libraryViewModel: LibraryViewModel,
  playerViewModel: PlayerViewModel,
) {
  val navController = rememberNavController()
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  val libraryState by libraryViewModel.libraryUiState.collectAsStateWithLifecycle()
  val searchQuery by libraryViewModel.searchQuery.collectAsStateWithLifecycle()
  val selectedFilter by libraryViewModel.selectedFilter.collectAsStateWithLifecycle()
  val sortOrder by libraryViewModel.sortOrder.collectAsStateWithLifecycle()
  val hasAudioPermission by libraryViewModel.hasAudioPermission.collectAsStateWithLifecycle()
  val playlists by libraryViewModel.playlists.collectAsStateWithLifecycle()
  val favoriteTracks by libraryViewModel.favoriteTracks.collectAsStateWithLifecycle()
  val recentTracks by libraryViewModel.recentTracks.collectAsStateWithLifecycle()
  val videoTracks by libraryViewModel.videoTracks.collectAsStateWithLifecycle()
  val themeSettings by libraryViewModel.themeSettings.collectAsStateWithLifecycle()

  val playbackState by playerViewModel.playbackState.collectAsStateWithLifecycle()
  val player by playerViewModel.playerFlow.collectAsStateWithLifecycle()
  val eqState by playerViewModel.equalizerState.collectAsStateWithLifecycle()
  val showNowPlaying by playerViewModel.showNowPlayingSheet.collectAsStateWithLifecycle()

  var trackForPlaylistSheet by remember { mutableStateOf<MediaTrack?>(null) }
  var trackForDetailsDialog by remember { mutableStateOf<MediaTrack?>(null) }
  var showSettingsSheet by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    libraryViewModel.snackbarMessages.collect { message ->
      snackbarHostState.showSnackbar(message)
    }
  }

  val audioPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      libraryViewModel.refreshPermissionAndScan()
      if (!granted) {
        scope.launch {
          snackbarHostState.showSnackbar(
            "Zugriff abgelehnt – nutze alternativ den manuellen Dateiimport."
          )
        }
      }
    }

  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

  val importFilesLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
      libraryViewModel.importFiles(uris)
    }

  val importFolderLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
      libraryViewModel.importFolder(treeUri)
    }

  val pickVideosLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
      libraryViewModel.importFiles(uris)
    }

  val requestAudioScanPermission: () -> Unit = {
    val perm =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
      } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
      }
    audioPermissionLauncher.launch(perm)
  }

  val maybeRequestNotificationPermission: () -> Unit = {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  val launchFilesPicker: () -> Unit = {
    importFilesLauncher.launch(arrayOf("audio/*", "video/*", "application/ogg"))
  }

  val launchFolderPicker: () -> Unit = { importFolderLauncher.launch(null) }

  val launchVideoPicker: () -> Unit = {
    pickVideosLauncher.launch(
      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
    )
  }

  val playAudioQueue: (List<MediaTrack>, Int) -> Unit = { tracks, index ->
    maybeRequestNotificationPermission()
    playerViewModel.playQueue(tracks, index)
  }

  val playVideoQueue: (List<MediaTrack>, Int) -> Unit = { videos, index ->
    maybeRequestNotificationPermission()
    playerViewModel.playQueue(videos, index)
    navController.navigate(VideoPlayerRoute)
  }

  val navItems =
    listOf(
      TopNavItem(
        label = "Bibliothek",
        route = LibraryRoute,
        selectedIcon = Icons.Filled.LibraryMusic,
        unselectedIcon = Icons.Outlined.LibraryMusic,
        testTag = "nav_library",
      ),
      TopNavItem(
        label = "Playlists",
        route = PlaylistsRoute,
        selectedIcon = Icons.AutoMirrored.Filled.QueueMusic,
        unselectedIcon = Icons.AutoMirrored.Outlined.QueueMusic,
        testTag = "nav_playlists",
      ),
      TopNavItem(
        label = "Videos",
        route = VideosRoute,
        selectedIcon = Icons.Filled.VideoLibrary,
        unselectedIcon = Icons.Outlined.VideoLibrary,
        testTag = "nav_videos",
      ),
      TopNavItem(
        label = "Equalizer",
        route = EqualizerRoute,
        selectedIcon = Icons.Filled.GraphicEq,
        unselectedIcon = Icons.Outlined.GraphicEq,
        testTag = "nav_equalizer",
      ),
    )

  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = backStackEntry?.destination
  val isVideoPlayerActive = currentDestination?.hasRoute<VideoPlayerRoute>() == true
  val useNavRail = widthSizeClass != WindowWidthSizeClass.Compact

  val navigateTopLevel: (Any) -> Unit = { targetRoute ->
    navController.navigate(targetRoute) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      bottomBar = {
        if (!useNavRail && !isVideoPlayerActive) {
          Column(modifier = Modifier.fillMaxWidth()) {
            val activeTrack = playbackState.currentTrack
            AnimatedVisibility(
              visible = activeTrack != null,
              enter = slideInVertically { it },
              exit = slideOutVertically { it },
            ) {
              if (activeTrack != null) {
                val progress =
                  if (playbackState.durationMs > 0L) {
                    playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat()
                  } else {
                    0f
                  }
                MiniPlayerBar(
                  track = activeTrack,
                  isPlaying = playbackState.isPlaying,
                  progressFraction = progress,
                  onClick = {
                    if (activeTrack.mediaType == MediaType.VIDEO) {
                      navController.navigate(VideoPlayerRoute)
                    } else {
                      playerViewModel.openNowPlaying()
                    }
                  },
                  onTogglePlayPause = { playerViewModel.togglePlayPause() },
                  onSkipNext = { playerViewModel.skipNext() },
                )
              }
            }

            NavigationBar {
              navItems.forEach { item ->
                val selected =
                  when (item.route) {
                    LibraryRoute -> currentDestination?.hasRoute<LibraryRoute>() == true
                    PlaylistsRoute ->
                      currentDestination?.hasRoute<PlaylistsRoute>() == true ||
                        currentDestination?.hasRoute<PlaylistDetailRoute>() == true
                    VideosRoute -> currentDestination?.hasRoute<VideosRoute>() == true
                    EqualizerRoute -> currentDestination?.hasRoute<EqualizerRoute>() == true
                    else -> false
                  }
                NavigationBarItem(
                  selected = selected,
                  onClick = { navigateTopLevel(item.route) },
                  icon = {
                    Icon(
                      imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                      contentDescription = item.label,
                    )
                  },
                  label = { Text(item.label) },
                  modifier = Modifier.testTag(item.testTag),
                )
              }
            }
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier =
          Modifier.fillMaxSize()
            .padding(innerPadding)
            .let { if (!isVideoPlayerActive) it.statusBarsPadding() else it }
      ) {
        if (useNavRail && !isVideoPlayerActive) {
          NavigationRail(
            modifier = Modifier.fillMaxHeight().navigationBarsPadding(),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          ) {
            navItems.forEach { item ->
              val selected =
                when (item.route) {
                  LibraryRoute -> currentDestination?.hasRoute<LibraryRoute>() == true
                  PlaylistsRoute ->
                    currentDestination?.hasRoute<PlaylistsRoute>() == true ||
                      currentDestination?.hasRoute<PlaylistDetailRoute>() == true
                  VideosRoute -> currentDestination?.hasRoute<VideosRoute>() == true
                  EqualizerRoute -> currentDestination?.hasRoute<EqualizerRoute>() == true
                  else -> false
                }
              NavigationRailItem(
                selected = selected,
                onClick = { navigateTopLevel(item.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label,
                  )
                },
                label = { Text(item.label) },
                modifier = Modifier.testTag(item.testTag),
              )
            }
          }
        }

        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
          Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            NavHost(navController = navController, startDestination = LibraryRoute) {
              composable<LibraryRoute> {
                LibraryScreen(
                  uiState = libraryState,
                  searchQuery = searchQuery,
                  selectedFilter = selectedFilter,
                  sortOrder = sortOrder,
                  hasAudioPermission = hasAudioPermission,
                  currentTrackUri = playbackState.currentTrack?.uri,
                  isPlaying = playbackState.isPlaying,
                  onSearchQueryChange = libraryViewModel::setSearchQuery,
                  onSelectFilter = libraryViewModel::setFilter,
                  onSelectSortOrder = libraryViewModel::setSortOrder,
                  onRequestAudioPermission = requestAudioScanPermission,
                  onRescanLibrary = { libraryViewModel.refreshPermissionAndScan() },
                  onImportFiles = launchFilesPicker,
                  onImportFolder = launchFolderPicker,
                  onOpenSettings = { showSettingsSheet = true },
                  onPlayQueue = playAudioQueue,
                  onPlayVideo = { video -> playVideoQueue(listOf(video), 0) },
                  onOpenPlaylist = { pl ->
                    navController.navigate(PlaylistDetailRoute(playlistId = pl.id))
                  },
                  onToggleFavorite = libraryViewModel::toggleFavorite,
                  onPlayNext = { track ->
                    playerViewModel.playNext(track)
                    scope.launch {
                      snackbarHostState.showSnackbar("„${track.title}“ spielt als Nächstes")
                    }
                  },
                  onAddToQueue = { track ->
                    playerViewModel.addToQueue(track)
                    scope.launch {
                      snackbarHostState.showSnackbar("„${track.title}“ zur Warteschlange hinzugefügt")
                    }
                  },
                  onAddToPlaylist = { track -> trackForPlaylistSheet = track },
                  onShowDetails = { track -> trackForDetailsDialog = track },
                  onRemoveImported = libraryViewModel::removeImportedMedia,
                )
              }

              composable<PlaylistsRoute> {
                PlaylistsScreen(
                  playlists = playlists,
                  favoriteTracks = favoriteTracks,
                  recentTracks = recentTracks,
                  onOpenSystemPlaylist = { systemType ->
                    navController.navigate(PlaylistDetailRoute(systemType = systemType))
                  },
                  onOpenPlaylist = { playlistId ->
                    navController.navigate(PlaylistDetailRoute(playlistId = playlistId))
                  },
                  onPlayTracks = { tracks -> playAudioQueue(tracks, 0) },
                  onCreatePlaylist = libraryViewModel::createPlaylist,
                  onRenamePlaylist = libraryViewModel::renamePlaylist,
                  onDeletePlaylist = libraryViewModel::deletePlaylist,
                )
              }

              composable<PlaylistDetailRoute> { entry ->
                val route = entry.toRoute<PlaylistDetailRoute>()
                val customPlaylist = playlists.firstOrNull { it.id == route.playlistId }
                val (title, tracks, isCustom) =
                  when (route.systemType) {
                    "favorites" -> Triple("Favoriten", favoriteTracks, false)
                    "recent" -> Triple("Zuletzt gespielt", recentTracks, false)
                    else ->
                      Triple(
                        customPlaylist?.name ?: "Playlist",
                        customPlaylist?.tracks.orEmpty(),
                        customPlaylist != null,
                      )
                  }

                PlaylistDetailScreen(
                  title = title,
                  tracks = tracks,
                  isCustomPlaylist = isCustom,
                  currentTrackUri = playbackState.currentTrack?.uri,
                  isPlaying = playbackState.isPlaying,
                  onNavigateBack = { navController.popBackStack() },
                  onPlayQueue = playAudioQueue,
                  onToggleFavorite = libraryViewModel::toggleFavorite,
                  onPlayNext = { track -> playerViewModel.playNext(track) },
                  onAddToQueue = { track -> playerViewModel.addToQueue(track) },
                  onAddToPlaylist = { track -> trackForPlaylistSheet = track },
                  onShowDetails = { track -> trackForDetailsDialog = track },
                  onMoveTrack = { fromIdx, toIdx ->
                    if (isCustom) {
                      libraryViewModel.moveTrackInPlaylist(route.playlistId, fromIdx, toIdx)
                    }
                  },
                  onRemoveFromPlaylist = { uri ->
                    if (isCustom) {
                      libraryViewModel.removeTrackFromPlaylist(route.playlistId, uri)
                    }
                  },
                )
              }

              composable<VideosRoute> {
                VideosScreen(
                  videos = videoTracks,
                  onPickVideos = launchVideoPicker,
                  onImportFolder = launchFolderPicker,
                  onPlayVideo = playVideoQueue,
                  onRemoveImported = libraryViewModel::removeImportedMedia,
                )
              }

              composable<EqualizerRoute> {
                EqualizerScreen(
                  eqState = eqState,
                  onToggleEnabled = playerViewModel::setEqEnabled,
                  onSelectPreset = playerViewModel::selectPreset,
                  onBandLevelChange = playerViewModel::setBandLevel,
                  onBassBoostChange = playerViewModel::setBassBoost,
                  onVirtualizerChange = playerViewModel::setVirtualizer,
                  onReset = playerViewModel::resetEqualizer,
                )
              }

              composable<VideoPlayerRoute> {
                VideoPlayerScreen(
                  player = player,
                  playbackState = playbackState,
                  onNavigateBack = { navController.popBackStack() },
                  onTogglePlayPause = playerViewModel::togglePlayPause,
                  onSeekTo = playerViewModel::seekTo,
                  onSpeedChange = playerViewModel::setPlaybackSpeed,
                  onOpenEqualizer = {
                    navController.popBackStack()
                    navigateTopLevel(EqualizerRoute)
                  },
                )
              }
            }
          }

          if (useNavRail && !isVideoPlayerActive) {
            val activeTrack = playbackState.currentTrack
            if (activeTrack != null) {
              val progress =
                if (playbackState.durationMs > 0L) {
                  playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat()
                } else {
                  0f
                }
              MiniPlayerBar(
                track = activeTrack,
                isPlaying = playbackState.isPlaying,
                progressFraction = progress,
                onClick = {
                  if (activeTrack.mediaType == MediaType.VIDEO) {
                    navController.navigate(VideoPlayerRoute)
                  } else {
                    playerViewModel.openNowPlaying()
                  }
                },
                onTogglePlayPause = { playerViewModel.togglePlayPause() },
                onSkipNext = { playerViewModel.skipNext() },
                modifier = Modifier.navigationBarsPadding(),
              )
            }
          }
        }
      }
    }

    AnimatedVisibility(
      visible = showNowPlaying && playbackState.currentTrack != null && !isVideoPlayerActive,
      enter = slideInVertically { it },
      exit = slideOutVertically { it },
    ) {
      NowPlayingScreenOverlay(
        playbackState = playbackState,
        onClose = playerViewModel::closeNowPlaying,
        onTogglePlayPause = playerViewModel::togglePlayPause,
        onSeekTo = playerViewModel::seekTo,
        onSkipNext = playerViewModel::skipNext,
        onSkipPrevious = playerViewModel::skipPrevious,
        onToggleShuffle = playerViewModel::toggleShuffle,
        onCycleRepeat = playerViewModel::cycleRepeatMode,
        onSpeedChange = playerViewModel::setPlaybackSpeed,
        onToggleFavorite = libraryViewModel::toggleFavorite,
        onAddToPlaylist = { track -> trackForPlaylistSheet = track },
        onOpenEqualizer = { navigateTopLevel(EqualizerRoute) },
        onPlayQueueIndex = playerViewModel::playQueueIndex,
        onRemoveFromQueue = playerViewModel::removeFromQueue,
      )
    }

    trackForPlaylistSheet?.let { track ->
      AddToPlaylistBottomSheet(
        track = track,
        playlists = playlists,
        onDismiss = { trackForPlaylistSheet = null },
        onSelectPlaylist = { pl ->
          libraryViewModel.addTrackToPlaylist(pl.id, track, pl.name)
          trackForPlaylistSheet = null
        },
        onCreateNewPlaylist = { name -> libraryViewModel.createPlaylist(name) },
      )
    }

    trackForDetailsDialog?.let { track ->
      TrackDetailsDialog(track = track, onDismiss = { trackForDetailsDialog = null })
    }

    if (showSettingsSheet) {
      SettingsBottomSheet(
        themeSettings = themeSettings,
        onDynamicColorChanged = libraryViewModel::setDynamicColor,
        onAmoledBlackChanged = libraryViewModel::setAmoledBlack,
        onRescanLibrary = { libraryViewModel.refreshPermissionAndScan() },
        onImportFiles = launchFilesPicker,
        onImportFolder = launchFolderPicker,
        onDismiss = { showSettingsSheet = false },
      )
    }
  }
}
