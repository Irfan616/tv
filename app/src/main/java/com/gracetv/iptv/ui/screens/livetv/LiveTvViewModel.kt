package com.gracetv.iptv.ui.screens.livetv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gracetv.iptv.data.local.PreferencesManager
import com.gracetv.iptv.data.local.UserPreferences
import com.gracetv.iptv.data.local.entity.CategoryEntity
import com.gracetv.iptv.data.local.entity.ChannelEntity
import com.gracetv.iptv.data.local.entity.EpgProgrammeEntity
import com.gracetv.iptv.data.local.entity.PlaylistEntity
import com.gracetv.iptv.data.repository.ContentRepository
import com.gracetv.iptv.data.repository.EpgRepository
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

data class LiveTvUiState(
    val activePlaylist: PlaylistEntity? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val channels: List<ChannelEntity> = emptyList(),
    val favoriteKeys: Set<String> = emptySet(),
    val currentProgrammesByTvgId: Map<String, EpgProgrammeEntity> = emptyMap(),
    val parentalUnlockedCategories: Set<String> = emptySet(),
    val preferences: UserPreferences = UserPreferences(),
    val pendingRestrictedCategory: CategoryEntity? = null,
    val pendingRestrictedChannel: ChannelEntity? = null,
    val pinError: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class LiveTvViewModel(
    private val playlistRepository: PlaylistRepository,
    private val contentRepository: ContentRepository,
    private val epgRepository: EpgRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val searchQuery = MutableStateFlow("")
    private val unlockedCategories = MutableStateFlow<Set<String>>(emptySet())
    private val pendingCategory = MutableStateFlow<CategoryEntity?>(null)
    private val pendingChannel = MutableStateFlow<ChannelEntity?>(null)
    private val pinError = MutableStateFlow<String?>(null)

    private val activePlaylistFlow = playlistRepository.observeDefaultPlaylist()

    private val categoriesFlow = activePlaylistFlow.flatMapLatest { playlist ->
        if (playlist == null) flowOf(emptyList())
        else contentRepository.observeCategories(playlist.id, ContentType.LIVE)
    }

    private val channelsFlow = activePlaylistFlow.flatMapLatest { playlist ->
        if (playlist == null) flowOf(emptyList())
        else contentRepository.observeChannels(playlist.id)
    }

    private val nowProgrammesFlow = epgRepository.observeUpcomingWindow(
        nowMs = System.currentTimeMillis(),
        horizonMs = System.currentTimeMillis() + 6 * 3600_000L
    )

    val uiState: StateFlow<LiveTvUiState> = combine(
        activePlaylistFlow,
        categoriesFlow,
        channelsFlow,
        contentRepository.observeFavoriteKeys(),
        nowProgrammesFlow,
        preferencesManager.preferences,
        selectedCategoryId,
        searchQuery,
        unlockedCategories,
        pendingCategory,
        pendingChannel,
        pinError
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val playlist = arr[0] as PlaylistEntity?
        @Suppress("UNCHECKED_CAST")
        val categories = arr[1] as List<CategoryEntity>
        @Suppress("UNCHECKED_CAST")
        val allChannels = arr[2] as List<ChannelEntity>
        @Suppress("UNCHECKED_CAST")
        val favKeys = (arr[3] as List<String>).toSet()
        @Suppress("UNCHECKED_CAST")
        val epgList = arr[4] as List<EpgProgrammeEntity>
        val prefs = arr[5] as UserPreferences
        val selCat = arr[6] as String?
        val query = arr[7] as String
        @Suppress("UNCHECKED_CAST")
        val unlocked = arr[8] as Set<String>
        val pendCat = arr[9] as CategoryEntity?
        val pendChan = arr[10] as ChannelEntity?
        val err = arr[11] as String?

        val now = System.currentTimeMillis()
        val epgMap = epgList
            .filter { it.startTimeMs <= now && it.endTimeMs > now }
            .associateBy { it.channelTvgId.lowercase() }

        val filtered = allChannels.filter { ch ->
            val matchesCat = selCat == null || ch.categoryId == selCat
            val matchesQuery = query.isBlank() || ch.name.contains(query, ignoreCase = true)
            val isRestricted = isChannelRestricted(ch, prefs, unlocked)
            // Keep locked channels visible with lock badge if category selected, or filter when global search
            matchesCat && matchesQuery && (!isRestricted || selCat == ch.categoryId)
        }

        LiveTvUiState(
            activePlaylist = playlist,
            categories = categories,
            selectedCategoryId = selCat,
            searchQuery = query,
            channels = filtered,
            favoriteKeys = favKeys,
            currentProgrammesByTvgId = epgMap,
            parentalUnlockedCategories = unlocked,
            preferences = prefs,
            pendingRestrictedCategory = pendCat,
            pendingRestrictedChannel = pendChan,
            pinError = err
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LiveTvUiState()
    )

    fun isCategoryRestricted(category: CategoryEntity, prefs: UserPreferences, unlocked: Set<String>): Boolean {
        if (!prefs.parentalEnabled || !prefs.hasParentalPin) return false
        if (category.remoteId in unlocked) return false
        return category.isAdult || UrlValidator.isAdultContent(category.name, prefs.restrictedKeywords)
    }

    fun isChannelRestricted(channel: ChannelEntity, prefs: UserPreferences, unlocked: Set<String>): Boolean {
        if (!prefs.parentalEnabled || !prefs.hasParentalPin) return false
        if (channel.categoryId in unlocked) return false
        return channel.isAdult ||
            UrlValidator.isAdultContent(channel.categoryName, prefs.restrictedKeywords) ||
            UrlValidator.isAdultContent(channel.name, prefs.restrictedKeywords)
    }

    fun selectCategory(category: CategoryEntity?) {
        if (category == null) {
            selectedCategoryId.value = null
            return
        }
        val state = uiState.value
        if (isCategoryRestricted(category, state.preferences, state.parentalUnlockedCategories)) {
            pendingCategory.value = category
            pinError.value = null
        } else {
            selectedCategoryId.value = category.remoteId
        }
    }

    fun requestChannelPlayback(channel: ChannelEntity, onAllowed: (ChannelEntity) -> Unit) {
        val state = uiState.value
        if (isChannelRestricted(channel, state.preferences, state.parentalUnlockedCategories)) {
            pendingChannel.value = channel
            pinError.value = null
        } else {
            onAllowed(channel)
        }
    }

    fun submitParentalPin(pin: String, onChannelUnlocked: (ChannelEntity) -> Unit) {
        viewModelScope.launch {
            val valid = preferencesManager.verifyParentalPin(pin)
            if (!valid) {
                pinError.value = "Incorrect PIN. Try again."
                return@launch
            }
            pinError.value = null
            pendingCategory.value?.let { cat ->
                unlockedCategories.update { it + cat.remoteId }
                selectedCategoryId.value = cat.remoteId
                pendingCategory.value = null
            }
            pendingChannel.value?.let { ch ->
                unlockedCategories.update { it + ch.categoryId }
                pendingChannel.value = null
                onChannelUnlocked(ch)
            }
        }
    }

    fun dismissPinPrompt() {
        pendingCategory.value = null
        pendingChannel.value = null
        pinError.value = null
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun toggleFavorite(channel: ChannelEntity) {
        viewModelScope.launch {
            contentRepository.toggleFavorite(
                playlistId = channel.playlistId,
                type = ContentType.LIVE,
                itemId = channel.id,
                remoteId = channel.remoteId,
                title = channel.name,
                subtitle = channel.categoryName,
                imageUrl = channel.logoUrl,
                streamUrl = channel.streamUrl
            )
        }
    }

    class Factory(
        private val playlistRepository: PlaylistRepository,
        private val contentRepository: ContentRepository,
        private val epgRepository: EpgRepository,
        private val preferencesManager: PreferencesManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LiveTvViewModel(playlistRepository, contentRepository, epgRepository, preferencesManager) as T
    }
}
