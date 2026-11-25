package com.gracetv.iptv

import android.app.Application
import com.gracetv.iptv.di.AppContainer

class GraceTvApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
