package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.SoniqViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SONIQ PRO", appName)
    }

    @Test
    fun `demo preview toggle explicitly distinguishes simulated mode from live hardware`() {
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

        // Default state must always be LIVE HARDWARE (never silently faking telemetry)
        composeTestRule.onNodeWithTag("badge_live_hardware").assertIsDisplayed()

        // Explicitly launch Demo Preview Sandbox
        composeTestRule.onNodeWithTag("launch_demo_preview_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("badge_demo_preview").assertIsDisplayed()

        // Exit Demo returns cleanly to Live Hardware mode
        composeTestRule.onNodeWithTag("exit_demo_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("badge_live_hardware").assertIsDisplayed()
    }
}
