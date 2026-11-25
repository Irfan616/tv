package com.gracetv.iptv

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.gracetv.iptv.di.AppContainer
import com.gracetv.iptv.ui.navigation.GraceTvApp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NavigationRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `navigates across Home, Playlists, Favorites, Search, Settings, and Support screens`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AppContainer(context)

        composeRule.setContent {
            GraceTvApp(container = container)
        }

        // Wait past SplashScreen (1.5s)
        composeRule.mainClock.advanceTimeBy(2500)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Welcome to Grace TV").assertIsDisplayed()

        // Navigate to Playlists
        composeRule.onNodeWithTag("home_add_playlist_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("add_playlist_fab").assertIsDisplayed()

        // Navigate to Favorites
        composeRule.onNodeWithTag("bottom_nav_favorites").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("No Favorites Yet").assertIsDisplayed()

        // Navigate to Search
        composeRule.onNodeWithTag("topbar_search_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("global_search_input").assertIsDisplayed()

        // Navigate to Settings
        composeRule.onNodeWithTag("topbar_settings_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("General Settings").assertIsDisplayed()

        // Navigate to Contact & Support via Drawer
        composeRule.onNodeWithTag("open_drawer_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("drawer_nav_support").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Grace TV Contact & Support").assertIsDisplayed()
        composeRule.onNodeWithText("irfanjameel293@gmail.com").performScrollTo().assertIsDisplayed()
    }
}
