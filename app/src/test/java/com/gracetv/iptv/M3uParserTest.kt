package com.gracetv.iptv

import com.gracetv.iptv.data.remote.M3uParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {

    private val parser = M3uParser()

    @Test
    fun `parses valid M3U8 with EPG header, attributes, VOD detection, and skips malformed URLs`() {
        val sample = """
            #EXTM3U url-tvg="https://epg.example.com/guide.xml"
            #EXTINF:-1 tvg-id="bbc.one" tvg-name="BBC One" tvg-logo="https://img.example.com/bbc1.png" group-title="News",BBC One HD
            https://streams.example.com/live/bbc1.m3u8
            #EXTINF:-1 tvg-id="cinema.1" group-title="Movies" type="movie",Sample Film
            https://streams.example.com/movie/sample_film.mp4
            #EXTINF:-1 group-title="Broken",Malformed Stream
            not_a_valid_url_line
            #EXTINF:-1,Fallback Name Channel
            http://streams.example.com/live/channel2.ts
        """.trimIndent()

        val result = parser.parse(sample)

        assertEquals("https://epg.example.com/guide.xml", result.epgUrl)
        assertEquals(3, result.items.size)
        assertEquals(1, result.skippedCount)

        val first = result.items[0]
        assertEquals("BBC One HD", first.name)
        assertEquals("News", first.groupTitle)
        assertEquals("bbc.one", first.tvgId)
        assertEquals("BBC One", first.tvgName)
        assertEquals("https://img.example.com/bbc1.png", first.tvgLogo)
        assertFalse(first.isVod)

        val second = result.items[1]
        assertEquals("Sample Film", second.name)
        assertTrue(second.isVod)
        assertEquals("mp4", second.containerExtension)

        val third = result.items[2]
        assertEquals("Fallback Name Channel", third.name)
        assertEquals("Uncategorized", third.groupTitle)
    }

    @Test
    fun `parses large playlist efficiently`() {
        val sb = StringBuilder("#EXTM3U\n")
        repeat(5000) { idx ->
            sb.append("#EXTINF:-1 tvg-id=\"ch$idx\" group-title=\"Group ${idx % 10}\",Channel $idx\n")
            sb.append("https://streams.example.com/live/$idx.m3u8\n")
        }
        val result = parser.parse(sb.toString())
        assertEquals(5000, result.items.size)
    }
}
