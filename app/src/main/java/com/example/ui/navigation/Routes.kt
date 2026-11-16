package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object LibraryRoute

@Serializable data object PlaylistsRoute

@Serializable
data class PlaylistDetailRoute(
  val playlistId: Long = -1L,
  val systemType: String? = null, // "favorites" or "recent"
)

@Serializable data object VideosRoute

@Serializable data object EqualizerRoute

@Serializable data object VideoPlayerRoute
