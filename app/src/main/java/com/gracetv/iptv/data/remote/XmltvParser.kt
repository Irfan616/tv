package com.gracetv.iptv.data.remote

import com.gracetv.iptv.domain.model.ParsedEpgProgramme
import com.gracetv.iptv.domain.model.ParsedEpgResult
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.xml.parsers.SAXParserFactory

class XmltvParser {

    fun parse(xmlContent: String): ParsedEpgResult {
        return parseInputSource(InputSource(StringReader(xmlContent)))
    }

    fun parse(inputStream: InputStream): ParsedEpgResult {
        return parseInputSource(InputSource(inputStream))
    }

    private fun parseInputSource(inputSource: InputSource): ParsedEpgResult {
        val channelNames = HashMap<String, String>()
        val programmes = ArrayList<ParsedEpgProgramme>(512)

        val factory = SAXParserFactory.newInstance().apply {
            isNamespaceAware = false
        }
        val saxParser = factory.newSAXParser()
        saxParser.parse(inputSource, object : DefaultHandler() {
            private var inChannel = false
            private var inProgramme = false
            private var currentChannelId: String? = null
            private var progChannel = ""
            private var progStartMs = 0L
            private var progStopMs = 0L
            private var progTitle = ""
            private var progDesc = ""
            private var progCategory = ""
            private val textBuffer = StringBuilder()

            override fun startElement(
                uri: String?,
                localName: String?,
                qName: String?,
                attributes: Attributes?
            ) {
                val tag = (localName?.ifEmpty { qName } ?: qName).orEmpty().lowercase()
                textBuffer.setLength(0)
                when (tag) {
                    "channel" -> {
                        inChannel = true
                        currentChannelId = attributes?.getValue("id")?.trim().orEmpty()
                    }
                    "programme" -> {
                        inProgramme = true
                        progChannel = attributes?.getValue("channel")?.trim().orEmpty()
                        progStartMs = parseXmltvTimestamp(attributes?.getValue("start"))
                        progStopMs = parseXmltvTimestamp(attributes?.getValue("stop"))
                        progTitle = ""
                        progDesc = ""
                        progCategory = ""
                    }
                }
            }

            override fun characters(ch: CharArray?, start: Int, length: Int) {
                if (ch != null && length > 0) {
                    textBuffer.append(ch, start, length)
                }
            }

            override fun endElement(uri: String?, localName: String?, qName: String?) {
                val tag = (localName?.ifEmpty { qName } ?: qName).orEmpty().lowercase()
                val text = textBuffer.toString().trim()
                when (tag) {
                    "display-name" -> {
                        val cid = currentChannelId
                        if (inChannel && !cid.isNullOrBlank() && text.isNotBlank() && !channelNames.containsKey(cid)) {
                            channelNames[cid] = text
                        }
                    }
                    "channel" -> {
                        inChannel = false
                        currentChannelId = null
                    }
                    "title" -> if (inProgramme && text.isNotBlank()) progTitle = text
                    "desc" -> if (inProgramme && text.isNotBlank()) progDesc = text
                    "category" -> if (inProgramme && progCategory.isEmpty() && text.isNotBlank()) progCategory = text
                    "programme" -> {
                        if (inProgramme && progChannel.isNotBlank() &&
                            progTitle.isNotBlank() && progStartMs > 0L && progStopMs > progStartMs
                        ) {
                            programmes.add(
                                ParsedEpgProgramme(
                                    channelTvgId = progChannel,
                                    title = progTitle,
                                    description = progDesc,
                                    category = progCategory,
                                    startTimeMs = progStartMs,
                                    endTimeMs = progStopMs
                                )
                            )
                        }
                        inProgramme = false
                    }
                }
            }
        })

        return ParsedEpgResult(channelNames = channelNames, programmes = programmes)
    }

    companion object {
        private val FORMAT_WITH_TZ = ThreadLocal.withInitial {
            SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)
        }
        private val FORMAT_NO_SPACE_TZ = ThreadLocal.withInitial {
            SimpleDateFormat("yyyyMMddHHmmssZ", Locale.US)
        }
        private val FORMAT_NO_TZ = ThreadLocal.withInitial {
            SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        }

        fun parseXmltvTimestamp(raw: String?): Long {
            val cleaned = raw?.trim().orEmpty()
            if (cleaned.isEmpty()) return 0L
            return try {
                FORMAT_WITH_TZ.get()?.parse(cleaned)?.time ?: 0L
            } catch (_: Throwable) {
                try {
                    FORMAT_NO_SPACE_TZ.get()?.parse(cleaned)?.time ?: 0L
                } catch (_: Throwable) {
                    try {
                        val digits = cleaned.take(14)
                        FORMAT_NO_TZ.get()?.parse(digits)?.time ?: 0L
                    } catch (_: Throwable) {
                        0L
                    }
                }
            }
        }
    }
}
