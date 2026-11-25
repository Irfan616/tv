package com.gracetv.iptv.di

import android.content.Context
import com.gracetv.iptv.data.local.GraceTvDatabase
import com.gracetv.iptv.data.local.PreferencesManager
import com.gracetv.iptv.data.remote.M3uParser
import com.gracetv.iptv.data.remote.NetworkClientFactory
import com.gracetv.iptv.data.remote.XmltvParser
import com.gracetv.iptv.data.repository.ContentRepository
import com.gracetv.iptv.data.repository.EpgRepository
import com.gracetv.iptv.data.repository.PlaylistRepository
import com.gracetv.iptv.security.KeystoreCredentialCipher

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: GraceTvDatabase by lazy { GraceTvDatabase.getInstance(appContext) }
    val preferencesManager: PreferencesManager by lazy { PreferencesManager(appContext) }
    val credentialCipher by lazy { KeystoreCredentialCipher() }

    private val okHttpClient by lazy { NetworkClientFactory.createOkHttpClient() }
    private val moshi by lazy { NetworkClientFactory.createMoshi() }
    val xtreamApiService by lazy { NetworkClientFactory.createXtreamService(okHttpClient, moshi) }

    val m3uParser by lazy { M3uParser() }
    val xmltvParser by lazy { XmltvParser() }

    val playlistRepository: PlaylistRepository by lazy {
        PlaylistRepository(
            context = appContext,
            database = database,
            apiService = xtreamApiService,
            m3uParser = m3uParser,
            cipher = credentialCipher,
            preferencesManager = preferencesManager
        )
    }

    val contentRepository: ContentRepository by lazy {
        ContentRepository(
            database = database,
            apiService = xtreamApiService,
            cipher = credentialCipher
        )
    }

    val epgRepository: EpgRepository by lazy {
        EpgRepository(
            database = database,
            apiService = xtreamApiService,
            xmltvParser = xmltvParser,
            preferencesManager = preferencesManager
        )
    }
}
