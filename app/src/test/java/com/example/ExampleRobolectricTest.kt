package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.SoniqViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-420dpi")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SONIQ", appName)
    }

    @Test
    fun `home screen renders honest standby empty state without simulated hardware and supports 5-tab navigation`() {
        val app = ApplicationProvider.getApplicationContext<SoniqApplication>()
        val vm = SoniqViewModel(app.repository)

        composeTestRule.setContent {
            SoniqAppRoot(
                viewModel = vm,
                onStartServices = {},
                onStopServices = {}
            )
        }
        composeTestRule.waitForIdle()

        // Must never fake a connected device or simulated battery reading
        composeTestRule.onNodeWithTag("badge_live_telemetry").assertIsDisplayed()
        composeTestRule.onNodeWithTag("empty_device_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_grant_bt_button").assertIsDisplayed()

        // Verify all 5 bottom navigation tabs exist and route cleanly
        composeTestRule.onNodeWithTag("nav_tab_home").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_devices").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("scan_toggle_button").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_tab_studio").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Audio Studio & 3D").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_tab_theatre").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("theatre_mdns_scan_button").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_tab_settings").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Settings & Ecosystem").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("run_diagnostics_button"))
        composeTestRule.onNodeWithTag("run_diagnostics_button").assertIsDisplayed()
    }
}
