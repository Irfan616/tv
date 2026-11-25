package com.gracetv.iptv.ui.screens.movies

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gracetv.iptv.data.local.entity.MovieEntity
import com.gracetv.iptv.data.repository.ContentRepository
import com.gracetv.iptv.domain.model.ContentType
import com.gracetv.iptv.ui.components.MediaImage
import com.gracetv.iptv.ui.theme.GraceGreen
import com.gracetv.iptv.ui.theme.GraceRed
import com.gracetv.iptv.ui.theme.GraceYellow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MovieDetailScreen(
    movieId: Long,
    viewModel: MoviesViewModel,
    onBack: () -> Unit,
    onPlayMovie: (MovieEntity) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val movie by viewModel.observeMovie(movieId).collectAsStateWithLifecycle(initialValue = null)
    var savedPositionMs by remember(movieId) { mutableLongStateOf(0L) }

    LaunchedEffect(movieId) {
        viewModel.refreshMovieDetails(movieId)
    }

    LaunchedEffect(movie?.id) {
        movie?.let { savedPositionMs = viewModel.getSavedPositionMs(it) }
    }

    val currentMovie = movie ?: return
    val favKey = ContentRepository.buildItemKey(ContentType.MOVIE, currentMovie.id, currentMovie.remoteId)
    val isFavorite = favKey in state.favoriteKeys

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
        ) {
            MediaImage(
                url = currentMovie.posterUrl,
                contentDescription = currentMovie.name,
                fallbackIcon = Icons.Filled.Movie,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.45f),
                                Color.Transparent,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .size(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(currentMovie.name, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentMovie.categoryName.isNotBlank()) InfoChip(currentMovie.categoryName, GraceYellow)
                if (currentMovie.releaseYear.isNotBlank()) InfoChip(currentMovie.releaseYear, GraceGreen)
                if (currentMovie.duration.isNotBlank()) InfoChip(currentMovie.duration, MaterialTheme.colorScheme.onSurface)
                if (currentMovie.rating.isNotBlank()) InfoChip("★ ${currentMovie.rating}", GraceYellow)
                if (currentMovie.genre.isNotBlank()) InfoChip(currentMovie.genre, MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onPlayMovie(currentMovie) },
                    colors = ButtonDefaults.buttonColors(containerColor = GraceRed),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("play_movie_button")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (savedPositionMs > 10_000L) "Resume Movie" else "Play Movie")
                }

                OutlinedButton(
                    onClick = { viewModel.toggleFavorite(currentMovie) },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (isFavorite) GraceRed else MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("favorite_movie_button")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) GraceRed else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (currentMovie.plot.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                Text("Synopsis", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = currentMovie.plot,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoChip(text: String, color: Color) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
