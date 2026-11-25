package com.gracetv.iptv.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gracetv.iptv.data.local.PreferencesManager
import com.gracetv.iptv.data.local.UserPreferences
import com.gracetv.iptv.data.local.entity.ChannelEntity
import com.gracetv.iptv.data.local.entity.MovieEntity
import com.gracetv.iptv.data.local.entity.PlaybackProgressEntity
import com.gracetv.iptv.data.local.entity.PlaylistEntity
import com.gracetv.iptv.data.local.entity.SeriesEntity
import com.gracetv.iptv.data.local.entity.WatchHistoryEntity
import com.gracetv.iptv.data.repository.ContentRepository
import com.gracetv.iptv.data.repository.PlaylistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val activePlaylist: PlaylistEntity? = null,
    val allPlaylists: List<PlaylistEntity> = emptyList(),
    val watchHistory: List<WatchHistoryEntity> = emptyList(),
    val continueWatching: List<PlaybackProgressEntity> = emptyList(),
    val recentMovies: List<MovieEntity> = emptyList(),
    val recentSeries: List<SeriesEntity> = emptyList(),
    val preferences: UserPreferences = UserPreferences()
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val playlistRepository: PlaylistRepository,
    private val contentRepository: ContentRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val activePlaylistFlow = playlistRepository.observeDefaultPlaylist()

    private val recentMoviesFlow = activePlaylistFlow.flatMapLatest { playlist ->
        if (playlist == null) flowOf(emptyList())
        else contentRepository.observeRecentlyAddedMovies(playlist.id)
    }

    private val recentSeriesFlow = activePlaylistFlow.flatMapLatest { playlist ->
        if (playlist == null) flowOf(emptyList())
        else contentRepository.observeRecentlyAddedSeries(playlist.id)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        activePlaylistFlow,
        playlistRepository.observePlaylists(),
        contentRepository.observeWatchHistory(),
        contentRepository.observeContinueWatching(),
        recentMoviesFlow,
        recentSeriesFlow,
        preferencesManager.preferences
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        HomeUiState(
            activePlaylist = values[0] as PlaylistEntity?,
            allPlaylists = values[1] as List<PlaylistEntity>,
            watchHistory = values[2] as List<WatchHistoryEntity>,
            continueWatching = values[3] as List<PlaybackProgressEntity>,
            recentMovies = values[4] as List<MovieEntity>,
            recentSeries = values[5] as List<SeriesEntity>,
            preferences = values[6] as UserPreferences
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    suspend fun getAutoStartChannel(): ChannelEntity? {
        val prefs = uiState.value.preferences
        if (!prefs.autoStartLastChannel || prefs.lastChannelId <= 0L) return null
        return contentRepository.getChannelById(prefs.lastChannelId)
    }

    class Factory(
        private val playlistRepository: PlaylistRepository,
        private val contentRepository: ContentRepository,
        private val preferencesManager: PreferencesManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(playlistRepository, contentRepository, preferencesManager) as T
    }
}
