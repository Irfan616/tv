package com.gracetv.iptv

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gracetv.iptv.data.local.GraceTvDatabase
import com.gracetv.iptv.data.local.PreferencesManager
import com.gracetv.iptv.data.local.entity.CategoryEntity
import com.gracetv.iptv.data.local.entity.ChannelEntity
import com.gracetv.iptv.data.local.entity.MovieEntity
import com.gracetv.iptv.data.local.entity.PlaylistEntity
import com.gracetv.iptv.data.remote.M3uParser
import com.gracetv.iptv.data.remote.NetworkClientFactory
import com.gracetv.iptv.data.remote.XmltvParser
import com.gracetv.iptv.data.repository.ContentRepository
import com.gracetv.iptv.data.repository.EpgRepository
import com.gracetv.iptv.data.repository.PlaylistRepository
import com.gracetv.iptv.domain.model.ConnectionStatus
import com.gracetv.iptv.domain.model.ContentType
import com.gracetv.iptv.domain.model.PlaylistType
import com.gracetv.iptv.security.CredentialCipher
import com.gracetv.iptv.security.PinHasher
import com.gracetv.iptv.ui.screens.favorites.FavoritesViewModel
import com.gracetv.iptv.ui.screens.search.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RepositoryAndViewModelRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: GraceTvDatabase
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var contentRepository: ContentRepository

    private val fakeCipher = object : CredentialCipher {
        override fun encrypt(plainText: String): String = "enc:$plainText"
        override fun decrypt(cipherText: String): String = cipherText.removePrefix("enc:")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(Dispatchers.Default)
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, GraceTvDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferencesManager = PreferencesManager(context)
        contentRepository = ContentRepository(
            database = database,
            apiService = NetworkClientFactory.createXtreamService(),
            cipher = fakeCipher
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `playlist import, favorites persistence, and local search work end-to-end`() = runBlocking(Dispatchers.Default) {
        val playlistId = database.playlistDao().insert(
            PlaylistEntity(
                name = "Test Playlist",
                type = PlaylistType.M3U_URL,
                m3uUrl = "https://example.com/list.m3u",
                isDefault = true,
                status = ConnectionStatus.CONNECTED,
                channelCount = 2,
                movieCount = 1
            )
        )

        database.categoryDao().insertAll(
            listOf(
                CategoryEntity(playlistId = playlistId, remoteId = "News", name = "News", contentType = ContentType.LIVE),
                CategoryEntity(playlistId = playlistId, remoteId = "Action", name = "Action", contentType = ContentType.MOVIE)
            )
        )

        database.channelDao().insertAll(
            listOf(
                ChannelEntity(
                    playlistId = playlistId,
                    categoryId = "News",
                    categoryName = "News",
                    remoteId = "1",
                    name = "Grace News Network",
                    streamUrl = "https://example.com/news.m3u8",
                    tvgId = "grace.news"
                ),
                ChannelEntity(
                    playlistId = playlistId,
                    categoryId = "Sports",
                    categoryName = "Sports",
                    remoteId = "2",
                    name = "Stadium Live HD",
                    streamUrl = "https://example.com/sports.m3u8"
                )
            )
        )

        database.movieDao().insertAll(
            listOf(
                MovieEntity(
                    playlistId = playlistId,
                    categoryId = "Action",
                    categoryName = "Action",
                    remoteId = "100",
                    name = "Grace Horizon",
                    streamUrl = "https://example.com/horizon.mp4"
                )
            )
        )

        // Search
        val searchResult = contentRepository.searchLocal(playlistId, "grace")
        assertEquals(1, searchResult.channels.size)
        assertEquals(1, searchResult.movies.size)
        assertEquals("Grace News Network", searchResult.channels.first().name)

        // Favorites persistence + ViewModel filtering
        val channel = searchResult.channels.first()
        val added = contentRepository.toggleFavorite(
            playlistId = playlistId,
            type = ContentType.LIVE,
            itemId = channel.id,
            remoteId = channel.remoteId,
            title = channel.name,
            subtitle = channel.categoryName,
            imageUrl = channel.logoUrl,
            streamUrl = channel.streamUrl
        )
        assertTrue(added)

        val favs = contentRepository.observeFavorites().first()
        assertEquals(1, favs.size)

        val favVm = FavoritesViewModel(contentRepository)
        favVm.setFilter(ContentType.MOVIE)
        val favUiState = withTimeout(5000L) {
            favVm.uiState.first { it.selectedFilter == ContentType.MOVIE && it.totalCount == 1 }
        }
        assertEquals(ContentType.MOVIE, favUiState.selectedFilter)
        assertEquals(1, favUiState.totalCount)

        val playlistRepo = PlaylistRepository(
            context = context,
            database = database,
            apiService = NetworkClientFactory.createXtreamService(),
            m3uParser = M3uParser(),
            cipher = fakeCipher,
            preferencesManager = preferencesManager
        )
        val searchVm = SearchViewModel(playlistRepo, contentRepository, preferencesManager)
        searchVm.updateQuery("stadium")
        val searchUiState = withTimeout(5000L) {
            searchVm.uiState.first {
                it.query == "stadium" && it.results.channels.any { ch -> ch.name == "Stadium Live HD" }
            }
        }
        assertEquals("stadium", searchUiState.query)
        assertEquals(1, searchUiState.results.channels.size)
    }

    @Test
    fun `epg repository replaces programmes and pin hasher verifies salted pins`() = runBlocking(Dispatchers.Default) {
        val fakeApi = object : com.gracetv.iptv.data.remote.XtreamApiService by NetworkClientFactory.createXtreamService() {
            override suspend fun downloadRaw(url: String) =
                """
                <tv>
                  <programme start="20261125100000 +0000" stop="20261125120000 +0000" channel="grace.news">
                    <title>Morning Bulletin</title>
                  </programme>
                </tv>
                """.trimIndent().toResponseBody("application/xml".toMediaType())
        }

        val epgRepo = EpgRepository(database, fakeApi, XmltvParser(), preferencesManager)
        val result = epgRepo.refreshEpg("https://example.com/epg.xml")
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
        assertEquals(1, epgRepo.observeProgrammeCount().first())

        val salt = PinHasher.generateSalt()
        val hash = PinHasher.hashPin("4829", salt)
        assertTrue(PinHasher.verifyPin("4829", salt, hash))
        assertFalse(PinHasher.verifyPin("0000", salt, hash))
    }
}
