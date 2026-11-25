package com.gracetv.iptv.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.gracetv.iptv.data.local.dao.CategoryDao
import com.gracetv.iptv.data.local.dao.ChannelDao
import com.gracetv.iptv.data.local.dao.EpgDao
import com.gracetv.iptv.data.local.dao.EpisodeDao
import com.gracetv.iptv.data.local.dao.FavoriteDao
import com.gracetv.iptv.data.local.dao.MovieDao
import com.gracetv.iptv.data.local.dao.PlaybackProgressDao
import com.gracetv.iptv.data.local.dao.PlaylistDao
import com.gracetv.iptv.data.local.dao.SearchHistoryDao
import com.gracetv.iptv.data.local.dao.SeriesDao
import com.gracetv.iptv.data.local.dao.WatchHistoryDao
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
import com.gracetv.iptv.domain.model.ConnectionStatus
import com.gracetv.iptv.domain.model.ContentType
import com.gracetv.iptv.domain.model.PlaylistType

class GraceTvConverters {
    @TypeConverter
    fun fromPlaylistType(value: PlaylistType): String = value.name

    @TypeConverter
    fun toPlaylistType(value: String): PlaylistType =
        runCatching { PlaylistType.valueOf(value) }.getOrDefault(PlaylistType.M3U_URL)

    @TypeConverter
    fun fromConnectionStatus(value: ConnectionStatus): String = value.name

    @TypeConverter
    fun toConnectionStatus(value: String): ConnectionStatus =
        runCatching { ConnectionStatus.valueOf(value) }.getOrDefault(ConnectionStatus.DISCONNECTED)

    @TypeConverter
    fun fromContentType(value: ContentType): String = value.name

    @TypeConverter
    fun toContentType(value: String): ContentType =
        runCatching { ContentType.valueOf(value) }.getOrDefault(ContentType.LIVE)
}

@Database(
    entities = [
        PlaylistEntity::class,
        CategoryEntity::class,
        ChannelEntity::class,
        MovieEntity::class,
        SeriesEntity::class,
        EpisodeEntity::class,
        FavoriteEntity::class,
        WatchHistoryEntity::class,
        PlaybackProgressEntity::class,
        SearchHistoryEntity::class,
        EpgProgrammeEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(GraceTvConverters::class)
abstract class GraceTvDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun categoryDao(): CategoryDao
    abstract fun channelDao(): ChannelDao
    abstract fun movieDao(): MovieDao
    abstract fun seriesDao(): SeriesDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun playbackProgressDao(): PlaybackProgressDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun epgDao(): EpgDao

    companion object {
        @Volatile
        private var INSTANCE: GraceTvDatabase? = null

        fun getInstance(context: Context): GraceTvDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GraceTvDatabase::class.java,
                    "grace_tv_database.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
