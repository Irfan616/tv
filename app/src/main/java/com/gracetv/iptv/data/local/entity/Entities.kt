package com.gracetv.iptv.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gracetv.iptv.domain.model.ConnectionStatus
import com.gracetv.iptv.domain.model.ContentType
import com.gracetv.iptv.domain.model.PlaylistType

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: PlaylistType,
    val serverUrl: String = "",
    val username: String = "",
    val encryptedPassword: String = "",
    val m3uUrl: String = "",
    val fileUri: String = "",
    val epgUrl: String = "",
    val isDefault: Boolean = false,
    val status: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val errorMessage: String? = null,
    val channelCount: Int = 0,
    val movieCount: Int = 0,
    val seriesCount: Int = 0,
    val accountStatus: String? = null,
    val accountExpiry: String? = null,
    val maxConnections: String? = null,
    val activeConnections: String? = null,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "categories",
    indices = [Index("playlistId"), Index("contentType")]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val remoteId: String,
    val name: String,
    val contentType: ContentType,
    val isAdult: Boolean = false
)

@Entity(
    tableName = "channels",
    indices = [Index("playlistId"), Index("categoryId"), Index("tvgId"), Index("name")]
)
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val categoryId: String,
    val categoryName: String,
    val remoteId: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String = "",
    val tvgId: String = "",
    val tvgName: String = "",
    val sortOrder: Int = 0,
    val isAdult: Boolean = false
)

@Entity(
    tableName = "movies",
    indices = [Index("playlistId"), Index("categoryId"), Index("name")]
)
data class MovieEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val categoryId: String,
    val categoryName: String,
    val remoteId: String,
    val name: String,
    val streamUrl: String,
    val posterUrl: String = "",
    val containerExtension: String = "mp4",
    val plot: String = "",
    val genre: String = "",
    val releaseYear: String = "",
    val rating: String = "",
    val duration: String = "",
    val addedTimestamp: Long = 0L,
    val isAdult: Boolean = false
)

@Entity(
    tableName = "series",
    indices = [Index("playlistId"), Index("categoryId"), Index("name")]
)
data class SeriesEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val categoryId: String,
    val categoryName: String,
    val remoteId: String,
    val name: String,
    val coverUrl: String = "",
    val plot: String = "",
    val genre: String = "",
    val releaseDate: String = "",
    val rating: String = "",
    val lastModified: Long = 0L,
    val isAdult: Boolean = false
)

@Entity(
    tableName = "episodes",
    indices = [Index("playlistId"), Index("seriesRemoteId"), Index("seasonNumber")]
)
data class EpisodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val seriesRemoteId: String,
    val remoteId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val streamUrl: String,
    val containerExtension: String = "mp4",
    val thumbnailUrl: String = "",
    val plot: String = "",
    val duration: String = ""
)

@Entity(
    tableName = "favorites",
    indices = [Index("playlistId"), Index("itemType")]
)
data class FavoriteEntity(
    @PrimaryKey val itemKey: String,
    val playlistId: Long,
    val itemType: ContentType,
    val itemId: Long,
    val remoteId: String = "",
    val title: String,
    val subtitle: String = "",
    val imageUrl: String = "",
    val streamUrl: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "watch_history",
    indices = [Index("playlistId"), Index("watchedAt")]
)
data class WatchHistoryEntity(
    @PrimaryKey val itemKey: String,
    val playlistId: Long,
    val itemType: ContentType,
    val itemId: Long,
    val remoteId: String = "",
    val title: String,
    val subtitle: String = "",
    val imageUrl: String = "",
    val streamUrl: String = "",
    val watchedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playback_progress",
    indices = [Index("playlistId"), Index("updatedAt")]
)
data class PlaybackProgressEntity(
    @PrimaryKey val itemKey: String,
    val playlistId: Long,
    val itemType: ContentType,
    val itemId: Long,
    val title: String,
    val subtitle: String = "",
    val imageUrl: String = "",
    val streamUrl: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "epg_programmes",
    indices = [Index("channelTvgId"), Index("startTimeMs"), Index("endTimeMs")]
)
data class EpgProgrammeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelTvgId: String,
    val title: String,
    val description: String = "",
    val category: String = "",
    val startTimeMs: Long,
    val endTimeMs: Long
)
