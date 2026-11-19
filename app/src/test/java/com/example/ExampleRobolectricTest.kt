package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PrivacyPreferences
import com.example.data.local.ScanRecordEntity
import com.example.domain.model.AssessmentVerdict
import com.example.domain.model.DeviceHealthSummary
import com.example.domain.model.NetworkMetadataSummary
import com.example.domain.model.ScanAssessmentResult
import com.example.domain.model.SensorAndPermissionSummary
import com.example.ui.screens.ConsentPermissionScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.WelcomeDisclosureScreen
import com.example.ui.theme.JagaPerangkatTheme
import com.example.ui.viewmodel.ScanExecutionState
import com.example.ui.viewmodel.SecurityUiState
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

    @Test
    fun `dashboard displays active permissions, enabled sensors, potential risks, and room history`() {
        val result = ScanAssessmentResult(
            scanId = "scan-1",
            timestampMillis = 1_700_000_000_000L,
            riskScore0to100 = 18,
            verdict = AssessmentVerdict.REVIEW_RECOMMENDED,
            confidencePercent = 84,
            coverageCheckedModules = 4,
            coverageTotalModules = 4,
            deviceHealth = DeviceHealthSummary(
                manufacturer = "Google",
                model = "Pixel",
                osVersion = "16",
                sdkInt = 36,
                securityPatch = "2026-10-05",
                isScreenLockSecure = true,
                isAdbEnabled = false,
                isDevOptionsEnabled = false,
                isTestKeysBuild = false,
                suBinaryPathsFound = emptyList(),
                isAdvancedProtectionEnabled = true,
                patchAgeStatus = "Mutakhir"
            ),
            networkMetadata = NetworkMetadataSummary(
                isConnected = true,
                transportLabel = "Wi-Fi",
                isVpnActive = false,
                hasHttpProxy = false,
                proxyDescription = null,
                isPrivateDnsActive = true,
                privateDnsServer = "dns.google",
                userCaCertCount = 0,
                userCaSubjects = emptyList()
            ),
            sensorAndPermissionSummary = SensorAndPermissionSummary(
                totalHardwareSensors = 12,
                hasCameraHardware = true,
                hasMicrophoneHardware = true,
                hasGpsHardware = true,
                supportsMicPrivacyToggle = true,
                supportsCameraPrivacyToggle = true,
                sampleSensorNames = listOf("Accelerometer", "Gyroscope"),
                appsWithMicPermission = 3,
                appsWithCameraPermission = 4,
                appsWithLocationPermission = 5,
                appsWithSmsOrCallPermission = 1,
                activeAccessibilityCount = 0,
                activeNotificationListenerCount = 1,
                activeDeviceAdminCount = 0
            ),
            analyzedApps = emptyList(),
            indicators = emptyList(),
            limitations = emptyList()
        )
        val historyRecord = ScanRecordEntity(
            scanId = "scan-1",
            timestampMillis = 1_700_000_000_000L,
            riskScore = 18,
            verdictName = AssessmentVerdict.REVIEW_RECOMMENDED.name,
            verdictShortBadge = AssessmentVerdict.REVIEW_RECOMMENDED.shortBadge,
            confidencePercent = 84,
            coverageChecked = 4,
            coverageTotal = 4,
            indicatorCount = 0,
            highSeverityCount = 0,
            activeMicAppsCount = 3,
            activeCameraAppsCount = 4,
            activeLocationAppsCount = 5,
            enabledSensorsCount = 12,
            topFindingTitle = "Tidak ada indikator risiko menonjol",
            shareableReportText = "Report"
        )

        composeTestRule.setContent {
            JagaPerangkatTheme {
                DashboardScreen(
                    uiState = SecurityUiState(
                        preferences = PrivacyPreferences(hasAcceptedDisclosure = true),
                        scanState = ScanExecutionState.Completed(result),
                        history = listOf(historyRecord)
                    ),
                    onRunQuickScan = {},
                    onOpenDetailedAssessment = {},
                    onOpenFindings = {},
                    onOpenNetworkAnalysis = {},
                    onOpenGuidedRecommendations = {},
                    onOpenHistory = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("card_active_permissions_summary").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_enabled_sensors_summary").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_potential_risks_summary").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_room_history_dashboard").performScrollTo().assertIsDisplayed()
    }
}
