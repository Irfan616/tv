package com.gracetv.iptv.ui.screens.playlist

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gracetv.iptv.data.local.entity.PlaylistEntity
import com.gracetv.iptv.data.repository.PlaylistRepository
import com.gracetv.iptv.domain.model.ImportProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaylistUiState(
    val playlists: List<PlaylistEntity> = emptyList(),
    val isImporting: Boolean = false,
    val importProgress: ImportProgress? = null,
    val errorMessage: String? = null,
    val statusMessage: String? = null
)

class PlaylistViewModel(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val localState = MutableStateFlow(PlaylistUiState())

    val uiState: StateFlow<PlaylistUiState> = combine(
        playlistRepository.observePlaylists(),
        localState
    ) { playlists, local ->
        local.copy(playlists = playlists)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlaylistUiState()
    )

    fun importXtream(
        existingId: Long? = null,
        name: String,
        serverUrl: String,
        username: String,
        password: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            localState.update {
                it.copy(
                    isImporting = true,
                    importProgress = ImportProgress("Connecting to Xtream server...", 0.05f),
                    errorMessage = null,
                    statusMessage = null
                )
            }
            val result = playlistRepository.importXtreamPlaylist(
                existingId = existingId,
                name = name,
                rawServerUrl = serverUrl,
                username = username,
                password = password
            ) { progress ->
                localState.update { it.copy(importProgress = progress) }
            }
            result.fold(
                onSuccess = { saved ->
                    localState.update {
                        it.copy(
                            isImporting = false,
                            importProgress = null,
                            statusMessage = "Imported \"${saved.name}\" (${saved.channelCount} live, ${saved.movieCount} movies, ${saved.seriesCount} series)."
                        )
                    }
                    onSuccess()
                },
                onFailure = { err ->
                    localState.update {
                        it.copy(
                            isImporting = false,
                            importProgress = null,
                            errorMessage = err.message ?: "Failed to import Xtream playlist."
                        )
                    }
                }
            )
        }
    }

    fun importM3uUrl(
        existingId: Long? = null,
        name: String,
        url: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            localState.update {
                it.copy(
                    isImporting = true,
                    importProgress = ImportProgress("Starting M3U download...", 0.05f),
                    errorMessage = null,
                    statusMessage = null
                )
            }
            val result = playlistRepository.importM3uUrl(
                existingId = existingId,
                name = name,
                m3uUrl = url
            ) { progress ->
                localState.update { it.copy(importProgress = progress) }
            }
            result.fold(
                onSuccess = { saved ->
                    localState.update {
                        it.copy(
                            isImporting = false,
                            importProgress = null,
                            statusMessage = "Imported \"${saved.name}\" with ${saved.channelCount + saved.movieCount} entries."
                        )
                    }
                    onSuccess()
                },
                onFailure = { err ->
                    localState.update {
                        it.copy(
                            isImporting = false,
                            importProgress = null,
                            errorMessage = err.message ?: "Failed to import M3U URL."
                        )
                    }
                }
            )
        }
    }

    fun importM3uFile(
        existingId: Long? = null,
        name: String,
        uri: Uri,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            localState.update {
                it.copy(
                    isImporting = true,
                    importProgress = ImportProgress("Reading local M3U file...", 0.10f),
                    errorMessage = null,
                    statusMessage = null
                )
            }
            val result = playlistRepository.importM3uFile(
                existingId = existingId,
                name = name,
                fileUri = uri
            ) { progress ->
                localState.update { it.copy(importProgress = progress) }
            }
            result.fold(
                onSuccess = { saved ->
                    localState.update {
                        it.copy(
                            isImporting = false,
                            importProgress = null,
                            statusMessage = "Imported \"${saved.name}\" with ${saved.channelCount + saved.movieCount} entries."
                        )
                    }
                    onSuccess()
                },
                onFailure = { err ->
                    localState.update {
                        it.copy(
                            isImporting = false,
                            importProgress = null,
                            errorMessage = err.message ?: "Failed to import M3U file."
                        )
                    }
                }
            )
        }
    }

    fun refreshPlaylist(playlistId: Long) {
        viewModelScope.launch {
            localState.update {
                it.copy(
                    isImporting = true,
                    importProgress = ImportProgress("Refreshing playlist...", 0.1f),
                    errorMessage = null,
                    statusMessage = null
                )
            }
            val result = playlistRepository.refreshPlaylist(playlistId) { progress ->
                localState.update { it.copy(importProgress = progress) }
            }
            result.fold(
                onSuccess = { saved ->
                    localState.update {
                        it.copy(
                            isImporting = false,
                            importProgress = null,
                            statusMessage = "Refreshed \"${saved.name}\"."
                        )
                    }
                },
                onFailure = { err ->
                    localState.update {
                        it.copy(
                            isImporting = false,
                            importProgress = null,
                            errorMessage = err.message ?: "Refresh failed."
                        )
                    }
                }
            )
        }
    }

    fun setDefault(playlistId: Long) {
        viewModelScope.launch {
            playlistRepository.setDefaultPlaylist(playlistId)
            localState.update { it.copy(statusMessage = "Default playlist updated.") }
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch {
            playlistRepository.updatePlaylistMetadata(playlistId, newName)
            localState.update { it.copy(statusMessage = "Playlist updated.") }
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
            localState.update { it.copy(statusMessage = "Playlist deleted.") }
        }
    }

    fun decryptPassword(playlist: PlaylistEntity): String =
        playlistRepository.decryptPassword(playlist)

    fun clearMessages() {
        localState.update { it.copy(errorMessage = null, statusMessage = null) }
    }

    class Factory(
        private val playlistRepository: PlaylistRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PlaylistViewModel(playlistRepository) as T
    }
}
