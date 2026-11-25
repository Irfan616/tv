package com.gracetv.iptv.ui.screens.series

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gracetv.iptv.data.local.entity.EpisodeEntity
import com.gracetv.iptv.data.local.entity.SeriesEntity
import com.gracetv.iptv.data.repository.ContentRepository
import com.gracetv.iptv.domain.model.ContentType
import com.gracetv.iptv.ui.components.EmptyStateView
import com.gracetv.iptv.ui.components.MediaImage
import com.gracetv.iptv.ui.components.tvFocusHighlight
import com.gracetv.iptv.ui.theme.GraceGreen
import com.gracetv.iptv.ui.theme.GraceRed
import com.gracetv.iptv.ui.theme.GraceYellow
import com.gracetv.iptv.ui.theme.GraceYellowContainer

@Composable
fun SeriesDetailScreen(
    seriesId: Long,
    viewModel: SeriesViewModel,
    onBack: () -> Unit,
    onPlayEpisode: (SeriesEntity, EpisodeEntity) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val series by viewModel.observeSeries(seriesId).collectAsStateWithLifecycle(initialValue = null)
    val currentSeries = series ?: return

    val episodes by viewModel.observeEpisodes(currentSeries.playlistId, currentSeries.remoteId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var isLoadingEpisodes by remember(seriesId) { mutableStateOf(true) }
    var loadError by remember(seriesId) { mutableStateOf<String?>(null) }

    LaunchedEffect(seriesId) {
        isLoadingEpisodes = true
        viewModel.refreshSeriesDetails(seriesId) { err ->
            isLoadingEpisodes = false
            loadError = err
        }
    }

    val seasons = remember(episodes) {
        episodes.map { it.seasonNumber }.distinct().sorted()
    }
    var selectedSeason by remember(seasons) {
        mutableIntStateOf(seasons.firstOrNull() ?: 1)
    }
    val visibleEpisodes = remember(episodes, selectedSeason) {
        episodes.filter { it.seasonNumber == selectedSeason }
    }

    val seriesFavKey = ContentRepository.buildItemKey(
        ContentType.SERIES,
        currentSeries.id,
        currentSeries.remoteId
    )
    val isSeriesFav = seriesFavKey in state.favoriteKeys

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
            ) {
                MediaImage(
                    url = currentSeries.coverUrl,
                    contentDescription = currentSeries.name,
                    fallbackIcon = Icons.Filled.Tv,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(currentSeries.name, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            listOfNotNull(
                                currentSeries.categoryName.takeIf { it.isNotBlank() },
                                currentSeries.releaseDate.takeIf { it.isNotBlank() },
                                currentSeries.rating.takeIf { it.isNotBlank() }?.let { "★ $it" }
                            ).joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = GraceYellow
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.toggleFavoriteSeries(currentSeries) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isSeriesFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite series",
                            tint = if (isSeriesFav) GraceRed else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                if (currentSeries.plot.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = currentSeries.plot,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (isLoadingEpisodes && episodes.isEmpty()) {
            item {
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    color = GraceRed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }

        if (seasons.isNotEmpty()) {
            item {
                Spacer(Modifier.height(16.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(seasons) { season ->
                        FilterChip(
                            selected = season == selectedSeason,
                            onClick = { selectedSeason = season },
                            label = { Text("Season $season") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GraceYellowContainer,
                                selectedLabelColor = GraceYellow
                            )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        if (!isLoadingEpisodes && visibleEpisodes.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Filled.Tv,
                    title = "No Episodes Found",
                    subtitle = loadError ?: "This series did not provide playable episodes from the server."
                )
            }
        } else {
            items(visibleEpisodes, key = { it.id }) { ep ->
                val epFavKey = ContentRepository.buildItemKey(ContentType.EPISODE, ep.id, ep.remoteId)
                val isEpFav = epFavKey in state.favoriteKeys
                EpisodeRow(
                    episode = ep,
                    isFavorite = isEpFav,
                    onClick = { onPlayEpisode(currentSeries, ep) },
                    onToggleFavorite = { viewModel.toggleFavoriteEpisode(ep, currentSeries.name) }
                )
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: EpisodeEntity,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .tvFocusHighlight(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MediaImage(
                url = episode.thumbnailUrl,
                contentDescription = episode.title,
                fallbackIcon = Icons.Filled.Tv,
                modifier = Modifier
                    .width(96.dp)
                    .height(62.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "E${episode.episodeNumber} • ${episode.title}",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (episode.duration.isNotBlank()) {
                    Text(
                        text = episode.duration,
                        style = MaterialTheme.typography.labelSmall,
                        color = GraceGreen
                    )
                }
                if (episode.plot.isNotBlank()) {
                    Text(
                        text = episode.plot,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite episode",
                    tint = if (isFavorite) GraceRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Filled.PlayArrow, contentDescription = "Play episode", tint = GraceYellow)
        }
    }
}
