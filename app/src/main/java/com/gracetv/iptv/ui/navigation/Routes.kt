package com.gracetv.iptv.ui.navigation

import com.gracetv.iptv.domain.model.ContentType
import kotlinx.serialization.Serializable

@Serializable
sealed interface GraceRoute {
    @Serializable
    data object Splash : GraceRoute

    @Serializable
    data object Home : GraceRoute

    @Serializable
    data object LiveTv : GraceRoute

    @Serializable
    data object Movies : GraceRoute

    @Serializable
    data class MovieDetail(val movieId: Long) : GraceRoute

    @Serializable
    data object Series : GraceRoute

    @Serializable
    data class SeriesDetail(val seriesId: Long) : GraceRoute

    @Serializable
    data object Favorites : GraceRoute

    @Serializable
    data object Search : GraceRoute

    @Serializable
    data object Playlists : GraceRoute

    @Serializable
    data class Epg(val initialChannelId: Long = -1L) : GraceRoute

    @Serializable
    data object Settings : GraceRoute

    @Serializable
    data object Support : GraceRoute

    @Serializable
    data class Player(
        val playlistId: Long,
        val contentType: ContentType,
        val itemId: Long,
        val remoteId: String,
        val title: String,
        val subtitle: String = "",
        val imageUrl: String = "",
        val streamUrl: String,
        val seriesRemoteId: String = ""
    ) : GraceRoute
}
