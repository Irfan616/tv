package com.gracetv.iptv.ui.screens.epg

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gracetv.iptv.data.local.entity.ChannelEntity
import com.gracetv.iptv.data.local.entity.EpgProgrammeEntity
import com.gracetv.iptv.ui.components.EmptyStateView
import com.gracetv.iptv.ui.theme.GraceGreen
import com.gracetv.iptv.ui.theme.GraceGreenContainer
import com.gracetv.iptv.ui.theme.GraceRed
import com.gracetv.iptv.ui.theme.GraceYellow
import com.gracetv.iptv.ui.theme.GraceYellowContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EpgScreen(
    initialChannelId: Long,
    viewModel: EpgViewModel,
    onPlayChannel: (ChannelEntity) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var urlInput by remember(state.configuredUrl) { mutableStateOf(state.configuredUrl) }

    LaunchedEffect(initialChannelId) {
        if (initialChannelId > 0L) {
            viewModel.selectChannel(initialChannelId)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // EPG Source Configuration Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("XMLTV TV Guide (EPG)", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "${state.totalProgrammes} cached",
                        style = MaterialTheme.typography.labelMedium,
                        color = GraceGreen
                    )
                }

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("XMLTV EPG URL") },
                    placeholder = { Text("https://provider.example/xmltv.xml") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("epg_url_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { viewModel.refreshEpg(urlInput) },
                        enabled = !state.isRefreshing && urlInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = GraceRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("epg_refresh_button")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (state.isRefreshing) "Syncing..." else "Load / Refresh EPG")
                    }
                    state.selectedChannel?.let { ch ->
                        OutlinedButton(
                            onClick = { onPlayChannel(ch) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = GraceYellow)
                            Spacer(Modifier.width(4.dp))
                            Text("Watch", color = GraceYellow)
                        }
                    }
                }

                if (state.isRefreshing) {
                    LinearProgressIndicator(color = GraceYellow, modifier = Modifier.fillMaxWidth())
                }
                state.errorMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = GraceRed)
                }
                state.statusMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = GraceGreen)
                }
            }
        }

        if (state.configuredUrl.isBlank() && state.totalProgrammes == 0) {
            EmptyStateView(
                icon = Icons.Filled.CalendarMonth,
                title = "No EPG Source Configured",
                subtitle = "Enter an XMLTV URL above or import a playlist with a url-tvg header / Xtream XMLTV feed to view programme schedules.",
                modifier = Modifier.weight(1f)
            )
            return
        }

        // Channel selector strip
        if (state.channelsWithTvg.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.channelsWithTvg, key = { it.id }) { ch ->
                    FilterChip(
                        selected = state.selectedChannel?.id == ch.id,
                        onClick = { viewModel.selectChannel(ch.id) },
                        label = { Text(ch.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GraceYellowContainer,
                            selectedLabelColor = GraceYellow
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        val now = System.currentTimeMillis()
        val current = state.channelProgrammes.firstOrNull { it.startTimeMs <= now && it.endTimeMs > now }
        val next = state.channelProgrammes.firstOrNull { it.startTimeMs > now }

        if (state.channelProgrammes.isEmpty()) {
            EmptyStateView(
                icon = Icons.Filled.CalendarMonth,
                title = "No Programmes Matched",
                subtitle = state.selectedChannel?.let {
                    "No XMLTV programmes matched tvg-id \"${it.tvgId.ifBlank { it.name }}\"."
                } ?: "Select a channel with EPG data.",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (current != null || next != null) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            current?.let {
                                NowNextCard(
                                    badge = "ON NOW",
                                    programme = it,
                                    accent = GraceGreen,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            next?.let {
                                NowNextCard(
                                    badge = "UP NEXT",
                                    programme = it,
                                    accent = GraceYellow,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        "Programme Timeline",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                items(state.channelProgrammes, key = { it.id }) { prog ->
                    ProgrammeTimelineRow(programme = prog, isNow = prog.startTimeMs <= now && prog.endTimeMs > now)
                }
            }
        }
    }
}

@Composable
private fun NowNextCard(
    badge: String,
    programme: EpgProgrammeEntity,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(badge, style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(programme.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${formatLocalTime(programme.startTimeMs)} - ${formatLocalTime(programme.endTimeMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProgrammeTimelineRow(
    programme: EpgProgrammeEntity,
    isNow: Boolean
) {
    val totalMs = (programme.endTimeMs - programme.startTimeMs).coerceAtLeast(1L)
    val elapsedMs = (System.currentTimeMillis() - programme.startTimeMs).coerceIn(0L, totalMs)
    val progress = elapsedMs.toFloat() / totalMs.toFloat()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isNow) GraceGreenContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isNow) GraceGreen.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${formatLocalTime(programme.startTimeMs)} – ${formatLocalTime(programme.endTimeMs)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isNow) GraceGreen else GraceYellow
                )
                if (programme.category.isNotBlank()) {
                    Text(
                        text = programme.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(programme.title, style = MaterialTheme.typography.titleSmall)
            if (programme.description.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    programme.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isNow) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    color = GraceGreen,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun formatLocalTime(millis: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
}
