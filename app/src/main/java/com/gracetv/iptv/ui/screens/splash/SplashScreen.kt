package com.gracetv.iptv.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gracetv.iptv.ui.components.GraceTvMark
import com.gracetv.iptv.ui.theme.DarkBackground
import com.gracetv.iptv.ui.theme.GraceGreen
import com.gracetv.iptv.ui.theme.GraceRed
import com.gracetv.iptv.ui.theme.GraceYellow
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    val scale = remember { Animatable(0.75f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = tween(550, easing = FastOutSlowInEasing))
        alpha.animateTo(1f, animationSpec = tween(400))
        delay(950)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1F1618), DarkBackground),
                    radius = 900f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale.value)
                .padding(32.dp)
        ) {
            GraceTvMark(size = 104.dp)
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Grace ",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "TV",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = GraceRed
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "MODERN IPTV PLAYER",
                style = MaterialTheme.typography.labelMedium,
                color = GraceYellow
            )
            Spacer(Modifier.height(28.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.alpha(alpha.value)
            ) {
                Box(
                    Modifier
                        .size(width = 24.dp, height = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(GraceRed)
                )
                Box(
                    Modifier
                        .size(width = 24.dp, height = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(GraceYellow)
                )
                Box(
                    Modifier
                        .size(width = 24.dp, height = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(GraceGreen)
                )
            }
            Spacer(Modifier.height(24.dp))
            LinearProgressIndicator(
                color = GraceRed,
                trackColor = Color(0xFF232730),
                modifier = Modifier
                    .width(160.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
            )
        }
    }
}
