package com.gracetv.iptv.ui.screens.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gracetv.iptv.data.local.PreferencesManager
import com.gracetv.iptv.data.local.UserPreferences
import com.gracetv.iptv.data.local.entity.CategoryEntity
import com.gracetv.iptv.data.local.entity.EpisodeEntity
import com.gracetv.iptv.data.local.entity.PlaylistEntity
import com.gracetv.iptv.data.local.entity.SeriesEntity
import com.gracetv.iptv.data.repository.ContentRepository
import com.gracetv.iptv.data.repository.PlaylistRepository
import com.gracetv.iptv.domain.model.ContentType
import com.gracetv.iptv.utils.UrlValidator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SeriesUiState(
    val activePlaylist: PlaylistEntity? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val seriesList: List<SeriesEntity> = emptyList(),
    val favoriteKeys: Set<String> = emptySet(),
    val preferences: UserPreferences = UserPreferences(),
    val unlockedCategories: Set<String> = emptySet(),
    val pendingRestrictedCategory: CategoryEntity? = null,
    val pinError: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class SeriesViewModel(
    private val playlistRepository: PlaylistRepository,
    private val contentRepository: ContentRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val searchQuery = MutableStateFlow("")
    private val unlockedCategories = MutableStateFlow<Set<String>>(emptySet())
    private val pendingCategory = MutableStateFlow<CategoryEntity?>(null)
    private val pinError = MutableStateFlow<String?>(null)

    private val activePlaylistFlow = playlistRepository.observeDefaultPlaylist()

    private val categoriesFlow = activePlaylistFlow.flatMapLatest { playlist ->
        if (playlist == null) flowOf(emptyList())
        else contentRepository.observeCategories(playlist.id, ContentType.SERIES)
    }

    private val seriesFlow = activePlaylistFlow.flatMapLatest { playlist ->
        if (playlist == null) flowOf(emptyList())
        else contentRepository.observeSeriesList(playlist.id)
    }

    val uiState: StateFlow<SeriesUiState> = combine(
        activePlaylistFlow,
        categoriesFlow,
        seriesFlow,
        contentRepository.observeFavoriteKeys(),
        preferencesManager.preferences,
        selectedCategoryId,
        searchQuery,
        unlockedCategories,
        pendingCategory,
        pinError
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val playlist = arr[0] as PlaylistEntity?
        @Suppress("UNCHECKED_CAST")
        val categories = arr[1] as List<CategoryEntity>
        @Suppress("UNCHECKED_CAST")
        val allSeries = arr[2] as List<SeriesEntity>
        @Suppress("UNCHECKED_CAST")
        val favKeys = (arr[3] as List<String>).toSet()
        val prefs = arr[4] as UserPreferences
        val selCat = arr[5] as String?
        val query = arr[6] as String
        @Suppress("UNCHECKED_CAST")
        val unlocked = arr[7] as Set<String>
        val pendCat = arr[8] as CategoryEntity?
        val err = arr[9] as String?

        val filtered = allSeries.filter { s ->
            val matchesCat = selCat == null || s.categoryId == selCat
            val matchesQuery = query.isBlank() || s.name.contains(query, ignoreCase = true)
            val restricted = prefs.parentalEnabled && prefs.hasParentalPin &&
                s.categoryId !in unlocked &&
                (s.isAdult || UrlValidator.isAdultContent(s.categoryName, prefs.restrictedKeywords))
            matchesCat && matchesQuery && !restricted
        }

        SeriesUiState(
            activePlaylist = playlist,
            categories = categories,
            selectedCategoryId = selCat,
            searchQuery = query,
            seriesList = filtered,
            favoriteKeys = favKeys,
            preferences = prefs,
            unlockedCategories = unlocked,
            pendingRestrictedCategory = pendCat,
            pinError = err
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SeriesUiState()
    )

    fun selectCategory(category: CategoryEntity?) {
        if (category == null) {
            selectedCategoryId.value = null
            return
        }
        val state = uiState.value
        val restricted = state.preferences.parentalEnabled &&
            state.preferences.hasParentalPin &&
            category.remoteId !in state.unlockedCategories &&
            (category.isAdult || UrlValidator.isAdultContent(category.name, state.preferences.restrictedKeywords))
        if (restricted) {
            pendingCategory.value = category
            pinError.value = null
        } else {
            selectedCategoryId.value = category.remoteId
        }
    }

    fun submitPin(pin: String) {
        viewModelScope.launch {
            if (preferencesManager.verifyParentalPin(pin)) {
                pendingCategory.value?.let { cat ->
                    unlockedCategories.update { it + cat.remoteId }
                    selectedCategoryId.value = cat.remoteId
                    pendingCategory.value = null
                }
                pinError.value = null
            } else {
                pinError.value = "Incorrect PIN."
            }
        }
    }

    fun dismissPinPrompt() {
        pendingCategory.value = null
        pinError.value = null
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun observeSeries(seriesId: Long): Flow<SeriesEntity?> = contentRepository.observeSeries(seriesId)

    fun observeEpisodes(playlistId: Long, seriesRemoteId: String): Flow<List<EpisodeEntity>> =
        contentRepository.observeEpisodes(playlistId, seriesRemoteId)

    fun refreshSeriesDetails(seriesId: Long, onComplete: (String?) -> Unit = {}) {
        viewModelScope.launch {
            val result = contentRepository.refreshSeriesDetails(seriesId)
            onComplete(result.exceptionOrNull()?.message)
        }
    }

    fun toggleFavoriteSeries(series: SeriesEntity) {
        viewModelScope.launch {
            contentRepository.toggleFavorite(
                playlistId = series.playlistId,
                type = ContentType.SERIES,
                itemId = series.id,
                remoteId = series.remoteId,
                title = series.name,
                subtitle = series.categoryName,
                imageUrl = series.coverUrl,
                streamUrl = ""
            )
        }
    }

    fun toggleFavoriteEpisode(episode: EpisodeEntity, seriesName: String) {
        viewModelScope.launch {
            contentRepository.toggleFavorite(
                playlistId = episode.playlistId,
                type = ContentType.EPISODE,
                itemId = episode.id,
                remoteId = episode.remoteId,
                title = "$seriesName • S${episode.seasonNumber}E${episode.episodeNumber}",
                subtitle = episode.title,
                imageUrl = episode.thumbnailUrl,
                streamUrl = episode.streamUrl
            )
        }
    }

    class Factory(
        private val playlistRepository: PlaylistRepository,
        private val contentRepository: ContentRepository,
        private val preferencesManager: PreferencesManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SeriesViewModel(playlistRepository, contentRepository, preferencesManager) as T
    }
}
