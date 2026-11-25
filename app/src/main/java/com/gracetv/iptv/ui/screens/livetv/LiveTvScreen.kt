package com.gracetv.iptv.ui.screens.livetv

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gracetv.iptv.data.local.entity.ChannelEntity
import com.gracetv.iptv.data.local.entity.EpgProgrammeEntity
import com.gracetv.iptv.data.repository.ContentRepository
import com.gracetv.iptv.domain.model.ContentType
import com.gracetv.iptv.ui.components.EmptyStateView
import com.gracetv.iptv.ui.components.MediaImage
import com.gracetv.iptv.ui.components.ParentalPinPromptDialog
import com.gracetv.iptv.ui.components.tvFocusHighlight
import com.gracetv.iptv.ui.theme.GraceGreen
import com.gracetv.iptv.ui.theme.GraceRed
import com.gracetv.iptv.ui.theme.GraceYellow
import com.gracetv.iptv.ui.theme.GraceYellowContainer

@Composable
fun LiveTvScreen(
    viewModel: LiveTvViewModel,
    onOpenPlaylists: () -> Unit,
    onPlayChannel: (ChannelEntity) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var gridMode by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (state.activePlaylist == null) {
            EmptyStateView(
                icon = Icons.Filled.LiveTv,
                title = "No Live TV Playlist",
                subtitle = "Add an Xtream Codes or M3U playlist to browse and stream Live TV channels.",
                actionLabel = "Add Playlist",
                onAction = onOpenPlaylists,
                modifier = Modifier.fillMaxSize()
            )
            return
        }

        // Top search + grid toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                placeholder = { Text("Filter ${state.channels.size} live channels...") },
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
                    .weight(1f)
                    .testTag("live_tv_search_input")
            )
            IconButton(
                onClick = { gridMode = !gridMode },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("live_tv_layout_toggle")
            ) {
                Icon(
                    imageVector = if (gridMode) Icons.Filled.ViewList else Icons.Filled.GridView,
                    contentDescription = if (gridMode) "List view" else "TV grid view",
                    tint = GraceYellow
                )
            }
        }

        // Categories strip
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
                    val locked = viewModel.isCategoryRestricted(
                        cat,
                        state.preferences,
                        state.parentalUnlockedCategories
                    )
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

        if (state.channels.isEmpty()) {
            EmptyStateView(
                icon = Icons.Filled.LiveTv,
                title = "No Channels Match",
                subtitle = "Try clearing your category filter or search query.",
                modifier = Modifier.weight(1f)
            )
        } else if (gridMode) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 168.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.channels, key = { it.id }) { channel ->
                    val favKey = ContentRepository.buildItemKey(ContentType.LIVE, channel.id, channel.remoteId)
                    val isFav = favKey in state.favoriteKeys
                    val epg = state.currentProgrammesByTvgId[channel.tvgId.lowercase()]
                    ChannelGridCard(
                        channel = channel,
                        currentProgramme = epg,
                        isFavorite = isFav,
                        isLocked = viewModel.isChannelRestricted(channel, state.preferences, state.parentalUnlockedCategories),
                        onClick = { viewModel.requestChannelPlayback(channel, onPlayChannel) },
                        onToggleFavorite = { viewModel.toggleFavorite(channel) }
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.channels, key = { it.id }) { channel ->
                    val favKey = ContentRepository.buildItemKey(ContentType.LIVE, channel.id, channel.remoteId)
                    val isFav = favKey in state.favoriteKeys
                    val epg = state.currentProgrammesByTvgId[channel.tvgId.lowercase()]
                    ChannelListRow(
                        channel = channel,
                        currentProgramme = epg,
                        isFavorite = isFav,
                        isLocked = viewModel.isChannelRestricted(channel, state.preferences, state.parentalUnlockedCategories),
                        onClick = { viewModel.requestChannelPlayback(channel, onPlayChannel) },
                        onToggleFavorite = { viewModel.toggleFavorite(channel) }
                    )
                }
            }
        }
    }

    if (state.pendingRestrictedCategory != null || state.pendingRestrictedChannel != null) {
        ParentalPinPromptDialog(
            errorMessage = state.pinError,
            onDismiss = viewModel::dismissPinPrompt,
            onSubmitPin = { pin ->
                viewModel.submitParentalPin(pin, onChannelUnlocked = onPlayChannel)
            }
        )
    }
}

@Composable
private fun ChannelListRow(
    channel: ChannelEntity,
    currentProgramme: EpgProgrammeEntity?,
    isFavorite: Boolean,
    isLocked: Boolean,
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
            .tvFocusHighlight(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MediaImage(
                url = channel.logoUrl,
                contentDescription = channel.name,
                fallbackIcon = Icons.Filled.LiveTv,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isLocked) {
                        Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = GraceYellow, modifier = Modifier.size(15.dp))
                    }
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = currentProgramme?.title?.let { "Now: $it" } ?: channel.categoryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (currentProgramme != null) GraceGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                    tint = if (isFavorite) GraceRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = GraceYellow)
        }
    }
}

@Composable
private fun ChannelGridCard(
    channel: ChannelEntity,
    currentProgramme: EpgProgrammeEntity?,
    isFavorite: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.tvFocusHighlight(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                MediaImage(
                    url = channel.logoUrl,
                    contentDescription = channel.name,
                    fallbackIcon = Icons.Filled.LiveTv,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) GraceRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isLocked) {
                    Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = GraceYellow, modifier = Modifier.size(14.dp))
                }
                Text(channel.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = currentProgramme?.title ?: channel.categoryName,
                style = MaterialTheme.typography.bodySmall,
                color = if (currentProgramme != null) GraceGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
