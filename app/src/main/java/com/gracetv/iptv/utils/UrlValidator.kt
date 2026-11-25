package com.gracetv.iptv.utils

import java.net.URI

object UrlValidator {
    fun normalizeBaseUrl(raw: String): String? {
        val trimmed = raw.trim().trimEnd('/')
        if (trimmed.isEmpty()) return null
        val withScheme = if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "http://$trimmed"
        }
        return try {
            val uri = URI(withScheme)
            if (uri.host.isNullOrBlank()) null else withScheme
        } catch (_: Throwable) {
            null
        }
    }

    fun isValidStreamOrPlaylistUrl(raw: String): Boolean {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return false
        return try {
            val uri = URI(trimmed)
            val scheme = uri.scheme?.lowercase() ?: return false
            (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()
        } catch (_: Throwable) {
            false
        }
    }

    fun isInsecureHttp(url: String): Boolean =
        url.trim().startsWith("http://", ignoreCase = true)

    fun isAdultContent(text: String, keywords: Set<String>): Boolean {
        if (text.isBlank() || keywords.isEmpty()) return false
        val lower = text.lowercase()
        return keywords.any { kw -> kw.isNotBlank() && lower.contains(kw.lowercase()) }
    }
}
