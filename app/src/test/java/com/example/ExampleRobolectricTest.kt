package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PrivacyPreferences
import com.example.ui.screens.ConsentPermissionScreen
import com.example.ui.screens.WelcomeDisclosureScreen
import com.example.ui.theme.JagaPerangkatTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("JagaPerangkat", appName)
    }

    @Test
    fun `welcome disclosure screen navigates to consent action`() {
        var continued = false
        composeTestRule.setContent {
            JagaPerangkatTheme {
                WelcomeDisclosureScreen(onContinueToConsent = { continued = true })
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_welcome_continue").assertIsDisplayed().performClick()
        assertTrue(continued)
    }

    @Test
    fun `consent screen triggers accept callback`() {
        var accepted = false
        composeTestRule.setContent {
            JagaPerangkatTheme {
                ConsentPermissionScreen(
                    preferences = PrivacyPreferences(),
                    onToggleAppScan = {},
                    onToggleNetworkScan = {},
                    onBack = {},
                    onAcceptAndStart = { accepted = true }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_accept_consent").assertIsDisplayed().performClick()
        assertTrue(accepted)
    }
}
