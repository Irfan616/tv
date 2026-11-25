package com.gracetv.iptv.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gracetv.iptv.domain.model.AspectRatioMode
import com.gracetv.iptv.domain.model.ThemeMode
import com.gracetv.iptv.security.PinHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.graceTvDataStore: DataStore<Preferences> by preferencesDataStore(name = "grace_tv_prefs")

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val languageCode: String = "en",
    val autoStartLastChannel: Boolean = false,
    val lastChannelId: Long = -1L,
    val defaultAspectRatio: AspectRatioMode = AspectRatioMode.FIT,
    val autoLandscape: Boolean = true,
    val hardwareDecoding: Boolean = true,
    val preferredAudioLanguage: String = "en",
    val preferredSubtitleLanguage: String = "Off",
    val showPlaybackControlsOnStart: Boolean = true,
    val epgUrl: String = "",
    val epgLastSyncMillis: Long = 0L,
    val parentalEnabled: Boolean = false,
    val hasParentalPin: Boolean = false,
    val restrictedKeywords: Set<String> = setOf("adult", "xxx", "18+", "+18", "erotic")
)

class PreferencesManager(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language_code")
        val AUTO_START_LAST_CHANNEL = booleanPreferencesKey("auto_start_last_channel")
        val LAST_CHANNEL_ID = longPreferencesKey("last_channel_id")
        val ASPECT_RATIO = stringPreferencesKey("default_aspect_ratio")
        val AUTO_LANDSCAPE = booleanPreferencesKey("auto_landscape")
        val HARDWARE_DECODING = booleanPreferencesKey("hardware_decoding")
        val AUDIO_LANG = stringPreferencesKey("preferred_audio_lang")
        val SUBTITLE_LANG = stringPreferencesKey("preferred_subtitle_lang")
        val SHOW_CONTROLS = booleanPreferencesKey("show_controls_on_start")
        val EPG_URL = stringPreferencesKey("epg_url")
        val EPG_LAST_SYNC = longPreferencesKey("epg_last_sync")
        val PARENTAL_ENABLED = booleanPreferencesKey("parental_enabled")
        val PARENTAL_PIN_HASH = stringPreferencesKey("parental_pin_hash")
        val PARENTAL_PIN_SALT = stringPreferencesKey("parental_pin_salt")
        val RESTRICTED_KEYWORDS = stringSetPreferencesKey("restricted_keywords")
    }

    val preferences: Flow<UserPreferences> = context.graceTvDataStore.data.map { prefs ->
        UserPreferences(
            themeMode = prefs[Keys.THEME_MODE]?.let {
                runCatching { ThemeMode.valueOf(it) }.getOrNull()
            } ?: ThemeMode.DARK,
            languageCode = prefs[Keys.LANGUAGE] ?: "en",
            autoStartLastChannel = prefs[Keys.AUTO_START_LAST_CHANNEL] ?: false,
            lastChannelId = prefs[Keys.LAST_CHANNEL_ID] ?: -1L,
            defaultAspectRatio = prefs[Keys.ASPECT_RATIO]?.let {
                runCatching { AspectRatioMode.valueOf(it) }.getOrNull()
            } ?: AspectRatioMode.FIT,
            autoLandscape = prefs[Keys.AUTO_LANDSCAPE] ?: true,
            hardwareDecoding = prefs[Keys.HARDWARE_DECODING] ?: true,
            preferredAudioLanguage = prefs[Keys.AUDIO_LANG] ?: "en",
            preferredSubtitleLanguage = prefs[Keys.SUBTITLE_LANG] ?: "Off",
            showPlaybackControlsOnStart = prefs[Keys.SHOW_CONTROLS] ?: true,
            epgUrl = prefs[Keys.EPG_URL] ?: "",
            epgLastSyncMillis = prefs[Keys.EPG_LAST_SYNC] ?: 0L,
            parentalEnabled = prefs[Keys.PARENTAL_ENABLED] ?: false,
            hasParentalPin = !prefs[Keys.PARENTAL_PIN_HASH].isNullOrBlank(),
            restrictedKeywords = prefs[Keys.RESTRICTED_KEYWORDS]
                ?: setOf("adult", "xxx", "18+", "+18", "erotic")
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = update { it[Keys.THEME_MODE] = mode.name }
    suspend fun setLanguageCode(code: String) = update { it[Keys.LANGUAGE] = code }
    suspend fun setAutoStartLastChannel(enabled: Boolean) = update { it[Keys.AUTO_START_LAST_CHANNEL] = enabled }
    suspend fun setLastChannelId(channelId: Long) = update { it[Keys.LAST_CHANNEL_ID] = channelId }
    suspend fun setDefaultAspectRatio(mode: AspectRatioMode) = update { it[Keys.ASPECT_RATIO] = mode.name }
    suspend fun setAutoLandscape(enabled: Boolean) = update { it[Keys.AUTO_LANDSCAPE] = enabled }
    suspend fun setHardwareDecoding(enabled: Boolean) = update { it[Keys.HARDWARE_DECODING] = enabled }
    suspend fun setPreferredAudioLanguage(lang: String) = update { it[Keys.AUDIO_LANG] = lang }
    suspend fun setPreferredSubtitleLanguage(lang: String) = update { it[Keys.SUBTITLE_LANG] = lang }
    suspend fun setShowPlaybackControlsOnStart(show: Boolean) = update { it[Keys.SHOW_CONTROLS] = show }
    suspend fun setEpgUrl(url: String) = update { it[Keys.EPG_URL] = url.trim() }
    suspend fun setEpgLastSyncMillis(millis: Long) = update { it[Keys.EPG_LAST_SYNC] = millis }
    suspend fun setRestrictedKeywords(keywords: Set<String>) = update { it[Keys.RESTRICTED_KEYWORDS] = keywords }

    suspend fun setParentalPin(pin: String) {
        val salt = PinHasher.generateSalt()
        val hash = PinHasher.hashPin(pin, salt)
        update {
            it[Keys.PARENTAL_PIN_SALT] = salt
            it[Keys.PARENTAL_PIN_HASH] = hash
            it[Keys.PARENTAL_ENABLED] = true
        }
    }

    suspend fun setParentalEnabled(enabled: Boolean) = update { it[Keys.PARENTAL_ENABLED] = enabled }

    suspend fun verifyParentalPin(pin: String): Boolean {
        var matched = false
        context.graceTvDataStore.edit { prefs ->
            val salt = prefs[Keys.PARENTAL_PIN_SALT].orEmpty()
            val hash = prefs[Keys.PARENTAL_PIN_HASH].orEmpty()
            matched = PinHasher.verifyPin(pin, salt, hash)
        }
        return matched
    }

    private suspend inline fun update(
        crossinline block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit
    ) {
        context.graceTvDataStore.edit { block(it) }
    }
}
