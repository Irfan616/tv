package com.gracetv.iptv

import com.gracetv.iptv.data.remote.XmltvParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XmltvParserTest {

    private val parser = XmltvParser()

    @Test
    fun `parses XMLTV channels, programmes, and timezone timestamps`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
              <channel id="news.hd">
                <display-name>World News HD</display-name>
              </channel>
              <programme start="20261125120000 +0000" stop="20261125130000 +0000" channel="news.hd">
                <title>Global Noon Briefing</title>
                <desc>Live coverage of international stories.</desc>
                <category>News</category>
              </programme>
              <programme start="20261125150000 +0200" stop="20261125160000 +0200" channel="news.hd">
                <title>Afternoon Analysis</title>
              </programme>
            </tv>
        """.trimIndent()

        val result = parser.parse(xml)

        assertEquals("World News HD", result.channelNames["news.hd"])
        assertEquals(2, result.programmes.size)

        val first = result.programmes[0]
        assertEquals("news.hd", first.channelTvgId)
        assertEquals("Global Noon Briefing", first.title)
        assertEquals("Live coverage of international stories.", first.description)
        assertEquals("News", first.category)
        assertEquals(3600_000L, first.endTimeMs - first.startTimeMs)

        val second = result.programmes[1]
        // 15:00 +0200 == 13:00 +0000, so it starts exactly when the first ends
        assertEquals(first.endTimeMs, second.startTimeMs)
        assertTrue(second.endTimeMs > second.startTimeMs)
    }
}
