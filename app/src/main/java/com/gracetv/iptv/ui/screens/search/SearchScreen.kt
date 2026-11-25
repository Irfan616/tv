package com.gracetv.iptv.ui.screens.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gracetv.iptv.data.local.entity.ChannelEntity
import com.gracetv.iptv.data.local.entity.MovieEntity
import com.gracetv.iptv.data.local.entity.SeriesEntity
import com.gracetv.iptv.ui.components.EmptyStateView
import com.gracetv.iptv.ui.components.MediaImage
import com.gracetv.iptv.ui.components.tvFocusHighlight
import com.gracetv.iptv.ui.theme.GraceGreen
import com.gracetv.iptv.ui.theme.GraceRed
import com.gracetv.iptv.ui.theme.GraceYellow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onPlayChannel: (ChannelEntity) -> Unit,
    onOpenMovieDetail: (MovieEntity) -> Unit,
    onOpenSeriesDetail: (SeriesEntity) -> Unit,
    onOpenPlaylists: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::updateQuery,
            placeholder = { Text("Search channels, movies, series, categories...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(
                        onClick = viewModel::clearQuery,
                        modifier = Modifier.testTag("search_clear_button")
                    ) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.submitQuery() }),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("global_search_input")
        )

        if (!state.hasActivePlaylist) {
            EmptyStateView(
                icon = Icons.Filled.Search,
                title = "No Playlist to Search",
                subtitle = "Import an IPTV playlist first to search channels, movies, and series.",
                actionLabel = "Add Playlist",
                onAction = onOpenPlaylists,
                modifier = Modifier.weight(1f)
            )
            return
        }

        if (state.query.isBlank()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                if (state.recentSearches.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Searches", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = viewModel::clearHistory) {
                            Text("Clear All", color = GraceRed)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.recentSearches.forEach { entry ->
                            AssistChip(
                                onClick = { viewModel.updateQuery(entry.query) },
                                label = { Text(entry.query) },
                                leadingIcon = {
                                    Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                } else {
                    EmptyStateView(
                        icon = Icons.Filled.Search,
                        title = "Search Your Library",
                        subtitle = "Type any channel name, movie, series, or category to see instant local results."
                    )
                }
            }
            return
        }

        val res = state.results
        val total = res.channels.size + res.movies.size + res.series.size + res.categories.size
        if (total == 0) {
            EmptyStateView(
                icon = Icons.Filled.Search,
                title = "No Results for \"${state.query}\"",
                subtitle = "Check spelling or try a shorter keyword.",
                modifier = Modifier.weight(1f)
            )
            return
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (res.channels.isNotEmpty()) {
                item { ResultSectionTitle("Live TV (${res.channels.size})") }
                items(res.channels, key = { "ch_${it.id}" }) { ch ->
                    SearchResultRow(
                        title = ch.name,
                        subtitle = ch.categoryName,
                        imageUrl = ch.logoUrl,
                        fallbackIcon = Icons.Filled.LiveTv,
                        onClick = {
                            viewModel.submitQuery()
                            onPlayChannel(ch)
                        }
                    )
                }
            }

            if (res.movies.isNotEmpty()) {
                item { ResultSectionTitle("Movies (${res.movies.size})") }
                items(res.movies, key = { "mv_${it.id}" }) { mv ->
                    SearchResultRow(
                        title = mv.name,
                        subtitle = mv.categoryName,
                        imageUrl = mv.posterUrl,
                        fallbackIcon = Icons.Filled.Movie,
                        onClick = {
                            viewModel.submitQuery()
                            onOpenMovieDetail(mv)
                        }
                    )
                }
            }

            if (res.series.isNotEmpty()) {
                item { ResultSectionTitle("Series (${res.series.size})") }
                items(res.series, key = { "sr_${it.id}" }) { sr ->
                    SearchResultRow(
                        title = sr.name,
                        subtitle = sr.categoryName,
                        imageUrl = sr.coverUrl,
                        fallbackIcon = Icons.Filled.Tv,
                        onClick = {
                            viewModel.submitQuery()
                            onOpenSeriesDetail(sr)
                        }
                    )
                }
            }

            if (res.categories.isNotEmpty()) {
                item { ResultSectionTitle("Categories (${res.categories.size})") }
                items(res.categories, key = { "cat_${it.id}" }) { cat ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.Category, contentDescription = null, tint = GraceGreen)
                            Column {
                                Text(cat.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    cat.contentType.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ResultSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = GraceYellow,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun SearchResultRow(
    title: String,
    subtitle: String,
    imageUrl: String,
    fallbackIcon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusHighlight(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MediaImage(
                url = imageUrl,
                contentDescription = title,
                fallbackIcon = fallbackIcon,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
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
            Icon(Icons.Filled.PlayArrow, contentDescription = "Open", tint = GraceYellow)
        }
    }
}
