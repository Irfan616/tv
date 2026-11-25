package com.gracetv.iptv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gracetv.iptv.domain.model.ConnectionStatus
import com.gracetv.iptv.ui.theme.GraceGreen
import com.gracetv.iptv.ui.theme.GraceGreenContainer
import com.gracetv.iptv.ui.theme.GraceRed
import com.gracetv.iptv.ui.theme.GraceRedContainer
import com.gracetv.iptv.ui.theme.GraceYellow
import com.gracetv.iptv.ui.theme.GraceYellowContainer

fun Modifier.tvFocusHighlight(
    cornerRadius: Dp = 16.dp,
    focusScale: Float = 1.03f
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) focusScale else 1f, label = "tvFocusScale")
    val borderColor by animateColorAsState(
        if (isFocused) GraceYellow else Color.Transparent,
        label = "tvFocusBorder"
    )
    this
        .scale(scale)
        .border(
            width = if (isFocused) 2.5.dp else 0.dp,
            color = borderColor,
            shape = RoundedCornerShape(cornerRadius)
        )
        .onFocusChanged { isFocused = it.isFocused }
        .focusable()
}

@Composable
fun GraceTvMark(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val pad = w * 0.08f
        // Rounded dark TV frame
        drawRoundRect(
            color = Color(0xFF1A1E26),
            topLeft = Offset(pad, pad * 1.2f),
            size = Size(w - pad * 2, h - pad * 2.4f),
            cornerRadius = CornerRadius(w * 0.22f, w * 0.22f)
        )
        // Tri-color border arcs (Red, Yellow, Green)
        drawArc(
            color = GraceRed,
            startAngle = 140f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(pad, pad * 1.2f),
            size = Size(w - pad * 2, h - pad * 2.4f),
            style = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
        )
        drawArc(
            color = GraceYellow,
            startAngle = 255f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(pad, pad * 1.2f),
            size = Size(w - pad * 2, h - pad * 2.4f),
            style = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
        )
        drawArc(
            color = GraceGreen,
            startAngle = 10f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(pad, pad * 1.2f),
            size = Size(w - pad * 2, h - pad * 2.4f),
            style = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
        )
        // Play triangle
        val playPath = Path().apply {
            moveTo(w * 0.40f, h * 0.33f)
            lineTo(w * 0.68f, h * 0.50f)
            lineTo(w * 0.40f, h * 0.67f)
            close()
        }
        drawPath(playPath, color = GraceRed)
    }
}

@Composable
fun GraceTvWordmark(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GraceTvMark(size = 34.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Grace ",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "TV",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = GraceRed
            )
        }
    }
}

@Composable
fun ConnectionStatusBadge(
    status: ConnectionStatus,
    modifier: Modifier = Modifier
) {
    val (bg, fg, label) = when (status) {
        ConnectionStatus.CONNECTED -> Triple(GraceGreenContainer, GraceGreen, "Connected")
        ConnectionStatus.SYNCING -> Triple(GraceYellowContainer, GraceYellow, "Syncing")
        ConnectionStatus.ERROR -> Triple(GraceRedContainer, GraceRed, "Error")
        ConnectionStatus.DISCONNECTED -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Disconnected"
        )
    }
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(fg)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun MediaImage(
    url: String?,
    contentDescription: String,
    fallbackIcon: ImageVector = Icons.Filled.LiveTv,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = fallbackIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.size(28.dp)
        )
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    actionTestTag: String = "empty_action_button",
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GraceYellow,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = GraceRed),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag(actionTestTag)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(text = actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun PosterMediaCard(
    title: String,
    subtitle: String,
    imageUrl: String,
    badgeText: String? = null,
    fallbackIcon: ImageVector = Icons.Filled.Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.tvFocusHighlight(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
            ) {
                MediaImage(
                    url = imageUrl,
                    contentDescription = title,
                    fallbackIcon = fallbackIcon,
                    modifier = Modifier.fillMaxSize()
                )
                if (!badgeText.isNullOrBlank()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = GraceYellow,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
