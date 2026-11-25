package com.gracetv.iptv.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.gracetv.iptv.data.local.entity.MovieEntity
import com.gracetv.iptv.data.local.entity.PlaybackProgressEntity
import com.gracetv.iptv.data.local.entity.SeriesEntity
import com.gracetv.iptv.data.local.entity.WatchHistoryEntity
import com.gracetv.iptv.domain.model.ContentType
import com.gracetv.iptv.ui.components.ConnectionStatusBadge
import com.gracetv.iptv.ui.components.EmptyStateView
import com.gracetv.iptv.ui.components.MediaImage
import com.gracetv.iptv.ui.components.PosterMediaCard
import com.gracetv.iptv.ui.components.tvFocusHighlight
import com.gracetv.iptv.ui.theme.GraceGreen
import com.gracetv.iptv.ui.theme.GraceRed
import com.gracetv.iptv.ui.theme.GraceYellow
import com.gracetv.iptv.ui.theme.GraceYellowContainer
import com.gracetv.iptv.utils.UrlValidator

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenPlaylists: () -> Unit,
    onOpenLiveTv: () -> Unit,
    onOpenMovies: () -> Unit,
    onOpenSeries: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenEpg: () -> Unit,
    onOpenMovieDetail: (Long) -> Unit,
    onOpenSeriesDetail: (Long) -> Unit,
    onPlayProgress: (PlaybackProgressEntity) -> Unit,
    onPlayHistory: (WatchHistoryEntity) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val active = state.activePlaylist

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Hero Welcome & Active Playlist Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner),
                        contentDescription = "Grace TV Studio Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(22.dp))
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.55f),
                                        Color(0xFF0E1013).copy(alpha = 0.94f)
                                    )
                                )
                            )
                    )
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Welcome to Grace TV",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (active != null) {
                                        "Active Playlist: ${active.name}"
                                    } else {
                                        "No active playlist configured yet"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (active != null) {
                                ConnectionStatusBadge(status = active.status)
                            }
                        }

                        if (active != null) {
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                StatPill(label = "Channels", value = active.channelCount.toString(), accent = GraceRed)
                                StatPill(label = "Movies", value = active.movieCount.toString(), accent = GraceYellow)
                                StatPill(label = "Series", value = active.seriesCount.toString(), accent = GraceGreen)
                            }

                            val isHttp = UrlValidator.isInsecureHttp(active.serverUrl.ifBlank { active.m3uUrl })
                            if (isHttp) {
                                Spacer(Modifier.height(12.dp))
                                Surface(
                                    color = GraceYellowContainer.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, GraceYellow.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.WarningAmber,
                                            contentDescription = null,
                                            tint = GraceYellow,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Connected over HTTP. Prefer HTTPS when supported by your provider.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = GraceYellow
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onOpenPlaylists,
                                colors = ButtonDefaults.buttonColors(containerColor = GraceRed),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("home_add_playlist_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (active == null) "Add Playlist" else "Manage Playlists")
                            }
                            if (active != null) {
                                OutlinedButton(
                                    onClick = onOpenLiveTv,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, GraceYellow.copy(alpha = 0.7f)),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("Watch Live", color = GraceYellow)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Shortcuts Grid
        item {
            Text(
                text = "Quick Access",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ShortcutTile(
                        title = "Live TV",
                        subtitle = "${active?.channelCount ?: 0} channels",
                        icon = Icons.Filled.LiveTv,
                        accent = GraceRed,
                        onClick = onOpenLiveTv,
                        modifier = Modifier.weight(1f).testTag("shortcut_live_tv")
                    )
                    ShortcutTile(
                        title = "Movies",
                        subtitle = "${active?.movieCount ?: 0} titles",
                        icon = Icons.Filled.Movie,
                        accent = GraceYellow,
                        onClick = onOpenMovies,
                        modifier = Modifier.weight(1f).testTag("shortcut_movies")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ShortcutTile(
                        title = "Series",
                        subtitle = "${active?.seriesCount ?: 0} shows",
                        icon = Icons.Filled.Tv,
                        accent = GraceGreen,
                        onClick = onOpenSeries,
                        modifier = Modifier.weight(1f).testTag("shortcut_series")
                    )
                    ShortcutTile(
                        title = "Favorites",
                        subtitle = "Saved library",
                        icon = Icons.Filled.Favorite,
                        accent = GraceRed,
                        onClick = onOpenFavorites,
                        modifier = Modifier.weight(1f).testTag("shortcut_favorites")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ShortcutTile(
                        title = "TV Guide (EPG)",
                        subtitle = "Schedule & timeline",
                        icon = Icons.Filled.CalendarMonth,
                        accent = GraceYellow,
                        onClick = onOpenEpg,
                        modifier = Modifier.weight(1f).testTag("shortcut_epg")
                    )
                    ShortcutTile(
                        title = "Playlists",
                        subtitle = "${state.allPlaylists.size} configured",
                        icon = Icons.Filled.PlaylistPlay,
                        accent = GraceGreen,
                        onClick = onOpenPlaylists,
                        modifier = Modifier.weight(1f).testTag("shortcut_playlists")
                    )
                }
            }
        }

        // Empty state when no playlist exists
        if (active == null) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    EmptyStateView(
                        icon = Icons.Filled.PlaylistAdd,
                        title = "No IPTV Playlist Added",
                        subtitle = stringResource(R.string.player_only_notice),
                        actionLabel = "Add Your First Playlist",
                        actionTestTag = "empty_add_playlist_btn",
                        onAction = onOpenPlaylists
                    )
                }
            }
        }

        // Continue Watching
        if (state.continueWatching.isNotEmpty()) {
            item {
                SectionHeader("Continue Watching")
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.continueWatching, key = { it.itemKey }) { item ->
                        ContinueWatchingCard(item = item, onClick = { onPlayProgress(item) })
                    }
                }
            }
        }

        // Recently Watched
        if (state.watchHistory.isNotEmpty()) {
            item {
                SectionHeader("Recently Watched")
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.watchHistory, key = { it.itemKey }) { item ->
                        PosterMediaCard(
                            title = item.title,
                            subtitle = item.subtitle.ifBlank { item.itemType.name },
                            imageUrl = item.imageUrl,
                            badgeText = if (item.itemType == ContentType.LIVE) "LIVE" else null,
                            fallbackIcon = if (item.itemType == ContentType.LIVE) Icons.Filled.LiveTv else Icons.Filled.Movie,
                            onClick = { onPlayHistory(item) },
                            modifier = Modifier.width(146.dp)
                        )
                    }
                }
            }
        }

        // Recently Added Movies
        if (state.recentMovies.isNotEmpty()) {
            item {
                SectionHeader("Recently Added Movies", actionText = "See All", onAction = onOpenMovies)
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.recentMovies, key = { it.id }) { movie: MovieEntity ->
                        PosterMediaCard(
                            title = movie.name,
                            subtitle = movie.categoryName,
                            imageUrl = movie.posterUrl,
                            badgeText = movie.rating.takeIf { it.isNotBlank() },
                            fallbackIcon = Icons.Filled.Movie,
                            onClick = { onOpenMovieDetail(movie.id) },
                            modifier = Modifier.width(142.dp)
                        )
                    }
                }
            }
        }

        // Recently Added Series
        if (state.recentSeries.isNotEmpty()) {
            item {
                SectionHeader("Recently Added Series", actionText = "See All", onAction = onOpenSeries)
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.recentSeries, key = { it.id }) { series: SeriesEntity ->
                        PosterMediaCard(
                            title = series.name,
                            subtitle = series.categoryName,
                            imageUrl = series.coverUrl,
                            badgeText = series.rating.takeIf { it.isNotBlank() },
                            fallbackIcon = Icons.Filled.Tv,
                            onClick = { onOpenSeriesDetail(series.id) },
                            modifier = Modifier.width(142.dp)
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun StatPill(label: String, value: String, accent: Color) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accent)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ShortcutTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.tvFocusHighlight(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accent.copy(alpha = 0.16f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    item: PlaybackProgressEntity,
    onClick: () -> Unit
) {
    val progress = if (item.durationMs > 0) (item.positionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .width(210.dp)
            .tvFocusHighlight(16.dp)
    ) {
        Column {
            MediaImage(
                url = item.imageUrl,
                contentDescription = item.title,
                fallbackIcon = Icons.Filled.Movie,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp)
            )
            LinearProgressIndicator(
                progress = { progress },
                color = GraceRed,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            )
            Column(modifier = Modifier.padding(10.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    item.subtitle.ifBlank { "Resume playback" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        if (actionText != null && onAction != null) {
            OutlinedButton(
                onClick = onAction,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(actionText, style = MaterialTheme.typography.labelLarge, color = GraceYellow)
            }
        }
    }
}
