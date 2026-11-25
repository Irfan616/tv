package com.gracetv.iptv.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gracetv.iptv.data.local.entity.FavoriteEntity
import com.gracetv.iptv.data.repository.ContentRepository
import com.gracetv.iptv.domain.model.ContentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val selectedFilter: ContentType? = null,
    val favorites: List<FavoriteEntity> = emptyList(),
    val totalCount: Int = 0
)

class FavoritesViewModel(
    private val contentRepository: ContentRepository
) : ViewModel() {

    private val filterFlow = MutableStateFlow<ContentType?>(null)

    val uiState: StateFlow<FavoritesUiState> = combine(
        contentRepository.observeFavorites(),
        filterFlow
    ) { allFavs, filter ->
        val visible = if (filter == null) allFavs else allFavs.filter { it.itemType == filter }
        FavoritesUiState(
            selectedFilter = filter,
            favorites = visible,
            totalCount = allFavs.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FavoritesUiState()
    )

    fun setFilter(type: ContentType?) {
        filterFlow.value = type
    }

    fun removeFavorite(itemKey: String) {
        viewModelScope.launch {
            contentRepository.removeFavorite(itemKey)
        }
    }

    class Factory(
        private val contentRepository: ContentRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FavoritesViewModel(contentRepository) as T
    }
}
