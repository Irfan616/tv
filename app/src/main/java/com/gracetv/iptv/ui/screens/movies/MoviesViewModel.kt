package com.gracetv.iptv.ui.screens.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gracetv.iptv.data.local.PreferencesManager
import com.gracetv.iptv.data.local.UserPreferences
import com.gracetv.iptv.data.local.entity.CategoryEntity
import com.gracetv.iptv.data.local.entity.MovieEntity
import com.gracetv.iptv.data.local.entity.PlaylistEntity
import com.gracetv.iptv.data.repository.ContentRepository
import com.gracetv.iptv.data.repository.PlaylistRepository
import com.gracetv.iptv.domain.model.ContentType
import com.gracetv.iptv.utils.UrlValidator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MoviesUiState(
    val activePlaylist: PlaylistEntity? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val movies: List<MovieEntity> = emptyList(),
    val favoriteKeys: Set<String> = emptySet(),
    val preferences: UserPreferences = UserPreferences(),
    val unlockedCategories: Set<String> = emptySet(),
    val pendingRestrictedCategory: CategoryEntity? = null,
    val pinError: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class MoviesViewModel(
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
        else contentRepository.observeCategories(playlist.id, ContentType.MOVIE)
    }

    private val moviesFlow = activePlaylistFlow.flatMapLatest { playlist ->
        if (playlist == null) flowOf(emptyList())
        else contentRepository.observeMovies(playlist.id)
    }

    val uiState: StateFlow<MoviesUiState> = combine(
        activePlaylistFlow,
        categoriesFlow,
        moviesFlow,
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
        val allMovies = arr[2] as List<MovieEntity>
        @Suppress("UNCHECKED_CAST")
        val favKeys = (arr[3] as List<String>).toSet()
        val prefs = arr[4] as UserPreferences
        val selCat = arr[5] as String?
        val query = arr[6] as String
        @Suppress("UNCHECKED_CAST")
        val unlocked = arr[7] as Set<String>
        val pendCat = arr[8] as CategoryEntity?
        val err = arr[9] as String?

        val filtered = allMovies.filter { m ->
            val matchesCat = selCat == null || m.categoryId == selCat
            val matchesQuery = query.isBlank() || m.name.contains(query, ignoreCase = true)
            val restricted = prefs.parentalEnabled && prefs.hasParentalPin &&
                m.categoryId !in unlocked &&
                (m.isAdult || UrlValidator.isAdultContent(m.categoryName, prefs.restrictedKeywords))
            matchesCat && matchesQuery && !restricted
        }

        MoviesUiState(
            activePlaylist = playlist,
            categories = categories,
            selectedCategoryId = selCat,
            searchQuery = query,
            movies = filtered,
            favoriteKeys = favKeys,
            preferences = prefs,
            unlockedCategories = unlocked,
            pendingRestrictedCategory = pendCat,
            pinError = err
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MoviesUiState()
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

    fun observeMovie(movieId: Long) = contentRepository.observeMovie(movieId)

    fun refreshMovieDetails(movieId: Long, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            contentRepository.refreshMovieDetails(movieId).onFailure {
                onError(it.message ?: "Could not fetch movie details.")
            }
        }
    }

    suspend fun getSavedPositionMs(movie: MovieEntity): Long =
        contentRepository.getSavedPositionMs(ContentType.MOVIE, movie.id, movie.remoteId)

    fun toggleFavorite(movie: MovieEntity) {
        viewModelScope.launch {
            contentRepository.toggleFavorite(
                playlistId = movie.playlistId,
                type = ContentType.MOVIE,
                itemId = movie.id,
                remoteId = movie.remoteId,
                title = movie.name,
                subtitle = movie.categoryName,
                imageUrl = movie.posterUrl,
                streamUrl = movie.streamUrl
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
            MoviesViewModel(playlistRepository, contentRepository, preferencesManager) as T
    }
}
