package com.gracetv.iptv.data.remote

import com.gracetv.iptv.domain.model.ParsedM3uItem
import com.gracetv.iptv.domain.model.ParsedM3uResult
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader

class M3uParser {
    companion object {
        private val ATTR_REGEX = Regex("""([a-zA-Z0-9_-]+)="([^"]*)"""")
        private val VOD_EXTENSIONS = setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v")
    }

    fun parse(content: String, onProgress: ((Int) -> Unit)? = null): ParsedM3uResult {
        return BufferedReader(StringReader(content)).use { reader ->
            parseReader(reader, onProgress)
        }
    }

    fun parse(inputStream: InputStream, onProgress: ((Int) -> Unit)? = null): ParsedM3uResult {
        return BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8), 32 * 1024).use { reader ->
            parseReader(reader, onProgress)
        }
    }

    private fun parseReader(reader: BufferedReader, onProgress: ((Int) -> Unit)?): ParsedM3uResult {
        val items = ArrayList<ParsedM3uItem>(256)
        var headerEpgUrl: String? = null
        var skippedCount = 0

        var pendingName: String? = null
        var pendingGroup = "Uncategorized"
        var pendingTvgId = ""
        var pendingTvgName = ""
        var pendingLogo = ""
        var pendingIsVod = false

        var lineNumber = 0
        while (true) {
            val rawLine = reader.readLine() ?: break
            lineNumber++
            val line = rawLine.trim().removePrefix("\uFEFF")
            if (line.isEmpty()) continue

            if (line.startsWith("#EXTM3U", ignoreCase = true)) {
                val attrs = parseAttributes(line)
                headerEpgUrl = attrs["url-tvg"]
                    ?: attrs["x-tvg-url"]
                    ?: attrs["tvg-url"]
                continue
            }

            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                val attrs = parseAttributes(line)
                val commaIdx = findDisplayTitleComma(line)
                val displayName = if (commaIdx >= 0 && commaIdx + 1 < line.length) {
                    line.substring(commaIdx + 1).trim()
                } else {
                    ""
                }

                pendingTvgId = attrs["tvg-id"].orEmpty().trim()
                pendingTvgName = attrs["tvg-name"].orEmpty().trim()
                pendingLogo = attrs["tvg-logo"].orEmpty().trim()
                pendingGroup = attrs["group-title"].orEmpty().trim().ifEmpty { "Uncategorized" }
                val typeAttr = attrs["type"].orEmpty().lowercase()
                pendingIsVod = typeAttr == "movie" || typeAttr == "vod"
                pendingName = displayName.ifEmpty { pendingTvgName.ifEmpty { "Unnamed Channel" } }
                continue
            }

            if (line.startsWith("#EXTGRP:", ignoreCase = true)) {
                val grp = line.substringAfter(":").trim()
                if (grp.isNotEmpty()) pendingGroup = grp
                continue
            }

            if (line.startsWith("#")) {
                // Other M3U directives (#EXTVLCOPT, #KODIPROP, comments)
                continue
            }

            // Stream URL line
            if (isSupportedStreamUri(line)) {
                val cleanUrl = line.trim()
                val ext = extractExtension(cleanUrl)
                val isVod = pendingIsVod ||
                    cleanUrl.contains("/movie/", ignoreCase = true) ||
                    ext in VOD_EXTENSIONS

                val name = pendingName ?: deriveNameFromUrl(cleanUrl)
                items.add(
                    ParsedM3uItem(
                        name = name,
                        streamUrl = cleanUrl,
                        groupTitle = pendingGroup,
                        tvgId = pendingTvgId,
                        tvgName = pendingTvgName,
                        tvgLogo = pendingLogo,
                        isVod = isVod,
                        containerExtension = ext.ifEmpty { if (isVod) "mp4" else "" }
                    )
                )
                if (items.size % 250 == 0) {
                    onProgress?.invoke(items.size)
                }
            } else {
                skippedCount++
            }

            // Reset pending metadata after consuming URL line
            pendingName = null
            pendingGroup = "Uncategorized"
            pendingTvgId = ""
            pendingTvgName = ""
            pendingLogo = ""
            pendingIsVod = false
        }

        onProgress?.invoke(items.size)
        return ParsedM3uResult(
            items = items,
            epgUrl = headerEpgUrl?.takeIf { it.isNotBlank() },
            skippedCount = skippedCount
        )
    }

    private fun parseAttributes(line: String): Map<String, String> {
        val map = HashMap<String, String>()
        for (match in ATTR_REGEX.findAll(line)) {
            val key = match.groupValues[1].lowercase()
            val value = match.groupValues[2]
            map[key] = value
        }
        return map
    }

    private fun findDisplayTitleComma(extinfLine: String): Int {
        var inQuotes = false
        for (i in extinfLine.indices) {
            val c = extinfLine[i]
            if (c == '"') inQuotes = !inQuotes
            else if (c == ',' && !inQuotes) return i
        }
        return -1
    }

    private fun isSupportedStreamUri(url: String): Boolean {
        val lower = url.lowercase()
        return lower.startsWith("http://") ||
            lower.startsWith("https://") ||
            lower.startsWith("rtsp://") ||
            lower.startsWith("rtmp://") ||
            lower.startsWith("udp://")
    }

    private fun extractExtension(url: String): String {
        val path = url.substringBefore('?').substringBefore('#')
        val lastSegment = path.substringAfterLast('/', "")
        val ext = lastSegment.substringAfterLast('.', "").lowercase()
        return if (ext.length in 2..5 && ext.all { it.isLetterOrDigit() }) ext else ""
    }

    private fun deriveNameFromUrl(url: String): String {
        val path = url.substringBefore('?').substringAfterLast('/')
        return path.substringBeforeLast('.').ifBlank { "Stream Channel" }
    }
}
