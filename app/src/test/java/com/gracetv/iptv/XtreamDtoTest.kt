package com.gracetv.iptv

import com.gracetv.iptv.data.remote.NetworkClientFactory
import com.gracetv.iptv.data.remote.XtreamAuthResponse
import com.gracetv.iptv.data.remote.XtreamLiveStreamDto
import com.gracetv.iptv.data.remote.XtreamSeriesInfoResponse
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class XtreamDtoTest {

    private val moshi = NetworkClientFactory.createMoshi()

    @Test
    fun `parses Xtream auth response with mixed string and integer types`() {
        val json = """
            {
              "user_info": {
                "username": "alice",
                "status": "Active",
                "auth": 1,
                "exp_date": 1893456000,
                "max_connections": 2,
                "active_cons": "0"
              },
              "server_info": {
                "url": "stream.example.com",
                "port": 8080,
                "server_protocol": "https"
              }
            }
        """.trimIndent()

        val parsed = moshi.adapter(XtreamAuthResponse::class.java).fromJson(json)
        assertNotNull(parsed)
        assertEquals("alice", parsed?.userInfo?.username)
        assertEquals("1", parsed?.userInfo?.auth)
        assertEquals("2", parsed?.userInfo?.maxConnections)
        assertEquals("8080", parsed?.serverInfo?.port)
    }

    @Test
    fun `parses live streams list and series episodes map`() {
        val liveJson = """
            [
              {"num": 1, "name": "Sports 1", "stream_id": 401, "stream_icon": "", "epg_channel_id": "sports.1", "category_id": 10, "is_adult": 0}
            ]
        """.trimIndent()
        val listType = Types.newParameterizedType(List::class.java, XtreamLiveStreamDto::class.java)
        val liveList = moshi.adapter<List<XtreamLiveStreamDto>>(listType).fromJson(liveJson)
        assertEquals(1, liveList?.size)
        assertEquals("401", liveList?.first()?.streamId)
        assertEquals("10", liveList?.first()?.categoryId)

        val seriesJson = """
            {
              "info": {"name": "Planet Series", "rating": 8.7},
              "episodes": {
                "1": [
                  {"id": 9001, "episode_num": 1, "title": "Pilot", "container_extension": "mkv", "season": 1}
                ]
              }
            }
        """.trimIndent()
        val seriesInfo = moshi.adapter(XtreamSeriesInfoResponse::class.java).fromJson(seriesJson)
        assertEquals("Planet Series", seriesInfo?.info?.name)
        assertEquals("8.7", seriesInfo?.info?.rating)
        val eps = seriesInfo?.episodes?.get("1")
        assertEquals(1, eps?.size)
        assertEquals("9001", eps?.first()?.id)
        assertEquals("mkv", eps?.first()?.containerExtension)
    }
}
