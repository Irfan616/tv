package com.gracetv.iptv.ui.screens.movies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gracetv.iptv.ui.components.EmptyStateView
import com.gracetv.iptv.ui.components.ParentalPinPromptDialog
import com.gracetv.iptv.ui.components.PosterMediaCard
import com.gracetv.iptv.ui.theme.GraceYellow
import com.gracetv.iptv.ui.theme.GraceYellowContainer
import com.gracetv.iptv.utils.UrlValidator

@Composable
fun MoviesScreen(
    viewModel: MoviesViewModel,
    onOpenPlaylists: () -> Unit,
    onOpenMovieDetail: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        if (state.activePlaylist == null) {
            EmptyStateView(
                icon = Icons.Filled.Movie,
                title = "No Movies Available",
                subtitle = "Import an Xtream Codes or VOD-enabled M3U playlist to browse movies.",
                actionLabel = "Add Playlist",
                onAction = onOpenPlaylists,
                modifier = Modifier.fillMaxSize()
            )
            return
        }

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = viewModel::updateSearchQuery,
            placeholder = { Text("Search ${state.movies.size} movies...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("movies_search_input")
        )

        if (state.categories.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedCategoryId == null,
                        onClick = { viewModel.selectCategory(null) },
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GraceYellowContainer,
                            selectedLabelColor = GraceYellow
                        )
                    )
                }
                items(state.categories, key = { it.id }) { cat ->
                    val locked = state.preferences.parentalEnabled &&
                        state.preferences.hasParentalPin &&
                        cat.remoteId !in state.unlockedCategories &&
                        (cat.isAdult || UrlValidator.isAdultContent(cat.name, state.preferences.restrictedKeywords))
                    FilterChip(
                        selected = state.selectedCategoryId == cat.remoteId,
                        onClick = { viewModel.selectCategory(cat) },
                        label = { Text(cat.name) },
                        leadingIcon = if (locked) {
                            { Icon(Icons.Filled.Lock, contentDescription = "Locked", modifier = Modifier.size(14.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GraceYellowContainer,
                            selectedLabelColor = GraceYellow
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        if (state.movies.isEmpty()) {
            EmptyStateView(
                icon = Icons.Filled.Movie,
                title = "No Movies Found",
                subtitle = "Your current playlist or filter did not return any movie titles.",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 148.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.movies, key = { it.id }) { movie ->
                    PosterMediaCard(
                        title = movie.name,
                        subtitle = movie.categoryName,
                        imageUrl = movie.posterUrl,
                        badgeText = movie.rating.takeIf { it.isNotBlank() },
                        fallbackIcon = Icons.Filled.Movie,
                        onClick = { onOpenMovieDetail(movie.id) }
                    )
                }
            }
        }
    }

    if (state.pendingRestrictedCategory != null) {
        ParentalPinPromptDialog(
            errorMessage = state.pinError,
            onDismiss = viewModel::dismissPinPrompt,
            onSubmitPin = viewModel::submitPin
        )
    }
}
