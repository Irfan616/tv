package com.gracetv.iptv.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gracetv.iptv.data.local.PreferencesManager
import com.gracetv.iptv.data.local.entity.SearchHistoryEntity
import com.gracetv.iptv.data.repository.ContentRepository
import com.gracetv.iptv.data.repository.GlobalSearchResult
import com.gracetv.iptv.data.repository.PlaylistRepository
import com.gracetv.iptv.utils.UrlValidator
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val hasActivePlaylist: Boolean = false,
    val results: GlobalSearchResult = GlobalSearchResult(),
    val recentSearches: List<SearchHistoryEntity> = emptyList(),
    val isSearching: Boolean = false
)

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val playlistRepository: PlaylistRepository,
    private val contentRepository: ContentRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")

    private val resultsFlow = combine(
        playlistRepository.observeDefaultPlaylist(),
        queryFlow.debounce(180),
        preferencesManager.preferences
    ) { playlist, query, prefs ->
        Triple(playlist, query, prefs)
    }.flatMapLatest { (playlist, query, prefs) ->
        flow {
            if (playlist == null || query.isBlank()) {
                emit(GlobalSearchResult())
            } else {
                val raw = contentRepository.searchLocal(playlist.id, query)
                val filtered = if (prefs.parentalEnabled && prefs.hasParentalPin) {
                    raw.copy(
                        channels = raw.channels.filterNot {
                            it.isAdult || UrlValidator.isAdultContent(it.categoryName, prefs.restrictedKeywords)
                        },
                        movies = raw.movies.filterNot {
                            it.isAdult || UrlValidator.isAdultContent(it.categoryName, prefs.restrictedKeywords)
                        },
                        series = raw.series.filterNot {
                            it.isAdult || UrlValidator.isAdultContent(it.categoryName, prefs.restrictedKeywords)
                        },
                        categories = raw.categories.filterNot {
                            it.isAdult || UrlValidator.isAdultContent(it.name, prefs.restrictedKeywords)
                        }
                    )
                } else {
                    raw
                }
                emit(filtered)
            }
        }
    }

    val uiState: StateFlow<SearchUiState> = combine(
        queryFlow,
        playlistRepository.observeDefaultPlaylist(),
        resultsFlow,
        contentRepository.observeSearchHistory()
    ) { query, playlist, results, history ->
        SearchUiState(
            query = query,
            hasActivePlaylist = playlist != null,
            results = results,
            recentSearches = history,
            isSearching = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState()
    )

    fun updateQuery(newQuery: String) {
        queryFlow.value = newQuery
    }

    fun submitQuery(query: String = queryFlow.value) {
        queryFlow.value = query
        viewModelScope.launch {
            contentRepository.saveSearchQuery(query)
        }
    }

    fun clearQuery() {
        queryFlow.value = ""
    }

    fun deleteHistoryEntry(query: String) {
        viewModelScope.launch {
            contentRepository.deleteSearchQuery(query)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            contentRepository.clearSearchHistory()
        }
    }

    class Factory(
        private val playlistRepository: PlaylistRepository,
        private val contentRepository: ContentRepository,
        private val preferencesManager: PreferencesManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SearchViewModel(playlistRepository, contentRepository, preferencesManager) as T
    }
}
