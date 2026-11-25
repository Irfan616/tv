package com.gracetv.iptv.ui.screens.playlist

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gracetv.iptv.data.local.entity.PlaylistEntity
import com.gracetv.iptv.domain.model.PlaylistType
import com.gracetv.iptv.ui.components.ConnectionStatusBadge
import com.gracetv.iptv.ui.components.EmptyStateView
import com.gracetv.iptv.ui.components.tvFocusHighlight
import com.gracetv.iptv.ui.theme.GraceGreen
import com.gracetv.iptv.ui.theme.GraceGreenContainer
import com.gracetv.iptv.ui.theme.GraceRed
import com.gracetv.iptv.ui.theme.GraceRedContainer
import com.gracetv.iptv.ui.theme.GraceYellow
import com.gracetv.iptv.ui.theme.GraceYellowContainer
import com.gracetv.iptv.utils.UrlValidator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlaylistScreen(
    viewModel: PlaylistViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showImportDialog by remember { mutableStateOf(false) }
    var editingPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    var deletingPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header action bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Playlists", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Manage Xtream Codes & M3U playlists",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = {
                    editingPlaylist = null
                    showImportDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = GraceRed),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("add_playlist_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add")
            }
        }

        // Import progress banner
        if (state.isImporting && state.importProgress != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        state.importProgress!!.stage,
                        style = MaterialTheme.typography.titleSmall,
                        color = GraceYellow
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { state.importProgress!!.progressFraction },
                        color = GraceGreen,
                        trackColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Status / Error banners
        state.errorMessage?.let { err ->
            Surface(
                color = GraceRedContainer,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GraceRed.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFDAD6),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.clearMessages() }) {
                        Text("Dismiss", color = GraceYellow)
                    }
                }
            }
        }

        state.statusMessage?.let { msg ->
            Surface(
                color = GraceGreenContainer,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GraceGreen.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = GraceGreen, modifier = Modifier.size(18.dp))
                        Text(msg, style = MaterialTheme.typography.bodySmall, color = Color(0xFFA7F5BA))
                    }
                    TextButton(onClick = { viewModel.clearMessages() }) {
                        Text("OK", color = GraceGreen)
                    }
                }
            }
        }

        if (state.playlists.isEmpty() && !state.isImporting) {
            EmptyStateView(
                icon = Icons.Filled.PlaylistAdd,
                title = "No Playlists Saved",
                subtitle = "Import an Xtream Codes API account, paste an M3U/M3U8 link, or select a local M3U file.",
                actionLabel = "Import Playlist",
                actionTestTag = "empty_import_playlist_btn",
                onAction = { showImportDialog = true },
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.playlists, key = { it.id }) { playlist ->
                    PlaylistCard(
                        playlist = playlist,
                        isImporting = state.isImporting,
                        onSetDefault = { viewModel.setDefault(playlist.id) },
                        onRefresh = { viewModel.refreshPlaylist(playlist.id) },
                        onEdit = {
                            editingPlaylist = playlist
                            showImportDialog = true
                        },
                        onDelete = { deletingPlaylist = playlist }
                    )
                }
            }
        }
    }

    if (showImportDialog) {
        AddOrEditPlaylistDialog(
            editing = editingPlaylist,
            decryptPassword = { viewModel.decryptPassword(it) },
            isImporting = state.isImporting,
            onDismiss = { showImportDialog = false },
            onSubmitXtream = { name, url, user, pass ->
                viewModel.importXtream(
                    existingId = editingPlaylist?.id,
                    name = name,
                    serverUrl = url,
                    username = user,
                    password = pass,
                    onSuccess = { showImportDialog = false }
                )
            },
            onSubmitM3uUrl = { name, url ->
                viewModel.importM3uUrl(
                    existingId = editingPlaylist?.id,
                    name = name,
                    url = url,
                    onSuccess = { showImportDialog = false }
                )
            },
            onSubmitM3uFile = { name, uri ->
                viewModel.importM3uFile(
                    existingId = editingPlaylist?.id,
                    name = name,
                    uri = uri,
                    onSuccess = { showImportDialog = false }
                )
            }
        )
    }

    deletingPlaylist?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingPlaylist = null },
            title = { Text("Delete Playlist?") },
            text = {
                Text("Remove \"${target.name}\" and all its cached channels, movies, and series from Grace TV?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePlaylist(target.id)
                        deletingPlaylist = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GraceRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingPlaylist = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PlaylistCard(
    playlist: PlaylistEntity,
    isImporting: Boolean,
    onSetDefault: () -> Unit,
    onRefresh: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val updatedText = remember(playlist.lastUpdatedMillis) {
        SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(Date(playlist.lastUpdatedMillis))
    }
    val typeLabel = when (playlist.type) {
        PlaylistType.XTREAM -> "Xtream Codes API"
        PlaylistType.M3U_URL -> "M3U / M3U8 URL"
        PlaylistType.M3U_FILE -> "Local M3U File"
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (playlist.isDefault) 1.5.dp else 1.dp,
            color = if (playlist.isDefault) GraceYellow.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusHighlight(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (playlist.isDefault) {
                            Surface(
                                color = GraceYellowContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "DEFAULT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GraceYellow,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "$typeLabel • Updated $updatedText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ConnectionStatusBadge(status = playlist.status)
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = "Live: ${playlist.channelCount}   •   Movies: ${playlist.movieCount}   •   Series: ${playlist.seriesCount}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!playlist.accountExpiry.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Expiry: ${playlist.accountExpiry}" +
                        (playlist.maxConnections?.let { " • Max Conn: $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = GraceGreen
                )
            }

            if (!playlist.errorMessage.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = playlist.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = GraceRed
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!playlist.isDefault) {
                    OutlinedButton(
                        onClick = onSetDefault,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = GraceYellow, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Set Default", color = GraceYellow)
                    }
                }
                OutlinedButton(
                    onClick = onRefresh,
                    enabled = !isImporting,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Refresh")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit playlist", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete playlist", tint = GraceRed)
                }
            }
        }
    }
}

@Composable
private fun AddOrEditPlaylistDialog(
    editing: PlaylistEntity?,
    decryptPassword: (PlaylistEntity) -> String,
    isImporting: Boolean,
    onDismiss: () -> Unit,
    onSubmitXtream: (String, String, String, String) -> Unit,
    onSubmitM3uUrl: (String, String) -> Unit,
    onSubmitM3uFile: (String, android.net.Uri) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember(editing) {
        mutableIntStateOf(
            when (editing?.type) {
                PlaylistType.M3U_URL -> 1
                PlaylistType.M3U_FILE -> 2
                else -> 0
            }
        )
    }

    var name by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
    var serverUrl by remember(editing) { mutableStateOf(editing?.serverUrl.orEmpty()) }
    var username by remember(editing) { mutableStateOf(editing?.username.orEmpty()) }
    var password by remember(editing) {
        mutableStateOf(if (editing != null) decryptPassword(editing) else "")
    }
    var m3uUrl by remember(editing) { mutableStateOf(editing?.m3uUrl.orEmpty()) }
    var pickedUri by remember(editing) {
        mutableStateOf(editing?.fileUri?.takeIf { it.isNotBlank() }?.let { android.net.Uri.parse(it) })
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            pickedUri = uri
            if (name.isBlank()) {
                name = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Local M3U"
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isImporting) onDismiss() },
        title = { Text(if (editing == null) "Import IPTV Playlist" else "Edit Playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    indicator = { positions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(positions[selectedTab]),
                            color = GraceRed
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Xtream") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("M3U URL") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("M3U File") }
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist Name") },
                    placeholder = { Text("e.g. Living Room IPTV") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playlist_name_input")
                )

                when (selectedTab) {
                    0 -> {
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            label = { Text("Server URL") },
                            placeholder = { Text("https://example.com:8080") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("xtream_server_input")
                        )
                        if (UrlValidator.isInsecureHttp(serverUrl)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = GraceYellow, modifier = Modifier.size(16.dp))
                                Text(
                                    "Warning: HTTP is unencrypted. Use HTTPS if supported.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GraceYellow
                                )
                            }
                        }
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("xtream_username_input")
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("xtream_password_input")
                        )
                    }
                    1 -> {
                        OutlinedTextField(
                            value = m3uUrl,
                            onValueChange = { m3uUrl = it },
                            label = { Text("M3U / M3U8 URL") },
                            placeholder = { Text("https://provider.example/playlist.m3u8") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("m3u_url_input")
                        )
                        if (UrlValidator.isInsecureHttp(m3uUrl)) {
                            Text(
                                "Warning: HTTP playlist URL is unencrypted.",
                                style = MaterialTheme.typography.bodySmall,
                                color = GraceYellow
                            )
                        }
                    }
                    2 -> {
                        OutlinedButton(
                            onClick = { filePicker.launch(arrayOf("*/*")) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("pick_m3u_file_button")
                        ) {
                            Icon(Icons.Filled.FolderOpen, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (pickedUri == null) "Select M3U / M3U8 File" else "Change Selected File")
                        }
                        pickedUri?.let { uri ->
                            Text(
                                text = "Selected: ${uri.lastPathSegment ?: uri}",
                                style = MaterialTheme.typography.bodySmall,
                                color = GraceGreen
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isImporting && name.isNotBlank() && when (selectedTab) {
                    0 -> serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
                    1 -> m3uUrl.isNotBlank()
                    else -> pickedUri != null
                },
                onClick = {
                    when (selectedTab) {
                        0 -> onSubmitXtream(name, serverUrl, username, password)
                        1 -> onSubmitM3uUrl(name, m3uUrl)
                        else -> pickedUri?.let { onSubmitM3uFile(name, it) }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GraceRed),
                modifier = Modifier.testTag("submit_playlist_button")
            ) {
                Text(if (isImporting) "Importing..." else "Connect & Import")
            }
        },
        dismissButton = {
            TextButton(enabled = !isImporting, onClick = onDismiss) { Text("Cancel") }
        }
    )
}
