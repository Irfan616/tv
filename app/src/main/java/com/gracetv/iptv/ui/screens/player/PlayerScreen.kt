package com.gracetv.iptv.ui.screens.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.Rational
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.gracetv.iptv.domain.model.AspectRatioMode
import com.gracetv.iptv.domain.model.ContentType
import com.gracetv.iptv.ui.theme.GraceGreen
import com.gracetv.iptv.ui.theme.GraceRed
import com.gracetv.iptv.ui.theme.GraceYellow
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    descriptor: ActiveMediaDescriptor,
    viewModel: PlayerViewModel,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current

    val state by viewModel.playerState.collectAsStateWithLifecycle()
    val currentMedia by viewModel.currentMedia.collectAsStateWithLifecycle()
    val aspectRatio by viewModel.aspectRatio.collectAsStateWithLifecycle()
    val hasPrev by viewModel.hasPrevious.collectAsStateWithLifecycle()
    val hasNext by viewModel.hasNext.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()

    var showControls by remember { mutableStateOf(true) }
    var isLandscape by remember { mutableStateOf(prefs.autoLandscape) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(descriptor) {
        viewModel.startPlayback(descriptor)
        showControls = prefs.showPlaybackControlsOnStart
    }

    // Periodic position sync & auto-hide controls
    LaunchedEffect(state.isPlaying, showControls) {
        while (true) {
            viewModel.playerManager.syncProgress()
            delay(1000L)
        }
    }

    LaunchedEffect(showControls, state.isPlaying) {
        if (showControls && state.isPlaying) {
            delay(4500L)
            showControls = false
        }
    }

    // Orientation & immersive system bars management
    DisposableEffect(activity, isLandscape) {
        if (activity != null) {
            activity.requestedOrientation = if (isLandscape) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (activity != null) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Pause if backgrounded outside PiP, save progress & release on exit
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val inPip = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                    activity?.isInPictureInPictureMode == true
                if (!inPip) {
                    viewModel.saveCurrentProgress()
                    viewModel.playerManager.pause()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.saveCurrentProgress()
            viewModel.playerManager.release()
        }
    }

    BackHandler {
        viewModel.saveCurrentProgress()
        viewModel.playerManager.release()
        onExit()
    }

    val activeTitle = currentMedia?.title ?: descriptor.title
    val activeSubtitle = currentMedia?.subtitle ?: descriptor.subtitle
    val isLive = (currentMedia?.contentType ?: descriptor.contentType) == ContentType.LIVE
    val inPipMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
        activity?.isInPictureInPictureMode == true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    player = viewModel.playerManager.getOrCreatePlayer(prefs.hardwareDecoding)
                    keepScreenOn = true
                }
            },
            update = { playerView ->
                playerView.player = viewModel.playerManager.getOrCreatePlayer(prefs.hardwareDecoding)
                playerView.resizeMode = when (aspectRatio) {
                    AspectRatioMode.FIT, AspectRatioMode.RATIO_16_9, AspectRatioMode.RATIO_4_3 ->
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                    AspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    AspectRatioMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (state.isBuffering && state.errorMessage == null) {
            CircularProgressIndicator(
                color = GraceRed,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Playback error overlay
        state.errorMessage?.let { errMsg ->
            Surface(
                color = Color(0xE614161B),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = GraceRed, modifier = Modifier.size(40.dp))
                    Text(errMsg, style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = onExit) {
                            Text("Back", color = Color.White)
                        }
                        Button(
                            onClick = { viewModel.playerManager.retry() },
                            colors = ButtonDefaults.buttonColors(containerColor = GraceRed),
                            modifier = Modifier.testTag("player_retry_button")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Retry")
                        }
                    }
                }
            }
        }

        // Player Overlay Controls
        AnimatedVisibility(
            visible = showControls && !inPipMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.72f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.82f)
                            )
                        )
                    )
                    .safeDrawingPadding()
                    .padding(16.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.saveCurrentProgress()
                                viewModel.playerManager.release()
                                onExit()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = activeTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isLive) {
                                    Surface(color = GraceRed, shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            "LIVE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            if (activeSubtitle.isNotBlank()) {
                                Text(
                                    text = activeSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GraceYellow,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                            IconButton(
                                onClick = {
                                    val params = PictureInPictureParams.Builder()
                                        .setAspectRatio(Rational(16, 9))
                                        .build()
                                    activity.enterPictureInPictureMode(params)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("player_pip_button")
                            ) {
                                Icon(Icons.Filled.PictureInPictureAlt, contentDescription = "Picture in Picture", tint = Color.White)
                            }
                        }
                        IconButton(
                            onClick = { isLandscape = !isLandscape },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (isLandscape) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                contentDescription = "Toggle fullscreen orientation",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Center Transport Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasPrev) {
                        IconButton(
                            onClick = viewModel::switchPrevious,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                    }
                    if (!isLive && state.isSeekable) {
                        IconButton(
                            onClick = { viewModel.playerManager.seekBy(-10_000L) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.Replay10, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                    Surface(
                        onClick = { viewModel.playerManager.togglePlayPause() },
                        shape = CircleShape,
                        color = GraceRed,
                        modifier = Modifier
                            .size(64.dp)
                            .testTag("player_play_pause_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    if (!isLive && state.isSeekable) {
                        IconButton(
                            onClick = { viewModel.playerManager.seekBy(10_000L) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                    if (hasNext) {
                        IconButton(
                            onClick = viewModel::switchNext,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                    }
                }

                // Bottom Bar: Seekbar + Options
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    if (!isLive && state.isSeekable && state.durationMs > 0L) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = formatDuration(state.currentPositionMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                            Slider(
                                value = state.currentPositionMs.toFloat().coerceIn(0f, state.durationMs.toFloat()),
                                onValueChange = { viewModel.playerManager.seekTo(it.toLong()) },
                                valueRange = 0f..state.durationMs.toFloat().coerceAtLeast(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = GraceYellow,
                                    activeTrackColor = GraceRed,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatDuration(state.durationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.playerManager.toggleMute() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Mute toggle",
                                    tint = if (state.isMuted) GraceRed else Color.White
                                )
                            }
                            Slider(
                                value = if (state.isMuted) 0f else state.volumeFraction,
                                onValueChange = { viewModel.playerManager.setVolume(it) },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = GraceGreen,
                                    activeTrackColor = GraceGreen
                                ),
                                modifier = Modifier.width(100.dp)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = viewModel::cycleAspectRatio,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.AspectRatio, contentDescription = "Aspect Ratio: ${aspectRatio.label}", tint = GraceYellow)
                            }
                            if (state.audioTracks.isNotEmpty()) {
                                IconButton(
                                    onClick = { showAudioDialog = true },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Filled.Audiotrack, contentDescription = "Audio Tracks", tint = Color.White)
                                }
                            }
                            if (state.subtitleTracks.isNotEmpty()) {
                                IconButton(
                                    onClick = { showSubtitleDialog = true },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Filled.Subtitles, contentDescription = "Subtitles", tint = Color.White)
                                }
                            }
                            if (!isLive) {
                                IconButton(
                                    onClick = { showSpeedDialog = true },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Filled.Speed, contentDescription = "Playback Speed", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAudioDialog) {
        AlertDialog(
            onDismissRequest = { showAudioDialog = false },
            title = { Text("Select Audio Track") },
            text = {
                Column {
                    state.audioTracks.forEach { track ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectAudioTrack(track)
                                    showAudioDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(selected = track.isSelected, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(track.label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAudioDialog = false }) { Text("Close") }
            }
        )
    }

    if (showSubtitleDialog) {
        AlertDialog(
            onDismissRequest = { showSubtitleDialog = false },
            title = { Text("Select Subtitles") },
            text = {
                Column {
                    val noneSelected = state.subtitleTracks.none { it.isSelected }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectSubtitleTrack(null)
                                showSubtitleDialog = false
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(selected = noneSelected, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Off")
                    }
                    state.subtitleTracks.forEach { track ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectSubtitleTrack(track)
                                    showSubtitleDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(selected = track.isSelected, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(track.label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubtitleDialog = false }) { Text("Close") }
            }
        )
    }

    if (showSpeedDialog) {
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Playback Speed") },
            text = {
                Column {
                    speeds.forEach { spd ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.playerManager.setPlaybackSpeed(spd)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(selected = state.playbackSpeed == spd, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text("${spd}x")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) { Text("Close") }
            }
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val hrs = totalSec / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    return if (hrs > 0L) "%d:%02d:%02d".format(hrs, mins, secs) else "%02d:%02d".format(mins, secs)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
