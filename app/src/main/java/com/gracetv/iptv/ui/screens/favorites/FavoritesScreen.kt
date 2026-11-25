package com.gracetv.iptv.ui.screens.favorites

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gracetv.iptv.data.local.entity.FavoriteEntity
import com.gracetv.iptv.domain.model.ContentType
import com.gracetv.iptv.ui.components.EmptyStateView
import com.gracetv.iptv.ui.components.MediaImage
import com.gracetv.iptv.ui.components.tvFocusHighlight
import com.gracetv.iptv.ui.theme.GraceGreen
import com.gracetv.iptv.ui.theme.GraceRed
import com.gracetv.iptv.ui.theme.GraceYellow
import com.gracetv.iptv.ui.theme.GraceYellowContainer

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onOpenFavorite: (FavoriteEntity) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("Favorites", style = MaterialTheme.typography.headlineMedium)
            Text(
                "${state.totalCount} saved across Live TV, Movies & Series",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val filters: List<Pair<String, ContentType?>> = listOf(
            "All" to null,
            "Live TV" to ContentType.LIVE,
            "Movies" to ContentType.MOVIE,
            "Series" to ContentType.SERIES,
            "Episodes" to ContentType.EPISODE
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters) { (label, type) ->
                FilterChip(
                    selected = state.selectedFilter == type,
                    onClick = { viewModel.setFilter(type) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GraceYellowContainer,
                        selectedLabelColor = GraceYellow
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (state.favorites.isEmpty()) {
            EmptyStateView(
                icon = Icons.Filled.Favorite,
                title = "No Favorites Yet",
                subtitle = "Tap the heart icon on any Live TV channel, movie, series, or episode to keep it here.",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.favorites, key = { it.itemKey }) { fav ->
                    val icon = when (fav.itemType) {
                        ContentType.LIVE -> Icons.Filled.LiveTv
                        ContentType.MOVIE -> Icons.Filled.Movie
                        else -> Icons.Filled.Tv
                    }
                    Surface(
                        onClick = { onOpenFavorite(fav) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvFocusHighlight(16.dp)
                            .testTag("favorite_item_${fav.itemId}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MediaImage(
                                url = fav.imageUrl,
                                contentDescription = fav.title,
                                fallbackIcon = icon,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = fav.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${fav.itemType.name} • ${fav.subtitle.ifBlank { "Saved" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GraceGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { viewModel.removeFavorite(fav.itemKey) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Filled.DeleteOutline,
                                    contentDescription = "Remove from favorites",
                                    tint = GraceRed
                                )
                            }
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Open", tint = GraceYellow)
                        }
                    }
                }
            }
        }
    }
}
