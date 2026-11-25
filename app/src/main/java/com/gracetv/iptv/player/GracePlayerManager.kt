package com.gracetv.iptv.player

import android.content.Context
import android.media.AudioManager
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TrackOption(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String,
    val isSelected: Boolean
)

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = true,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isSeekable: Boolean = false,
    val isMuted: Boolean = false,
    val volumeFraction: Float = 1f,
    val playbackSpeed: Float = 1f,
    val errorMessage: String? = null,
    val audioTracks: List<TrackOption> = emptyList(),
    val subtitleTracks: List<TrackOption> = emptyList()
)

@OptIn(UnstableApi::class)
class GracePlayerManager(private val context: Context) {
    private var exoPlayer: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var lastLoadedUrl: String? = null

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val player = exoPlayer ?: return
            _state.update {
                it.copy(
                    isBuffering = playbackState == Player.STATE_BUFFERING,
                    isPlaying = player.isPlaying,
                    durationMs = player.duration.coerceAtLeast(0L),
                    isSeekable = player.isCurrentMediaItemSeekable,
                    errorMessage = if (playbackState == Player.STATE_READY) null else it.errorMessage
                )
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onTracksChanged(tracks: Tracks) {
            refreshTrackOptions(tracks)
        }

        override fun onPlayerError(error: PlaybackException) {
            val friendly = when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                    "Network connection failed while loading the stream."
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                    "Stream unavailable or returned an HTTP error."
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
                PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ->
                    "Unsupported stream or codec format."
                else -> "Stream playback failed (${error.errorCodeName})."
            }
            _state.update {
                it.copy(
                    isBuffering = false,
                    isPlaying = false,
                    errorMessage = friendly
                )
            }
        }
    }

    fun getOrCreatePlayer(hardwareDecoding: Boolean = true): ExoPlayer {
        exoPlayer?.let { return it }

        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("GraceTV/1.0 (Android ExoPlayer)")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)

        val selector = DefaultTrackSelector(context)
        trackSelector = selector

        val renderersFactory = DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(
                if (hardwareDecoding) DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
                else DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
            )
            setEnableDecoderFallback(true)
        }

        val player = ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(selector)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory))
            .build().apply {
                addListener(playerListener)
                playWhenReady = true
            }
        exoPlayer = player
        return player
    }

    fun loadStream(url: String, startPositionMs: Long = 0L, hardwareDecoding: Boolean = true) {
        val player = getOrCreatePlayer(hardwareDecoding)
        lastLoadedUrl = url
        _state.update {
            it.copy(
                isBuffering = true,
                errorMessage = null,
                currentPositionMs = startPositionMs,
                playbackSpeed = 1f
            )
        }
        val mediaItem = MediaItem.fromUri(url)
        player.setMediaItem(mediaItem)
        if (startPositionMs > 0L) {
            player.seekTo(startPositionMs)
        }
        player.prepare()
        player.playWhenReady = true
    }

    fun retry() {
        val url = lastLoadedUrl ?: return
        val pos = exoPlayer?.currentPosition?.coerceAtLeast(0L) ?: 0L
        loadStream(url, startPositionMs = pos)
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs.coerceAtLeast(0L))
        syncProgress()
    }

    fun seekBy(deltaMs: Long) {
        val player = exoPlayer ?: return
        val target = (player.currentPosition + deltaMs).coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(target)
        syncProgress()
    }

    fun toggleMute() {
        val player = exoPlayer ?: return
        val nextMuted = !_state.value.isMuted
        player.volume = if (nextMuted) 0f else _state.value.volumeFraction
        _state.update { it.copy(isMuted = nextMuted) }
    }

    fun setVolume(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        exoPlayer?.volume = clamped
        audioManager?.let { am ->
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (max > 0) {
                am.setStreamVolume(AudioManager.STREAM_MUSIC, (clamped * max).toInt(), 0)
            }
        }
        _state.update { it.copy(volumeFraction = clamped, isMuted = clamped == 0f) }
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.playbackParameters = PlaybackParameters(speed)
        _state.update { it.copy(playbackSpeed = speed) }
    }

    fun selectAudioTrack(option: TrackOption) {
        val player = exoPlayer ?: return
        val groups = player.currentTracks.groups
        if (option.groupIndex !in groups.indices) return
        val group = groups[option.groupIndex].mediaTrackGroup
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(TrackSelectionOverride(group, option.trackIndex))
            .build()
    }

    fun selectSubtitleTrack(option: TrackOption?) {
        val player = exoPlayer ?: return
        val builder = player.trackSelectionParameters.buildUpon()
        if (option == null) {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        } else {
            val groups = player.currentTracks.groups
            if (option.groupIndex in groups.indices) {
                val group = groups[option.groupIndex].mediaTrackGroup
                builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                builder.setOverrideForType(TrackSelectionOverride(group, option.trackIndex))
            }
        }
        player.trackSelectionParameters = builder.build()
    }

    fun syncProgress() {
        val player = exoPlayer ?: return
        _state.update {
            it.copy(
                currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = player.duration.coerceAtLeast(0L),
                isPlaying = player.isPlaying,
                isSeekable = player.isCurrentMediaItemSeekable
            )
        }
    }

    fun release() {
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
        trackSelector = null
        lastLoadedUrl = null
        _state.value = PlayerUiState()
    }

    private fun refreshTrackOptions(tracks: Tracks) {
        val audio = ArrayList<TrackOption>()
        val subs = ArrayList<TrackOption>()

        tracks.groups.forEachIndexed { groupIdx, group ->
            when (group.type) {
                C.TRACK_TYPE_AUDIO -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val lang = format.language?.ifBlank { null } ?: "und"
                        val label = format.label?.ifBlank { null }
                            ?: "Audio ${audio.size + 1} (${lang.uppercase()})"
                        audio.add(
                            TrackOption(
                                groupIndex = groupIdx,
                                trackIndex = i,
                                label = label,
                                language = lang,
                                isSelected = group.isTrackSelected(i)
                            )
                        )
                    }
                }
                C.TRACK_TYPE_TEXT -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val lang = format.language?.ifBlank { null } ?: "und"
                        val label = format.label?.ifBlank { null }
                            ?: "Subtitle ${subs.size + 1} (${lang.uppercase()})"
                        subs.add(
                            TrackOption(
                                groupIndex = groupIdx,
                                trackIndex = i,
                                label = label,
                                language = lang,
                                isSelected = group.isTrackSelected(i)
                            )
                        )
                    }
                }
            }
        }

        _state.update { it.copy(audioTracks = audio, subtitleTracks = subs) }
    }
}
