package com.gracetv.iptv.ui.screens.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gracetv.iptv.data.local.PreferencesManager
import com.gracetv.iptv.data.local.UserPreferences
import com.gracetv.iptv.data.local.entity.ChannelEntity
import com.gracetv.iptv.data.local.entity.EpisodeEntity
import com.gracetv.iptv.data.repository.ContentRepository
import com.gracetv.iptv.domain.model.AspectRatioMode
import com.gracetv.iptv.domain.model.ContentType
import com.gracetv.iptv.player.GracePlayerManager
import com.gracetv.iptv.player.PlayerUiState
import com.gracetv.iptv.player.TrackOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActiveMediaDescriptor(
    val playlistId: Long,
    val contentType: ContentType,
    val itemId: Long,
    val remoteId: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val streamUrl: String,
    val seriesRemoteId: String = ""
)

class PlayerViewModel(
    appContext: Context,
    private val contentRepository: ContentRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val playerManager = GracePlayerManager(appContext)
    val playerState: StateFlow<PlayerUiState> = playerManager.state

    val preferences: StateFlow<UserPreferences> = preferencesManager.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserPreferences()
    )

    private val _currentMedia = MutableStateFlow<ActiveMediaDescriptor?>(null)
    val currentMedia: StateFlow<ActiveMediaDescriptor?> = _currentMedia.asStateFlow()

    private val _aspectRatio = MutableStateFlow(AspectRatioMode.FIT)
    val aspectRatio: StateFlow<AspectRatioMode> = _aspectRatio.asStateFlow()

    private var channelQueue: List<ChannelEntity> = emptyList()
    private var episodeQueue: List<EpisodeEntity> = emptyList()

    private val _hasPrevious = MutableStateFlow(false)
    val hasPrevious: StateFlow<Boolean> = _hasPrevious.asStateFlow()

    private val _hasNext = MutableStateFlow(false)
    val hasNext: StateFlow<Boolean> = _hasNext.asStateFlow()

    fun startPlayback(descriptor: ActiveMediaDescriptor) {
        if (_currentMedia.value?.streamUrl == descriptor.streamUrl &&
            _currentMedia.value?.itemId == descriptor.itemId
        ) {
            return
        }
        _currentMedia.value = descriptor
        viewModelScope.launch {
            val prefs = preferencesManager.preferences.first()
            _aspectRatio.value = prefs.defaultAspectRatio

            val resumeMs = if (descriptor.contentType != ContentType.LIVE) {
                contentRepository.getSavedPositionMs(
                    descriptor.contentType,
                    descriptor.itemId,
                    descriptor.remoteId
                )
            } else {
                preferencesManager.setLastChannelId(descriptor.itemId)
                0L
            }

            playerManager.loadStream(
                url = descriptor.streamUrl,
                startPositionMs = resumeMs,
                hardwareDecoding = prefs.hardwareDecoding
            )

            contentRepository.recordWatchHistory(
                playlistId = descriptor.playlistId,
                type = descriptor.contentType,
                itemId = descriptor.itemId,
                remoteId = descriptor.remoteId,
                title = descriptor.title,
                subtitle = descriptor.subtitle,
                imageUrl = descriptor.imageUrl,
                streamUrl = descriptor.streamUrl
            )

            loadSwitchQueue(descriptor)
        }
    }

    private suspend fun loadSwitchQueue(descriptor: ActiveMediaDescriptor) {
        when (descriptor.contentType) {
            ContentType.LIVE -> {
                channelQueue = contentRepository.getChannelsOnce(descriptor.playlistId)
                val idx = channelQueue.indexOfFirst { it.id == descriptor.itemId }
                _hasPrevious.value = idx > 0
                _hasNext.value = idx >= 0 && idx < channelQueue.lastIndex
            }
            ContentType.EPISODE -> {
                if (descriptor.seriesRemoteId.isNotBlank()) {
                    episodeQueue = contentRepository.getEpisodesOnce(
                        descriptor.playlistId,
                        descriptor.seriesRemoteId
                    )
                    val idx = episodeQueue.indexOfFirst {
                        it.id == descriptor.itemId || it.remoteId == descriptor.remoteId
                    }
                    _hasPrevious.value = idx > 0
                    _hasNext.value = idx >= 0 && idx < episodeQueue.lastIndex
                } else {
                    _hasPrevious.value = false
                    _hasNext.value = false
                }
            }
            else -> {
                _hasPrevious.value = false
                _hasNext.value = false
            }
        }
    }

    fun switchPrevious() {
        val current = _currentMedia.value ?: return
        saveCurrentProgress()
        when (current.contentType) {
            ContentType.LIVE -> {
                val idx = channelQueue.indexOfFirst { it.id == current.itemId }
                if (idx > 0) {
                    val prev = channelQueue[idx - 1]
                    startPlayback(
                        ActiveMediaDescriptor(
                            playlistId = prev.playlistId,
                            contentType = ContentType.LIVE,
                            itemId = prev.id,
                            remoteId = prev.remoteId,
                            title = prev.name,
                            subtitle = prev.categoryName,
                            imageUrl = prev.logoUrl,
                            streamUrl = prev.streamUrl
                        )
                    )
                }
            }
            ContentType.EPISODE -> {
                val idx = episodeQueue.indexOfFirst {
                    it.id == current.itemId || it.remoteId == current.remoteId
                }
                if (idx > 0) {
                    val prev = episodeQueue[idx - 1]
                    startPlayback(
                        ActiveMediaDescriptor(
                            playlistId = prev.playlistId,
                            contentType = ContentType.EPISODE,
                            itemId = prev.id,
                            remoteId = prev.remoteId,
                            title = "S${prev.seasonNumber}E${prev.episodeNumber} • ${prev.title}",
                            subtitle = current.subtitle,
                            imageUrl = prev.thumbnailUrl,
                            streamUrl = prev.streamUrl,
                            seriesRemoteId = prev.seriesRemoteId
                        )
                    )
                }
            }
            else -> Unit
        }
    }

    fun switchNext() {
        val current = _currentMedia.value ?: return
        saveCurrentProgress()
        when (current.contentType) {
            ContentType.LIVE -> {
                val idx = channelQueue.indexOfFirst { it.id == current.itemId }
                if (idx >= 0 && idx < channelQueue.lastIndex) {
                    val next = channelQueue[idx + 1]
                    startPlayback(
                        ActiveMediaDescriptor(
                            playlistId = next.playlistId,
                            contentType = ContentType.LIVE,
                            itemId = next.id,
                            remoteId = next.remoteId,
                            title = next.name,
                            subtitle = next.categoryName,
                            imageUrl = next.logoUrl,
                            streamUrl = next.streamUrl
                        )
                    )
                }
            }
            ContentType.EPISODE -> {
                val idx = episodeQueue.indexOfFirst {
                    it.id == current.itemId || it.remoteId == current.remoteId
                }
                if (idx >= 0 && idx < episodeQueue.lastIndex) {
                    val next = episodeQueue[idx + 1]
                    startPlayback(
                        ActiveMediaDescriptor(
                            playlistId = next.playlistId,
                            contentType = ContentType.EPISODE,
                            itemId = next.id,
                            remoteId = next.remoteId,
                            title = "S${next.seasonNumber}E${next.episodeNumber} • ${next.title}",
                            subtitle = current.subtitle,
                            imageUrl = next.thumbnailUrl,
                            streamUrl = next.streamUrl,
                            seriesRemoteId = next.seriesRemoteId
                        )
                    )
                }
            }
            else -> Unit
        }
    }

    fun cycleAspectRatio() {
        val modes = AspectRatioMode.entries
        val nextIdx = (modes.indexOf(_aspectRatio.value) + 1) % modes.size
        _aspectRatio.value = modes[nextIdx]
    }

    fun selectAudioTrack(option: TrackOption) = playerManager.selectAudioTrack(option)
    fun selectSubtitleTrack(option: TrackOption?) = playerManager.selectSubtitleTrack(option)

    fun saveCurrentProgress() {
        playerManager.syncProgress()
        val media = _currentMedia.value ?: return
        val st = playerState.value
        if (media.contentType != ContentType.LIVE && st.durationMs > 0L) {
            viewModelScope.launch {
                contentRepository.savePlaybackProgress(
                    playlistId = media.playlistId,
                    type = media.contentType,
                    itemId = media.itemId,
                    remoteId = media.remoteId,
                    title = media.title,
                    subtitle = media.subtitle,
                    imageUrl = media.imageUrl,
                    streamUrl = media.streamUrl,
                    positionMs = st.currentPositionMs,
                    durationMs = st.durationMs
                )
            }
        }
    }

    override fun onCleared() {
        saveCurrentProgress()
        playerManager.release()
        super.onCleared()
    }

    class Factory(
        private val context: Context,
        private val contentRepository: ContentRepository,
        private val preferencesManager: PreferencesManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PlayerViewModel(context.applicationContext, contentRepository, preferencesManager) as T
    }
}
