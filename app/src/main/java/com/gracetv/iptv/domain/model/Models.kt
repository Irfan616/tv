package com.gracetv.iptv.domain.model

enum class PlaylistType {
    XTREAM,
    M3U_URL,
    M3U_FILE
}

enum class ConnectionStatus {
    CONNECTED,
    SYNCING,
    ERROR,
    DISCONNECTED
}

enum class ContentType {
    LIVE,
    MOVIE,
    SERIES,
    EPISODE
}

enum class AspectRatioMode(val label: String) {
    FIT("Fit"),
    FILL("Fill"),
    ZOOM("Zoom"),
    RATIO_16_9("16:9"),
    RATIO_4_3("4:3")
}

enum class ThemeMode(val label: String) {
    DARK("Grace Dark"),
    AMOLED("AMOLED Black"),
    HIGH_CONTRAST("High Contrast")
}

data class ParsedM3uItem(
    val name: String,
    val streamUrl: String,
    val groupTitle: String,
    val tvgId: String,
    val tvgName: String,
    val tvgLogo: String,
    val isVod: Boolean,
    val containerExtension: String = ""
)

data class ParsedM3uResult(
    val items: List<ParsedM3uItem>,
    val epgUrl: String?,
    val skippedCount: Int
)

data class ParsedEpgProgramme(
    val channelTvgId: String,
    val title: String,
    val description: String,
    val category: String,
    val startTimeMs: Long,
    val endTimeMs: Long
)

data class ParsedEpgResult(
    val channelNames: Map<String, String>,
    val programmes: List<ParsedEpgProgramme>
)

data class ImportProgress(
    val stage: String,
    val progressFraction: Float,
    val itemsProcessed: Int = 0
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
