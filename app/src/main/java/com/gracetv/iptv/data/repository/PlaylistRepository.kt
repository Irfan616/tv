package com.gracetv.iptv.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.gracetv.iptv.data.local.GraceTvDatabase
import com.gracetv.iptv.data.local.PreferencesManager
import com.gracetv.iptv.data.local.entity.CategoryEntity
import com.gracetv.iptv.data.local.entity.ChannelEntity
import com.gracetv.iptv.data.local.entity.MovieEntity
import com.gracetv.iptv.data.local.entity.PlaylistEntity
import com.gracetv.iptv.data.local.entity.SeriesEntity
import com.gracetv.iptv.data.remote.M3uParser
import com.gracetv.iptv.data.remote.XtreamApiService
import com.gracetv.iptv.domain.model.ConnectionStatus
import com.gracetv.iptv.domain.model.ContentType
import com.gracetv.iptv.domain.model.ImportProgress
import com.gracetv.iptv.domain.model.ParsedM3uResult
import com.gracetv.iptv.domain.model.PlaylistType
import com.gracetv.iptv.security.CredentialCipher
import com.gracetv.iptv.utils.UrlValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlaylistRepository(
    private val context: Context,
    private val database: GraceTvDatabase,
    private val apiService: XtreamApiService,
    private val m3uParser: M3uParser,
    private val cipher: CredentialCipher,
    private val preferencesManager: PreferencesManager
) {
    private val playlistDao = database.playlistDao()
    private val categoryDao = database.categoryDao()
    private val channelDao = database.channelDao()
    private val movieDao = database.movieDao()
    private val seriesDao = database.seriesDao()
    private val episodeDao = database.episodeDao()
    private val favoriteDao = database.favoriteDao()

    fun observePlaylists(): Flow<List<PlaylistEntity>> = playlistDao.observeAll()
    fun observeDefaultPlaylist(): Flow<PlaylistEntity?> = playlistDao.observeDefault()

    suspend fun getPlaylistById(id: Long): PlaylistEntity? = playlistDao.getById(id)

    fun decryptPassword(playlist: PlaylistEntity): String =
        cipher.decrypt(playlist.encryptedPassword)

    suspend fun setDefaultPlaylist(playlistId: Long) {
        playlistDao.setDefault(playlistId)
    }

    suspend fun deletePlaylist(playlistId: Long) {
        val existing = playlistDao.getById(playlistId)
        database.withTransaction {
            channelDao.deleteByPlaylist(playlistId)
            categoryDao.deleteByPlaylist(playlistId)
            movieDao.deleteByPlaylist(playlistId)
            seriesDao.deleteByPlaylist(playlistId)
            episodeDao.deleteByPlaylist(playlistId)
            favoriteDao.deleteByPlaylist(playlistId)
            playlistDao.deleteById(playlistId)
        }
        if (existing?.isDefault == true) {
            playlistDao.getAllOnce().firstOrNull()?.let { next ->
                playlistDao.setDefault(next.id)
            }
        }
    }

    suspend fun importXtreamPlaylist(
        existingId: Long? = null,
        name: String,
        rawServerUrl: String,
        username: String,
        password: String,
        onProgress: (ImportProgress) -> Unit = {}
    ): Result<PlaylistEntity> = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        val cleanUser = username.trim()
        if (cleanName.isEmpty()) return@withContext Result.failure(IllegalArgumentException("Playlist name is required."))
        if (cleanUser.isEmpty()) return@withContext Result.failure(IllegalArgumentException("Username is required."))
        if (password.isEmpty()) return@withContext Result.failure(IllegalArgumentException("Password is required."))

        val baseUrl = UrlValidator.normalizeBaseUrl(rawServerUrl)
            ?: return@withContext Result.failure(IllegalArgumentException("Enter a valid server URL (e.g. https://example.com:8080)."))

        val apiEndpoint = "$baseUrl/player_api.php"

        try {
            onProgress(ImportProgress("Authenticating with server...", 0.12f))
            val authResp = apiService.authenticate(apiEndpoint, cleanUser, password)
            val userInfo = authResp.userInfo
                ?: return@withContext Result.failure(IllegalStateException("Invalid server response: missing account details."))

            val isAuthorized = userInfo.auth == "1" ||
                userInfo.status.equals("Active", ignoreCase = true)
            if (!isAuthorized) {
                val reason = userInfo.message.ifBlank {
                    "Authentication failed (status: ${userInfo.status.ifBlank { "Unauthorized" }})."
                }
                return@withContext Result.failure(IllegalStateException(reason))
            }

            onProgress(ImportProgress("Loading categories & catalogs...", 0.30f))
            val (liveCats, liveStreams, vodCats, vodStreams, seriesCats, seriesList) = coroutineScope {
                val lc = async { runCatching { apiService.getLiveCategories(apiEndpoint, cleanUser, password) }.getOrDefault(emptyList()) }
                val ls = async { runCatching { apiService.getLiveStreams(apiEndpoint, cleanUser, password) }.getOrDefault(emptyList()) }
                val vc = async { runCatching { apiService.getVodCategories(apiEndpoint, cleanUser, password) }.getOrDefault(emptyList()) }
                val vs = async { runCatching { apiService.getVodStreams(apiEndpoint, cleanUser, password) }.getOrDefault(emptyList()) }
                val sc = async { runCatching { apiService.getSeriesCategories(apiEndpoint, cleanUser, password) }.getOrDefault(emptyList()) }
                val sl = async { runCatching { apiService.getSeries(apiEndpoint, cleanUser, password) }.getOrDefault(emptyList()) }
                XtreamFetchBundle(lc.await(), ls.await(), vc.await(), vs.await(), sc.await(), sl.await())
            }

            if (liveStreams.isEmpty() && vodStreams.isEmpty() && seriesList.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException("Connected to server, but no live channels, movies, or series were returned.")
                )
            }

            onProgress(
                ImportProgress(
                    "Saving ${liveStreams.size} channels, ${vodStreams.size} movies, ${seriesList.size} series...",
                    0.72f,
                    liveStreams.size + vodStreams.size + seriesList.size
                )
            )

            val shouldBeDefault = existingId?.let { playlistDao.getById(it)?.isDefault }
                ?: (playlistDao.getDefault() == null)

            val formattedExpiry = formatUnixExpiry(userInfo.expDate)
            val encryptedPw = cipher.encrypt(password)
            val xmltvUrl = "$baseUrl/xmltv.php?username=$cleanUser&password=$password"

            val entity = PlaylistEntity(
                id = existingId ?: 0L,
                name = cleanName,
                type = PlaylistType.XTREAM,
                serverUrl = baseUrl,
                username = cleanUser,
                encryptedPassword = encryptedPw,
                epgUrl = xmltvUrl,
                isDefault = shouldBeDefault,
                status = ConnectionStatus.CONNECTED,
                errorMessage = null,
                channelCount = liveStreams.size,
                movieCount = vodStreams.size,
                seriesCount = seriesList.size,
                accountStatus = userInfo.status.ifBlank { "Active" },
                accountExpiry = formattedExpiry,
                maxConnections = userInfo.maxConnections.ifBlank { null },
                activeConnections = userInfo.activeCons.ifBlank { null },
                lastUpdatedMillis = System.currentTimeMillis()
            )

            val liveCatMap = liveCats.associate { it.categoryId to it.categoryName }
            val vodCatMap = vodCats.associate { it.categoryId to it.categoryName }
            val seriesCatMap = seriesCats.associate { it.categoryId to it.categoryName }

            val savedId = saveXtreamDataInTransaction(
                entity = entity,
                baseUrl = baseUrl,
                username = cleanUser,
                password = password,
                liveCatMap = liveCatMap,
                vodCatMap = vodCatMap,
                seriesCatMap = seriesCatMap,
                liveCats = liveCats,
                liveStreams = liveStreams,
                vodCats = vodCats,
                vodStreams = vodStreams,
                seriesCats = seriesCats,
                seriesList = seriesList
            )

            val saved = playlistDao.getById(savedId) ?: entity.copy(id = savedId)
            onProgress(ImportProgress("Import complete!", 1f, liveStreams.size + vodStreams.size + seriesList.size))
            Result.success(saved)
        } catch (t: Throwable) {
            Result.failure(IllegalStateException(NetworkErrorMapper.map(t), t))
        }
    }

    suspend fun importM3uUrl(
        existingId: Long? = null,
        name: String,
        m3uUrl: String,
        onProgress: (ImportProgress) -> Unit = {}
    ): Result<PlaylistEntity> = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        val cleanUrl = m3uUrl.trim()
        if (cleanName.isEmpty()) return@withContext Result.failure(IllegalArgumentException("Playlist name is required."))
        if (!UrlValidator.isValidStreamOrPlaylistUrl(cleanUrl)) {
            return@withContext Result.failure(IllegalArgumentException("Enter a valid http:// or https:// M3U playlist URL."))
        }

        try {
            onProgress(ImportProgress("Downloading M3U playlist...", 0.15f))
            val body = apiService.downloadRaw(cleanUrl)
            val parsed = body.byteStream().use { stream ->
                m3uParser.parse(stream) { count ->
                    onProgress(ImportProgress("Parsed $count entries...", 0.55f, count))
                }
            }
            saveParsedM3uPlaylist(
                existingId = existingId,
                name = cleanName,
                type = PlaylistType.M3U_URL,
                m3uUrl = cleanUrl,
                fileUri = "",
                parsed = parsed,
                onProgress = onProgress
            )
        } catch (t: Throwable) {
            Result.failure(IllegalStateException(NetworkErrorMapper.map(t), t))
        }
    }

    suspend fun importM3uFile(
        existingId: Long? = null,
        name: String,
        fileUri: Uri,
        onProgress: (ImportProgress) -> Unit = {}
    ): Result<PlaylistEntity> = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return@withContext Result.failure(IllegalArgumentException("Playlist name is required."))

        try {
            onProgress(ImportProgress("Reading M3U file...", 0.20f))
            val parsed = context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                m3uParser.parse(inputStream) { count ->
                    onProgress(ImportProgress("Parsed $count entries...", 0.55f, count))
                }
            } ?: return@withContext Result.failure(IllegalStateException("Could not open the selected file."))

            saveParsedM3uPlaylist(
                existingId = existingId,
                name = cleanName,
                type = PlaylistType.M3U_FILE,
                m3uUrl = "",
                fileUri = fileUri.toString(),
                parsed = parsed,
                onProgress = onProgress
            )
        } catch (t: Throwable) {
            Result.failure(IllegalStateException("Failed to import M3U file: ${t.message ?: "Unknown error"}", t))
        }
    }

    suspend fun refreshPlaylist(
        playlistId: Long,
        onProgress: (ImportProgress) -> Unit = {}
    ): Result<PlaylistEntity> {
        val playlist = playlistDao.getById(playlistId)
            ?: return Result.failure(IllegalArgumentException("Playlist not found."))

        playlistDao.update(playlist.copy(status = ConnectionStatus.SYNCING, errorMessage = null))

        val result = when (playlist.type) {
            PlaylistType.XTREAM -> {
                val password = cipher.decrypt(playlist.encryptedPassword)
                importXtreamPlaylist(
                    existingId = playlist.id,
                    name = playlist.name,
                    rawServerUrl = playlist.serverUrl,
                    username = playlist.username,
                    password = password,
                    onProgress = onProgress
                )
            }
            PlaylistType.M3U_URL -> importM3uUrl(
                existingId = playlist.id,
                name = playlist.name,
                m3uUrl = playlist.m3uUrl,
                onProgress = onProgress
            )
            PlaylistType.M3U_FILE -> {
                val uri = runCatching { Uri.parse(playlist.fileUri) }.getOrNull()
                    ?: return Result.failure(IllegalStateException("Saved file URI is invalid."))
                importM3uFile(
                    existingId = playlist.id,
                    name = playlist.name,
                    fileUri = uri,
                    onProgress = onProgress
                )
            }
        }

        result.onFailure { err ->
            playlistDao.update(
                playlist.copy(
                    status = ConnectionStatus.ERROR,
                    errorMessage = err.message ?: "Refresh failed"
                )
            )
        }
        return result
    }

    suspend fun updatePlaylistMetadata(playlistId: Long, newName: String) {
        val clean = newName.trim()
        if (clean.isEmpty()) return
        playlistDao.getById(playlistId)?.let { existing ->
            playlistDao.update(existing.copy(name = clean))
        }
    }

    private suspend fun saveParsedM3uPlaylist(
        existingId: Long?,
        name: String,
        type: PlaylistType,
        m3uUrl: String,
        fileUri: String,
        parsed: ParsedM3uResult,
        onProgress: (ImportProgress) -> Unit
    ): Result<PlaylistEntity> {
        if (parsed.items.isEmpty()) {
            return Result.failure(IllegalStateException("The M3U playlist contains no valid playable streams."))
        }

        val liveItems = parsed.items.filter { !it.isVod }
        val vodItems = parsed.items.filter { it.isVod }

        onProgress(ImportProgress("Saving ${parsed.items.size} playlist items...", 0.78f, parsed.items.size))

        val shouldBeDefault = existingId?.let { playlistDao.getById(it)?.isDefault }
            ?: (playlistDao.getDefault() == null)

        val entity = PlaylistEntity(
            id = existingId ?: 0L,
            name = name,
            type = type,
            m3uUrl = m3uUrl,
            fileUri = fileUri,
            epgUrl = parsed.epgUrl.orEmpty(),
            isDefault = shouldBeDefault,
            status = ConnectionStatus.CONNECTED,
            errorMessage = null,
            channelCount = liveItems.size,
            movieCount = vodItems.size,
            seriesCount = 0,
            lastUpdatedMillis = System.currentTimeMillis()
        )

        val savedId = database.withTransaction {
            val playlistId = playlistDao.insert(entity)
            channelDao.deleteByPlaylist(playlistId)
            categoryDao.deleteByPlaylist(playlistId)
            movieDao.deleteByPlaylist(playlistId)
            seriesDao.deleteByPlaylist(playlistId)
            episodeDao.deleteByPlaylist(playlistId)

            val liveCategories = liveItems.map { it.groupTitle }.distinct().map { group ->
                CategoryEntity(
                    playlistId = playlistId,
                    remoteId = group,
                    name = group,
                    contentType = ContentType.LIVE
                )
            }
            val vodCategories = vodItems.map { it.groupTitle }.distinct().map { group ->
                CategoryEntity(
                    playlistId = playlistId,
                    remoteId = group,
                    name = group,
                    contentType = ContentType.MOVIE
                )
            }
            (liveCategories + vodCategories).chunked(500).forEach { categoryDao.insertAll(it) }

            val channels = liveItems.mapIndexed { idx, item ->
                ChannelEntity(
                    playlistId = playlistId,
                    categoryId = item.groupTitle,
                    categoryName = item.groupTitle,
                    remoteId = "m3u_$idx",
                    name = item.name,
                    streamUrl = item.streamUrl,
                    logoUrl = item.tvgLogo,
                    tvgId = item.tvgId,
                    tvgName = item.tvgName,
                    sortOrder = idx
                )
            }
            channels.chunked(500).forEach { channelDao.insertAll(it) }

            val movies = vodItems.mapIndexed { idx, item ->
                MovieEntity(
                    playlistId = playlistId,
                    categoryId = item.groupTitle,
                    categoryName = item.groupTitle,
                    remoteId = "m3u_vod_$idx",
                    name = item.name,
                    streamUrl = item.streamUrl,
                    posterUrl = item.tvgLogo,
                    containerExtension = item.containerExtension.ifBlank { "mp4" }
                )
            }
            movies.chunked(500).forEach { movieDao.insertAll(it) }

            playlistId
        }

        if (!parsed.epgUrl.isNullOrBlank()) {
            preferencesManager.setEpgUrl(parsed.epgUrl)
        }

        val saved = playlistDao.getById(savedId) ?: entity.copy(id = savedId)
        onProgress(ImportProgress("Imported ${parsed.items.size} streams", 1f, parsed.items.size))
        return Result.success(saved)
    }

    private suspend fun saveXtreamDataInTransaction(
        entity: PlaylistEntity,
        baseUrl: String,
        username: String,
        password: String,
        liveCatMap: Map<String, String>,
        vodCatMap: Map<String, String>,
        seriesCatMap: Map<String, String>,
        liveCats: List<com.gracetv.iptv.data.remote.XtreamCategoryDto>,
        liveStreams: List<com.gracetv.iptv.data.remote.XtreamLiveStreamDto>,
        vodCats: List<com.gracetv.iptv.data.remote.XtreamCategoryDto>,
        vodStreams: List<com.gracetv.iptv.data.remote.XtreamVodStreamDto>,
        seriesCats: List<com.gracetv.iptv.data.remote.XtreamCategoryDto>,
        seriesList: List<com.gracetv.iptv.data.remote.XtreamSeriesDto>
    ): Long = database.withTransaction {
        val playlistId = playlistDao.insert(entity)
        channelDao.deleteByPlaylist(playlistId)
        categoryDao.deleteByPlaylist(playlistId)
        movieDao.deleteByPlaylist(playlistId)
        seriesDao.deleteByPlaylist(playlistId)
        episodeDao.deleteByPlaylist(playlistId)

        val categories = buildList {
            liveCats.forEach {
                add(CategoryEntity(playlistId = playlistId, remoteId = it.categoryId, name = it.categoryName.ifBlank { "General" }, contentType = ContentType.LIVE))
            }
            vodCats.forEach {
                add(CategoryEntity(playlistId = playlistId, remoteId = it.categoryId, name = it.categoryName.ifBlank { "General" }, contentType = ContentType.MOVIE))
            }
            seriesCats.forEach {
                add(CategoryEntity(playlistId = playlistId, remoteId = it.categoryId, name = it.categoryName.ifBlank { "General" }, contentType = ContentType.SERIES))
            }
        }
        categories.chunked(500).forEach { categoryDao.insertAll(it) }

        val channels = liveStreams.mapIndexed { idx, dto ->
            val catName = liveCatMap[dto.categoryId].orEmpty().ifBlank { "General" }
            val streamUrl = "$baseUrl/live/$username/$password/${dto.streamId}.m3u8"
            ChannelEntity(
                playlistId = playlistId,
                categoryId = dto.categoryId,
                categoryName = catName,
                remoteId = dto.streamId,
                name = dto.name.ifBlank { "Channel ${dto.streamId}" },
                streamUrl = streamUrl,
                logoUrl = dto.streamIcon,
                tvgId = dto.epgChannelId,
                tvgName = dto.name,
                sortOrder = dto.num.toIntOrNull() ?: idx,
                isAdult = dto.isAdult == "1"
            )
        }
        channels.chunked(500).forEach { channelDao.insertAll(it) }

        val movies = vodStreams.map { dto ->
            val ext = dto.containerExtension.ifBlank { "mp4" }
            val catName = vodCatMap[dto.categoryId].orEmpty().ifBlank { "Movies" }
            MovieEntity(
                playlistId = playlistId,
                categoryId = dto.categoryId,
                categoryName = catName,
                remoteId = dto.streamId,
                name = dto.name.ifBlank { "Movie ${dto.streamId}" },
                streamUrl = "$baseUrl/movie/$username/$password/${dto.streamId}.$ext",
                posterUrl = dto.streamIcon,
                containerExtension = ext,
                rating = dto.rating,
                addedTimestamp = dto.added.toLongOrNull() ?: 0L,
                isAdult = dto.isAdult == "1"
            )
        }
        movies.chunked(500).forEach { movieDao.insertAll(it) }

        val series = seriesList.map { dto ->
            val catName = seriesCatMap[dto.categoryId].orEmpty().ifBlank { "Series" }
            SeriesEntity(
                playlistId = playlistId,
                categoryId = dto.categoryId,
                categoryName = catName,
                remoteId = dto.seriesId,
                name = dto.name.ifBlank { "Series ${dto.seriesId}" },
                coverUrl = dto.cover,
                plot = dto.plot,
                genre = dto.genre,
                releaseDate = dto.releaseDate,
                rating = dto.rating,
                lastModified = dto.lastModified.toLongOrNull() ?: 0L
            )
        }
        series.chunked(500).forEach { seriesDao.insertAll(it) }

        playlistId
    }

    private fun formatUnixExpiry(expDateRaw: String): String? {
        val seconds = expDateRaw.trim().toLongOrNull() ?: return null
        if (seconds <= 0L) return "Unlimited"
        val df = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        return df.format(Date(seconds * 1000L))
    }

    private data class XtreamFetchBundle(
        val liveCats: List<com.gracetv.iptv.data.remote.XtreamCategoryDto>,
        val liveStreams: List<com.gracetv.iptv.data.remote.XtreamLiveStreamDto>,
        val vodCats: List<com.gracetv.iptv.data.remote.XtreamCategoryDto>,
        val vodStreams: List<com.gracetv.iptv.data.remote.XtreamVodStreamDto>,
        val seriesCats: List<com.gracetv.iptv.data.remote.XtreamCategoryDto>,
        val seriesList: List<com.gracetv.iptv.data.remote.XtreamSeriesDto>
    )
}
