package com.gracetv.iptv.data.repository

import com.gracetv.iptv.data.local.GraceTvDatabase
import com.gracetv.iptv.data.local.PreferencesManager
import com.gracetv.iptv.data.local.entity.EpgProgrammeEntity
import com.gracetv.iptv.data.remote.XmltvParser
import com.gracetv.iptv.data.remote.XtreamApiService
import com.gracetv.iptv.utils.UrlValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class EpgRepository(
    private val database: GraceTvDatabase,
    private val apiService: XtreamApiService,
    private val xmltvParser: XmltvParser,
    private val preferencesManager: PreferencesManager
) {
    private val epgDao = database.epgDao()

    fun observeProgrammeCount(): Flow<Int> = epgDao.observeCount()

    fun observeUpcomingWindow(nowMs: Long, horizonMs: Long): Flow<List<EpgProgrammeEntity>> =
        epgDao.observeUpcomingWindow(nowMs, horizonMs)

    fun observeProgrammesForChannel(tvgId: String, fromMs: Long): Flow<List<EpgProgrammeEntity>> =
        epgDao.observeProgrammesForChannel(tvgId, fromMs)

    suspend fun refreshEpg(url: String): Result<Int> = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()
        if (!UrlValidator.isValidStreamOrPlaylistUrl(cleanUrl)) {
            return@withContext Result.failure(IllegalArgumentException("Enter a valid http:// or https:// XMLTV EPG URL."))
        }

        try {
            val body = apiService.downloadRaw(cleanUrl)
            val parsed = body.byteStream().use { stream ->
                xmltvParser.parse(stream)
            }

            if (parsed.programmes.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException("XMLTV file was downloaded, but no valid programmes were found.")
                )
            }

            val entities = parsed.programmes.map { p ->
                EpgProgrammeEntity(
                    channelTvgId = p.channelTvgId,
                    title = p.title,
                    description = p.description,
                    category = p.category,
                    startTimeMs = p.startTimeMs,
                    endTimeMs = p.endTimeMs
                )
            }
            epgDao.replaceAll(entities)
            preferencesManager.setEpgUrl(cleanUrl)
            preferencesManager.setEpgLastSyncMillis(System.currentTimeMillis())
            Result.success(entities.size)
        } catch (t: Throwable) {
            Result.failure(IllegalStateException(NetworkErrorMapper.map(t), t))
        }
    }

    suspend fun clearEpgCache() {
        epgDao.clearAll()
    }
}
