package com.gracetv.iptv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.gracetv.iptv.data.local.entity.CategoryEntity
import com.gracetv.iptv.data.local.entity.ChannelEntity
import com.gracetv.iptv.data.local.entity.EpgProgrammeEntity
import com.gracetv.iptv.data.local.entity.EpisodeEntity
import com.gracetv.iptv.data.local.entity.FavoriteEntity
import com.gracetv.iptv.data.local.entity.MovieEntity
import com.gracetv.iptv.data.local.entity.PlaybackProgressEntity
import com.gracetv.iptv.data.local.entity.PlaylistEntity
import com.gracetv.iptv.data.local.entity.SearchHistoryEntity
import com.gracetv.iptv.data.local.entity.SeriesEntity
import com.gracetv.iptv.data.local.entity.WatchHistoryEntity
import com.gracetv.iptv.domain.model.ContentType
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY isDefault DESC, lastUpdatedMillis DESC")
    fun observeAll(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE isDefault = 1 LIMIT 1")
    fun observeDefault(): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getById(id: Long): PlaylistEntity?

    @Query("SELECT * FROM playlists")
    suspend fun getAllOnce(): List<PlaylistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: PlaylistEntity): Long

    @Update
    suspend fun update(playlist: PlaylistEntity)

    @Query("UPDATE playlists SET isDefault = CASE WHEN id = :playlistId THEN 1 ELSE 0 END")
    suspend fun setDefault(playlistId: Long)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deleteById(playlistId: Long)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE playlistId = :playlistId AND contentType = :type ORDER BY name ASC")
    fun observeByPlaylistAndType(playlistId: Long, type: ContentType): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE playlistId = :playlistId AND LOWER(name) LIKE '%' || LOWER(:query) || '%'")
    suspend fun searchCategories(playlistId: Long, query: String): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylist(playlistId: Long)
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels WHERE playlistId = :playlistId ORDER BY sortOrder ASC, name ASC")
    fun observeByPlaylist(playlistId: Long): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND categoryId = :categoryId ORDER BY sortOrder ASC, name ASC")
    fun observeByCategory(playlistId: Long, categoryId: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE id = :id")
    suspend fun getById(id: Long): ChannelEntity?

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId ORDER BY sortOrder ASC, name ASC")
    suspend fun getAllForPlaylist(playlistId: Long): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND LOWER(name) LIKE '%' || LOWER(:query) || '%' ORDER BY name ASC LIMIT 120")
    suspend fun search(playlistId: Long, query: String): List<ChannelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<ChannelEntity>)

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylist(playlistId: Long)
}

@Dao
interface MovieDao {
    @Query("SELECT * FROM movies WHERE playlistId = :playlistId ORDER BY id ASC")
    fun observeByPlaylist(playlistId: Long): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE playlistId = :playlistId ORDER BY addedTimestamp DESC LIMIT 20")
    fun observeRecentlyAdded(playlistId: Long): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE id = :id")
    fun observeById(id: Long): Flow<MovieEntity?>

    @Query("SELECT * FROM movies WHERE id = :id")
    suspend fun getById(id: Long): MovieEntity?

    @Update
    suspend fun update(movie: MovieEntity)

    @Query("SELECT * FROM movies WHERE playlistId = :playlistId AND LOWER(name) LIKE '%' || LOWER(:query) || '%' ORDER BY name ASC LIMIT 100")
    suspend fun search(playlistId: Long, query: String): List<MovieEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movies: List<MovieEntity>)

    @Query("DELETE FROM movies WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylist(playlistId: Long)
}

@Dao
interface SeriesDao {
    @Query("SELECT * FROM series WHERE playlistId = :playlistId ORDER BY id ASC")
    fun observeByPlaylist(playlistId: Long): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE playlistId = :playlistId ORDER BY lastModified DESC LIMIT 20")
    fun observeRecentlyAdded(playlistId: Long): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE id = :id")
    fun observeById(id: Long): Flow<SeriesEntity?>

    @Query("SELECT * FROM series WHERE id = :id")
    suspend fun getById(id: Long): SeriesEntity?

    @Update
    suspend fun update(series: SeriesEntity)

    @Query("SELECT * FROM series WHERE playlistId = :playlistId AND LOWER(name) LIKE '%' || LOWER(:query) || '%' ORDER BY name ASC LIMIT 100")
    suspend fun search(playlistId: Long, query: String): List<SeriesEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(series: List<SeriesEntity>)

    @Query("DELETE FROM series WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylist(playlistId: Long)
}

@Dao
interface EpisodeDao {
    @Query("SELECT * FROM episodes WHERE playlistId = :playlistId AND seriesRemoteId = :seriesRemoteId ORDER BY seasonNumber ASC, episodeNumber ASC")
    fun observeEpisodes(playlistId: Long, seriesRemoteId: String): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE playlistId = :playlistId AND seriesRemoteId = :seriesRemoteId ORDER BY seasonNumber ASC, episodeNumber ASC")
    suspend fun getEpisodes(playlistId: Long, seriesRemoteId: String): List<EpisodeEntity>

    @Query("SELECT * FROM episodes WHERE id = :id")
    suspend fun getById(id: Long): EpisodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(episodes: List<EpisodeEntity>)

    @Query("DELETE FROM episodes WHERE playlistId = :playlistId AND seriesRemoteId = :seriesRemoteId")
    suspend fun deleteForSeries(playlistId: Long, seriesRemoteId: String)

    @Query("DELETE FROM episodes WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylist(playlistId: Long)
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT itemKey FROM favorites")
    fun observeFavoriteKeys(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE itemKey = :key)")
    suspend fun isFavorite(key: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE itemKey = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM favorites WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylist(playlistId: Long)
}

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history ORDER BY watchedAt DESC LIMIT 30")
    fun observeRecent(): Flow<List<WatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: WatchHistoryEntity)

    @Query("DELETE FROM watch_history")
    suspend fun clearAll()
}

@Dao
interface PlaybackProgressDao {
    @Query("SELECT * FROM playback_progress WHERE durationMs > 0 AND positionMs > 5000 AND positionMs < (durationMs - 10000) ORDER BY updatedAt DESC LIMIT 20")
    fun observeContinueWatching(): Flow<List<PlaybackProgressEntity>>

    @Query("SELECT * FROM playback_progress WHERE itemKey = :key LIMIT 1")
    suspend fun getByKey(key: String): PlaybackProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: PlaybackProgressEntity)

    @Query("DELETE FROM playback_progress")
    suspend fun clearAll()
}

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT 12")
    fun observeRecent(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE `query` = :query")
    suspend fun delete(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearAll()
}

@Dao
interface EpgDao {
    @Query("SELECT * FROM epg_programmes WHERE LOWER(channelTvgId) = LOWER(:tvgId) AND endTimeMs > :fromMs ORDER BY startTimeMs ASC LIMIT 40")
    fun observeProgrammesForChannel(tvgId: String, fromMs: Long): Flow<List<EpgProgrammeEntity>>

    @Query("SELECT * FROM epg_programmes WHERE endTimeMs > :nowMs AND startTimeMs <= :horizonMs ORDER BY startTimeMs ASC")
    fun observeUpcomingWindow(nowMs: Long, horizonMs: Long): Flow<List<EpgProgrammeEntity>>

    @Query("SELECT COUNT(*) FROM epg_programmes")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(programmes: List<EpgProgrammeEntity>)

    @Query("DELETE FROM epg_programmes")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(programmes: List<EpgProgrammeEntity>) {
        clearAll()
        programmes.chunked(500).forEach { insertAll(it) }
    }
}
