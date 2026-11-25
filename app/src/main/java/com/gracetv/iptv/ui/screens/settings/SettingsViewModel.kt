package com.gracetv.iptv.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gracetv.iptv.data.local.PreferencesManager
import com.gracetv.iptv.data.local.UserPreferences
import com.gracetv.iptv.data.local.entity.PlaylistEntity
import com.gracetv.iptv.data.repository.ContentRepository
import com.gracetv.iptv.data.repository.EpgRepository
import com.gracetv.iptv.data.repository.PlaylistRepository
import com.gracetv.iptv.domain.model.AspectRatioMode
import com.gracetv.iptv.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val statusMessage: String? = null
)

class SettingsViewModel(
    private val preferencesManager: PreferencesManager,
    private val playlistRepository: PlaylistRepository,
    private val contentRepository: ContentRepository,
    private val epgRepository: EpgRepository
) : ViewModel() {

    private val messageFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesManager.preferences,
        playlistRepository.observePlaylists(),
        messageFlow
    ) { prefs, playlists, msg ->
        SettingsUiState(
            preferences = prefs,
            playlists = playlists,
            statusMessage = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        preferencesManager.setThemeMode(mode)
    }

    fun setLanguageCode(code: String) = viewModelScope.launch {
        preferencesManager.setLanguageCode(code)
    }

    fun setAutoStartLastChannel(enabled: Boolean) = viewModelScope.launch {
        preferencesManager.setAutoStartLastChannel(enabled)
    }

    fun setDefaultPlaylist(playlistId: Long) = viewModelScope.launch {
        playlistRepository.setDefaultPlaylist(playlistId)
        messageFlow.value = "Default playlist updated."
    }

    fun refreshActivePlaylist() = viewModelScope.launch {
        val active = uiState.value.playlists.firstOrNull { it.isDefault } ?: uiState.value.playlists.firstOrNull()
        if (active == null) {
            messageFlow.value = "No playlist configured to refresh."
            return@launch
        }
        messageFlow.value = "Refreshing \"${active.name}\"..."
        playlistRepository.refreshPlaylist(active.id).fold(
            onSuccess = { messageFlow.value = "Refreshed \"${it.name}\" successfully." },
            onFailure = { messageFlow.value = it.message ?: "Refresh failed." }
        )
    }

    fun clearCache() = viewModelScope.launch {
        contentRepository.clearWatchAndProgressCache()
        epgRepository.clearEpgCache()
        messageFlow.value = "Cleared watch history, resume positions, and EPG cache."
    }

    fun setDefaultAspectRatio(mode: AspectRatioMode) = viewModelScope.launch {
        preferencesManager.setDefaultAspectRatio(mode)
    }

    fun setAutoLandscape(enabled: Boolean) = viewModelScope.launch {
        preferencesManager.setAutoLandscape(enabled)
    }

    fun setHardwareDecoding(enabled: Boolean) = viewModelScope.launch {
        preferencesManager.setHardwareDecoding(enabled)
    }

    fun setPreferredAudioLanguage(lang: String) = viewModelScope.launch {
        preferencesManager.setPreferredAudioLanguage(lang)
    }

    fun setPreferredSubtitleLanguage(lang: String) = viewModelScope.launch {
        preferencesManager.setPreferredSubtitleLanguage(lang)
    }

    fun setShowControlsOnStart(show: Boolean) = viewModelScope.launch {
        preferencesManager.setShowPlaybackControlsOnStart(show)
    }

    fun saveParentalPin(pin: String) = viewModelScope.launch {
        preferencesManager.setParentalPin(pin)
        messageFlow.value = "Parental PIN saved and lock enabled."
    }

    suspend fun verifyParentalPin(pin: String): Boolean =
        preferencesManager.verifyParentalPin(pin)

    fun setParentalEnabled(enabled: Boolean) = viewModelScope.launch {
        preferencesManager.setParentalEnabled(enabled)
        messageFlow.value = if (enabled) "Parental controls enabled." else "Parental controls disabled."
    }

    fun setRestrictedKeywords(rawCommaSeparated: String) = viewModelScope.launch {
        val set = rawCommaSeparated.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()
        preferencesManager.setRestrictedKeywords(set)
        messageFlow.value = "Restricted category keywords updated."
    }

    fun clearMessage() {
        messageFlow.value = null
    }

    class Factory(
        private val preferencesManager: PreferencesManager,
        private val playlistRepository: PlaylistRepository,
        private val contentRepository: ContentRepository,
        private val epgRepository: EpgRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(preferencesManager, playlistRepository, contentRepository, epgRepository) as T
    }
}
