package com.gracetv.iptv.data.repository

import com.gracetv.iptv.data.local.GraceTvDatabase
import com.gracetv.iptv.data.local.entity.CategoryEntity
import com.gracetv.iptv.data.local.entity.ChannelEntity
import com.gracetv.iptv.data.local.entity.EpisodeEntity
import com.gracetv.iptv.data.local.entity.FavoriteEntity
import com.gracetv.iptv.data.local.entity.MovieEntity
import com.gracetv.iptv.data.local.entity.PlaybackProgressEntity
import com.gracetv.iptv.data.local.entity.SearchHistoryEntity
import com.gracetv.iptv.data.local.entity.SeriesEntity
import com.gracetv.iptv.data.local.entity.WatchHistoryEntity
import com.gracetv.iptv.data.remote.XtreamApiService
import com.gracetv.iptv.domain.model.ContentType
import com.gracetv.iptv.domain.model.PlaylistType
import com.gracetv.iptv.security.CredentialCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class GlobalSearchResult(
    val channels: List<ChannelEntity> = emptyList(),
    val movies: List<MovieEntity> = emptyList(),
    val series: List<SeriesEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList()
)

class ContentRepository(
    private val database: GraceTvDatabase,
    private val apiService: XtreamApiService,
    private val cipher: CredentialCipher
) {
    private val categoryDao = database.categoryDao()
    private val channelDao = database.channelDao()
    private val movieDao = database.movieDao()
    private val seriesDao = database.seriesDao()
    private val episodeDao = database.episodeDao()
    private val favoriteDao = database.favoriteDao()
    private val watchHistoryDao = database.watchHistoryDao()
    private val progressDao = database.playbackProgressDao()
    private val searchHistoryDao = database.searchHistoryDao()
    private val playlistDao = database.playlistDao()

    fun observeCategories(playlistId: Long, type: ContentType): Flow<List<CategoryEntity>> =
        categoryDao.observeByPlaylistAndType(playlistId, type)

    fun observeChannels(playlistId: Long): Flow<List<ChannelEntity>> =
        channelDao.observeByPlaylist(playlistId)

    suspend fun getChannelsOnce(playlistId: Long): List<ChannelEntity> =
        channelDao.getAllForPlaylist(playlistId)

    suspend fun getChannelById(id: Long): ChannelEntity? = channelDao.getById(id)

    fun observeMovies(playlistId: Long): Flow<List<MovieEntity>> =
        movieDao.observeByPlaylist(playlistId)

    fun observeRecentlyAddedMovies(playlistId: Long): Flow<List<MovieEntity>> =
        movieDao.observeRecentlyAdded(playlistId)

    fun observeMovie(id: Long): Flow<MovieEntity?> = movieDao.observeById(id)

    fun observeSeriesList(playlistId: Long): Flow<List<SeriesEntity>> =
        seriesDao.observeByPlaylist(playlistId)

    fun observeRecentlyAddedSeries(playlistId: Long): Flow<List<SeriesEntity>> =
        seriesDao.observeRecentlyAdded(playlistId)

    fun observeSeries(id: Long): Flow<SeriesEntity?> = seriesDao.observeById(id)

    fun observeEpisodes(playlistId: Long, seriesRemoteId: String): Flow<List<EpisodeEntity>> =
        episodeDao.observeEpisodes(playlistId, seriesRemoteId)

    suspend fun getEpisodesOnce(playlistId: Long, seriesRemoteId: String): List<EpisodeEntity> =
        episodeDao.getEpisodes(playlistId, seriesRemoteId)

    fun observeFavorites(): Flow<List<FavoriteEntity>> = favoriteDao.observeAll()
    fun observeFavoriteKeys(): Flow<List<String>> = favoriteDao.observeFavoriteKeys()

    fun observeWatchHistory(): Flow<List<WatchHistoryEntity>> = watchHistoryDao.observeRecent()
    fun observeContinueWatching(): Flow<List<PlaybackProgressEntity>> = progressDao.observeContinueWatching()
    fun observeSearchHistory(): Flow<List<SearchHistoryEntity>> = searchHistoryDao.observeRecent()

    suspend fun refreshMovieDetails(movieId: Long): Result<MovieEntity> = withContext(Dispatchers.IO) {
        val movie = movieDao.getById(movieId)
            ?: return@withContext Result.failure(IllegalArgumentException("Movie not found."))
        val playlist = playlistDao.getById(movie.playlistId)
            ?: return@withContext Result.success(movie)
        if (playlist.type != PlaylistType.XTREAM) return@withContext Result.success(movie)

        return@withContext try {
            val password = cipher.decrypt(playlist.encryptedPassword)
            val endpoint = "${playlist.serverUrl}/player_api.php"
            val infoResp = apiService.getVodInfo(
                endpointUrl = endpoint,
                username = playlist.username,
                password = password,
                vodId = movie.remoteId
            )
            val info = infoResp.info
            val ext = infoResp.movieData?.containerExtension?.ifBlank { null } ?: movie.containerExtension
            val updated = movie.copy(
                plot = info?.plot?.ifBlank { null } ?: movie.plot,
                genre = info?.genre?.ifBlank { null } ?: movie.genre,
                releaseYear = (info?.releaseDate?.ifBlank { null } ?: info?.releaseDateLower?.ifBlank { null })
                    ?.take(4) ?: movie.releaseYear,
                duration = info?.duration?.ifBlank { null } ?: movie.duration,
                rating = info?.rating?.ifBlank { null } ?: movie.rating,
                posterUrl = info?.movieImage?.ifBlank { null } ?: movie.posterUrl,
                containerExtension = ext,
                streamUrl = "${playlist.serverUrl}/movie/${playlist.username}/$password/${movie.remoteId}.$ext"
            )
            movieDao.update(updated)
            Result.success(updated)
        } catch (t: Throwable) {
            Result.failure(IllegalStateException(NetworkErrorMapper.map(t), t))
        }
    }

    suspend fun refreshSeriesDetails(seriesId: Long): Result<List<EpisodeEntity>> = withContext(Dispatchers.IO) {
        val series = seriesDao.getById(seriesId)
            ?: return@withContext Result.failure(IllegalArgumentException("Series not found."))
        val playlist = playlistDao.getById(series.playlistId)
            ?: return@withContext Result.success(emptyList())
        if (playlist.type != PlaylistType.XTREAM) return@withContext Result.success(emptyList())

        return@withContext try {
            val password = cipher.decrypt(playlist.encryptedPassword)
            val endpoint = "${playlist.serverUrl}/player_api.php"
            val resp = apiService.getSeriesInfo(
                endpointUrl = endpoint,
                username = playlist.username,
                password = password,
                seriesId = series.remoteId
            )

            resp.info?.let { detail ->
                seriesDao.update(
                    series.copy(
                        plot = detail.plot.ifBlank { series.plot },
                        genre = detail.genre.ifBlank { series.genre },
                        releaseDate = detail.releaseDate.ifBlank { series.releaseDate },
                        rating = detail.rating.ifBlank { series.rating },
                        coverUrl = detail.cover.ifBlank { series.coverUrl }
                    )
                )
            }

            val parsedEpisodes = ArrayList<EpisodeEntity>()
            resp.episodes.orEmpty().forEach { (seasonKey, epList) ->
                val seasonNum = seasonKey.toIntOrNull() ?: 1
                epList.forEach { ep ->
                    val ext = ep.containerExtension.ifBlank { "mp4" }
                    val epNum = ep.episodeNum.toIntOrNull() ?: 1
                    parsedEpisodes.add(
                        EpisodeEntity(
                            playlistId = series.playlistId,
                            seriesRemoteId = series.remoteId,
                            remoteId = ep.id,
                            seasonNumber = if (ep.season > 0) ep.season else seasonNum,
                            episodeNumber = epNum,
                            title = ep.title.ifBlank { "Episode $epNum" },
                            streamUrl = "${playlist.serverUrl}/series/${playlist.username}/$password/${ep.id}.$ext",
                            containerExtension = ext,
                            thumbnailUrl = ep.info?.movieImage.orEmpty(),
                            plot = ep.info?.plot.orEmpty(),
                            duration = ep.info?.duration.orEmpty()
                        )
                    )
                }
            }

            episodeDao.deleteForSeries(series.playlistId, series.remoteId)
            if (parsedEpisodes.isNotEmpty()) {
                parsedEpisodes.chunked(200).forEach { episodeDao.insertAll(it) }
            }
            Result.success(parsedEpisodes)
        } catch (t: Throwable) {
            Result.failure(IllegalStateException(NetworkErrorMapper.map(t), t))
        }
    }

    suspend fun toggleFavorite(
        playlistId: Long,
        type: ContentType,
        itemId: Long,
        remoteId: String,
        title: String,
        subtitle: String,
        imageUrl: String,
        streamUrl: String
    ): Boolean {
        val key = buildItemKey(type, itemId, remoteId)
        return if (favoriteDao.isFavorite(key)) {
            favoriteDao.deleteByKey(key)
            false
        } else {
            favoriteDao.insert(
                FavoriteEntity(
                    itemKey = key,
                    playlistId = playlistId,
                    itemType = type,
                    itemId = itemId,
                    remoteId = remoteId,
                    title = title,
                    subtitle = subtitle,
                    imageUrl = imageUrl,
                    streamUrl = streamUrl
                )
            )
            true
        }
    }

    suspend fun removeFavorite(itemKey: String) {
        favoriteDao.deleteByKey(itemKey)
    }

    suspend fun recordWatchHistory(
        playlistId: Long,
        type: ContentType,
        itemId: Long,
        remoteId: String,
        title: String,
        subtitle: String,
        imageUrl: String,
        streamUrl: String
    ) {
        val key = buildItemKey(type, itemId, remoteId)
        watchHistoryDao.upsert(
            WatchHistoryEntity(
                itemKey = key,
                playlistId = playlistId,
                itemType = type,
                itemId = itemId,
                remoteId = remoteId,
                title = title,
                subtitle = subtitle,
                imageUrl = imageUrl,
                streamUrl = streamUrl
            )
        )
    }

    suspend fun savePlaybackProgress(
        playlistId: Long,
        type: ContentType,
        itemId: Long,
        remoteId: String,
        title: String,
        subtitle: String,
        imageUrl: String,
        streamUrl: String,
        positionMs: Long,
        durationMs: Long
    ) {
        if (type == ContentType.LIVE || durationMs <= 0L) return
        val key = buildItemKey(type, itemId, remoteId)
        progressDao.upsert(
            PlaybackProgressEntity(
                itemKey = key,
                playlistId = playlistId,
                itemType = type,
                itemId = itemId,
                title = title,
                subtitle = subtitle,
                imageUrl = imageUrl,
                streamUrl = streamUrl,
                positionMs = positionMs,
                durationMs = durationMs
            )
        )
    }

    suspend fun getSavedPositionMs(type: ContentType, itemId: Long, remoteId: String): Long {
        val key = buildItemKey(type, itemId, remoteId)
        return progressDao.getByKey(key)?.positionMs ?: 0L
    }

    suspend fun searchLocal(playlistId: Long, query: String): GlobalSearchResult = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext GlobalSearchResult()
        GlobalSearchResult(
            channels = channelDao.search(playlistId, trimmed),
            movies = movieDao.search(playlistId, trimmed),
            series = seriesDao.search(playlistId, trimmed),
            categories = categoryDao.searchCategories(playlistId, trimmed)
        )
    }

    suspend fun saveSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.length >= 2) {
            searchHistoryDao.upsert(SearchHistoryEntity(query = trimmed))
        }
    }

    suspend fun deleteSearchQuery(query: String) {
        searchHistoryDao.delete(query)
    }

    suspend fun clearSearchHistory() {
        searchHistoryDao.clearAll()
    }

    suspend fun clearWatchAndProgressCache() {
        watchHistoryDao.clearAll()
        progressDao.clearAll()
    }

    companion object {
        fun buildItemKey(type: ContentType, itemId: Long, remoteId: String): String =
            "${type.name}:${itemId}:${remoteId}"
    }
}
