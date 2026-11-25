package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gracetv.iptv.GraceTvApplication
import com.gracetv.iptv.ui.navigation.GraceTvApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as GraceTvApplication).container
        setContent {
            GraceTvApp(container = container)
        }
    }
}
