package com.gracetv.iptv.data.remote

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import com.squareup.moshi.ToJson

/**
 * Xtream Codes servers frequently vary field types (e.g., numbers vs strings, null vs empty string).
 * ResilientString handles string, number, boolean, or null cleanly.
 */
@Retention(AnnotationRetention.RUNTIME)
annotation class ResilientString

class ResilientStringAdapter {
    @FromJson
    @ResilientString
    fun fromJson(reader: JsonReader): String {
        return when (reader.peek()) {
            JsonReader.Token.STRING -> reader.nextString()
            JsonReader.Token.NUMBER -> reader.nextString()
            JsonReader.Token.BOOLEAN -> reader.nextBoolean().toString()
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                ""
            }
            else -> {
                reader.skipValue()
                ""
            }
        }
    }

    @ToJson
    fun toJson(@ResilientString value: String): String = value
}

@JsonClass(generateAdapter = true)
data class XtreamAuthResponse(
    @Json(name = "user_info") val userInfo: XtreamUserInfo? = null,
    @Json(name = "server_info") val serverInfo: XtreamServerInfo? = null
)

@JsonClass(generateAdapter = true)
data class XtreamUserInfo(
    @ResilientString val username: String = "",
    @ResilientString val status: String = "",
    @ResilientString val auth: String = "",
    @Json(name = "exp_date") @ResilientString val expDate: String = "",
    @Json(name = "max_connections") @ResilientString val maxConnections: String = "",
    @Json(name = "active_cons") @ResilientString val activeCons: String = "",
    @ResilientString val message: String = ""
)

@JsonClass(generateAdapter = true)
data class XtreamServerInfo(
    @ResilientString val url: String = "",
    @ResilientString val port: String = "",
    @Json(name = "https_port") @ResilientString val httpsPort: String = "",
    @Json(name = "server_protocol") @ResilientString val serverProtocol: String = "",
    @ResilientString val timezone: String = ""
)

@JsonClass(generateAdapter = true)
data class XtreamCategoryDto(
    @Json(name = "category_id") @ResilientString val categoryId: String = "",
    @Json(name = "category_name") @ResilientString val categoryName: String = ""
)

@JsonClass(generateAdapter = true)
data class XtreamLiveStreamDto(
    @Json(name = "stream_id") @ResilientString val streamId: String = "",
    @ResilientString val name: String = "",
    @Json(name = "stream_icon") @ResilientString val streamIcon: String = "",
    @Json(name = "epg_channel_id") @ResilientString val epgChannelId: String = "",
    @Json(name = "category_id") @ResilientString val categoryId: String = "",
    @Json(name = "is_adult") @ResilientString val isAdult: String = "0",
    @Json(name = "num") @ResilientString val num: String = "0"
)

@JsonClass(generateAdapter = true)
data class XtreamVodStreamDto(
    @Json(name = "stream_id") @ResilientString val streamId: String = "",
    @ResilientString val name: String = "",
    @Json(name = "stream_icon") @ResilientString val streamIcon: String = "",
    @ResilientString val rating: String = "",
    @Json(name = "category_id") @ResilientString val categoryId: String = "",
    @Json(name = "container_extension") @ResilientString val containerExtension: String = "mp4",
    @ResilientString val added: String = "0",
    @Json(name = "is_adult") @ResilientString val isAdult: String = "0"
)

@JsonClass(generateAdapter = true)
data class XtreamVodInfoResponse(
    val info: XtreamVodDetailInfo? = null,
    @Json(name = "movie_data") val movieData: XtreamVodMovieData? = null
)

@JsonClass(generateAdapter = true)
data class XtreamVodDetailInfo(
    @ResilientString val plot: String = "",
    @ResilientString val genre: String = "",
    @ResilientString val releaseDate: String = "",
    @Json(name = "releasedate") @ResilientString val releaseDateLower: String = "",
    @ResilientString val duration: String = "",
    @ResilientString val rating: String = "",
    @Json(name = "movie_image") @ResilientString val movieImage: String = ""
)

@JsonClass(generateAdapter = true)
data class XtreamVodMovieData(
    @Json(name = "stream_id") @ResilientString val streamId: String = "",
    @ResilientString val name: String = "",
    @Json(name = "container_extension") @ResilientString val containerExtension: String = "mp4"
)

@JsonClass(generateAdapter = true)
data class XtreamSeriesDto(
    @Json(name = "series_id") @ResilientString val seriesId: String = "",
    @ResilientString val name: String = "",
    @ResilientString val cover: String = "",
    @ResilientString val plot: String = "",
    @ResilientString val genre: String = "",
    @Json(name = "releaseDate") @ResilientString val releaseDate: String = "",
    @ResilientString val rating: String = "",
    @Json(name = "last_modified") @ResilientString val lastModified: String = "0",
    @Json(name = "category_id") @ResilientString val categoryId: String = ""
)

@JsonClass(generateAdapter = true)
data class XtreamSeriesInfoResponse(
    val info: XtreamSeriesDetailInfo? = null,
    val episodes: Map<String, List<XtreamEpisodeDto>>? = null
)

@JsonClass(generateAdapter = true)
data class XtreamSeriesDetailInfo(
    @ResilientString val name: String = "",
    @ResilientString val cover: String = "",
    @ResilientString val plot: String = "",
    @ResilientString val genre: String = "",
    @Json(name = "releaseDate") @ResilientString val releaseDate: String = "",
    @ResilientString val rating: String = ""
)

@JsonClass(generateAdapter = true)
data class XtreamEpisodeDto(
    @ResilientString val id: String = "",
    @Json(name = "episode_num") @ResilientString val episodeNum: String = "1",
    @ResilientString val title: String = "",
    @Json(name = "container_extension") @ResilientString val containerExtension: String = "mp4",
    val season: Int = 1,
    val info: XtreamEpisodeInfo? = null
)

@JsonClass(generateAdapter = true)
data class XtreamEpisodeInfo(
    @Json(name = "movie_image") @ResilientString val movieImage: String = "",
    @ResilientString val plot: String = "",
    @ResilientString val duration: String = ""
)
