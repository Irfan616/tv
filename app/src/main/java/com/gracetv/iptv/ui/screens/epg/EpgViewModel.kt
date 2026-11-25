package com.gracetv.iptv.ui.screens.epg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gracetv.iptv.data.local.PreferencesManager
import com.gracetv.iptv.data.local.entity.ChannelEntity
import com.gracetv.iptv.data.local.entity.EpgProgrammeEntity
import com.gracetv.iptv.data.repository.ContentRepository
import com.gracetv.iptv.data.repository.EpgRepository
import com.gracetv.iptv.data.repository.PlaylistRepository
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

data class EpgUiState(
    val configuredUrl: String = "",
    val lastSyncMillis: Long = 0L,
    val totalProgrammes: Int = 0,
    val channelsWithTvg: List<ChannelEntity> = emptyList(),
    val selectedChannel: ChannelEntity? = null,
    val channelProgrammes: List<EpgProgrammeEntity> = emptyList(),
    val isRefreshing: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class EpgViewModel(
    private val playlistRepository: PlaylistRepository,
    private val contentRepository: ContentRepository,
    private val epgRepository: EpgRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val selectedChannelId = MutableStateFlow<Long?>(null)
    private val loadingState = MutableStateFlow(Triple(false, null as String?, null as String?))

    private val channelsFlow = playlistRepository.observeDefaultPlaylist().flatMapLatest { playlist ->
        if (playlist == null) flowOf(emptyList())
        else contentRepository.observeChannels(playlist.id)
    }

    private val selectedChannelFlow = combine(channelsFlow, selectedChannelId) { channels, selId ->
        val candidates = channels.filter { it.tvgId.isNotBlank() }.ifEmpty { channels }
        candidates.firstOrNull { it.id == selId } ?: candidates.firstOrNull()
    }

    private val programmesFlow = selectedChannelFlow.flatMapLatest { channel ->
        val tvgId = channel?.tvgId?.ifBlank { channel.tvgName }.orEmpty()
        if (tvgId.isBlank()) flowOf(emptyList())
        else epgRepository.observeProgrammesForChannel(tvgId, System.currentTimeMillis() - 3600_000L)
    }

    val uiState: StateFlow<EpgUiState> = combine(
        preferencesManager.preferences,
        epgRepository.observeProgrammeCount(),
        channelsFlow,
        selectedChannelFlow,
        programmesFlow,
        loadingState
    ) { arr ->
        val prefs = arr[0] as com.gracetv.iptv.data.local.UserPreferences
        val count = arr[1] as Int
        @Suppress("UNCHECKED_CAST")
        val channels = arr[2] as List<ChannelEntity>
        val selected = arr[3] as ChannelEntity?
        @Suppress("UNCHECKED_CAST")
        val programmes = arr[4] as List<EpgProgrammeEntity>
        @Suppress("UNCHECKED_CAST")
        val statusTriple = arr[5] as Triple<Boolean, String?, String?>

        val tvgChannels = channels.filter { it.tvgId.isNotBlank() }.ifEmpty { channels.take(40) }
        EpgUiState(
            configuredUrl = prefs.epgUrl,
            lastSyncMillis = prefs.epgLastSyncMillis,
            totalProgrammes = count,
            channelsWithTvg = tvgChannels,
            selectedChannel = selected,
            channelProgrammes = programmes,
            isRefreshing = statusTriple.first,
            statusMessage = statusTriple.second,
            errorMessage = statusTriple.third
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EpgUiState()
    )

    fun selectChannel(channelId: Long) {
        selectedChannelId.value = channelId
    }

    fun refreshEpg(url: String) {
        viewModelScope.launch {
            loadingState.update { Triple(true, null, null) }
            val result = epgRepository.refreshEpg(url)
            result.fold(
                onSuccess = { count ->
                    loadingState.update {
                        Triple(false, "Loaded $count EPG programmes.", null)
                    }
                },
                onFailure = { err ->
                    loadingState.update {
                        Triple(false, null, err.message ?: "Failed to load XMLTV EPG.")
                    }
                }
            )
        }
    }

    fun clearMessages() {
        loadingState.update { Triple(it.first, null, null) }
    }

    class Factory(
        private val playlistRepository: PlaylistRepository,
        private val contentRepository: ContentRepository,
        private val epgRepository: EpgRepository,
        private val preferencesManager: PreferencesManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            EpgViewModel(playlistRepository, contentRepository, epgRepository, preferencesManager) as T
    }
}
