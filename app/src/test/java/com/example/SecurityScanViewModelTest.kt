package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppAuditSnapshot
import com.example.data.DangerousPermissionReport
import com.example.data.DeviceSensorStatus
import com.example.data.SecurityScanDataSource
import com.example.data.SecurityScanRepository
import com.example.data.local.PrivacyPreferences
import com.example.data.local.ScanRecordEntity
import com.example.domain.model.AppRiskSummary
import com.example.domain.model.AssessmentVerdict
import com.example.domain.model.DeviceHealthSummary
import com.example.domain.model.NetworkMetadataSummary
import com.example.domain.model.ScanAssessmentResult
import com.example.domain.model.SensorAndPermissionSummary
import com.example.ui.viewmodel.SecurityScanViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SecurityScanViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `repository audits visible apps, identifies dangerous permissions, and checks sensors`() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repository = SecurityScanRepository(app)

        val apps = listOf(
            AppRiskSummary(
                packageName = "com.example.cameratool",
                appLabel = "Camera Tool",
                installerSourceLabel = "Sideload",
                isSideloaded = true,
                isSystemApp = false,
                isDebuggable = false,
                isTestOnly = false,
                hasActiveAccessibility = true,
                hasActiveNotificationAccess = false,
                isActiveDeviceAdmin = false,
                dangerousPermissions = listOf("Kamera", "Mikrofon"),
                riskFlags = listOf("Sideload", "Aksesibilitas Aktif"),
                localRiskPoints = 45
            ),
            AppRiskSummary(
                packageName = "com.android.systemui",
                appLabel = "System UI",
                installerSourceLabel = "Bawaan Sistem",
                isSideloaded = false,
                isSystemApp = true,
                isDebuggable = false,
                isTestOnly = false,
                hasActiveAccessibility = false,
                hasActiveNotificationAccess = false,
                isActiveDeviceAdmin = false,
                dangerousPermissions = listOf("Kamera"),
                riskFlags = emptyList(),
                localRiskPoints = 0
            )
        )

        val permReport = repository.identifyDangerousPermissions(apps)
        assertEquals(1, permReport.totalFlaggedApps)
        assertEquals(1, permReport.cameraPermissionAppsCount)
        assertEquals(1, permReport.micPermissionAppsCount)

        val sensorStatus = repository.checkDeviceSensorStatus()
        assertTrue(sensorStatus.totalHardwareSensors >= 0)

        val auditSnapshot = repository.auditVisibleApplications()
        assertTrue(auditSnapshot.totalVisibleApps >= 0)
    }

    @Test
    fun `viewModel executes complete scan, filters dangerous permissions, and updates Room history`() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val fakeRepo = FakeSecurityScanDataSource()
        val viewModel = SecurityScanViewModel(app, fakeRepo)

        val collectJob = launch { viewModel.uiState.collect() }

        viewModel.runCompleteSecurityScan()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.latestAssessment)
        assertNotNull(state.appAuditSnapshot)
        assertNotNull(state.dangerousPermissionReport)
        assertNotNull(state.deviceSensorStatus)
        assertEquals(1, state.scanHistory.size)
        assertEquals(1, state.filteredPermissionFindings.size)

        viewModel.setPermissionFilter("Mikrofon")
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.filteredPermissionFindings.size)

        viewModel.setPermissionFilter("SMS")
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.filteredPermissionFindings.size)

        collectJob.cancel()
    }

    private class FakeSecurityScanDataSource : SecurityScanDataSource {
        private val historyFlow = MutableStateFlow<List<ScanRecordEntity>>(emptyList())
        override val scanHistory: Flow<List<ScanRecordEntity>> = historyFlow
        override val privacyPreferences: Flow<PrivacyPreferences> = MutableStateFlow(PrivacyPreferences())

        private val sampleApp = AppRiskSummary(
            packageName = "com.synthetic.spy",
            appLabel = "Synthetic Tracker",
            installerSourceLabel = "Sideload",
            isSideloaded = true,
            isSystemApp = false,
            isDebuggable = false,
            isTestOnly = false,
            hasActiveAccessibility = true,
            hasActiveNotificationAccess = false,
            isActiveDeviceAdmin = false,
            dangerousPermissions = listOf("Mikrofon", "Kamera"),
            riskFlags = listOf("Sideload", "Aksesibilitas Aktif"),
            localRiskPoints = 55
        )

        override suspend fun auditVisibleApplications(): AppAuditSnapshot = AppAuditSnapshot(
            totalVisibleApps = 1,
            nonSystemAppsCount = 1,
            sideloadedAppsCount = 1,
            debuggableAppsCount = 0,
            privilegedServicesCount = 1,
            auditedApps = listOf(sampleApp),
            indicators = emptyList()
        )

        override suspend fun identifyDangerousPermissions(apps: List<AppRiskSummary>): DangerousPermissionReport {
            return SecurityScanRepository(ApplicationProvider.getApplicationContext()).identifyDangerousPermissions(apps)
        }

        override suspend fun checkDeviceSensorStatus(): DeviceSensorStatus = DeviceSensorStatus(
            totalHardwareSensors = 10,
            hasCameraHardware = true,
            hasMicrophoneHardware = true,
            hasGpsHardware = true,
            supportsMicPrivacyToggle = true,
            supportsCameraPrivacyToggle = true,
            activeSensorNames = listOf("Accelerometer", "Gyroscope")
        )

        override suspend fun runAndPersistFullScan(
            includeAppScan: Boolean,
            includeNetworkScan: Boolean,
            onStepProgress: (String, Float) -> Unit
        ): ScanAssessmentResult {
            onStepProgress("Scanning...", 0.8f)
            val result = ScanAssessmentResult(
                scanId = "scan-test-1",
                timestampMillis = 1_700_000_000_000L,
                riskScore0to100 = 42,
                verdict = AssessmentVerdict.FURTHER_INVESTIGATION_NEEDED,
                confidencePercent = 85,
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
                    totalHardwareSensors = 10,
                    hasCameraHardware = true,
                    hasMicrophoneHardware = true,
                    hasGpsHardware = true,
                    supportsMicPrivacyToggle = true,
                    supportsCameraPrivacyToggle = true,
                    sampleSensorNames = listOf("Accelerometer", "Gyroscope"),
                    appsWithMicPermission = 1,
                    appsWithCameraPermission = 1,
                    appsWithLocationPermission = 0,
                    appsWithSmsOrCallPermission = 0,
                    activeAccessibilityCount = 1,
                    activeNotificationListenerCount = 0,
                    activeDeviceAdminCount = 0
                ),
                analyzedApps = listOf(sampleApp),
                indicators = emptyList(),
                limitations = emptyList()
            )
            historyFlow.value = listOf(
                ScanRecordEntity(
                    scanId = result.scanId,
                    timestampMillis = result.timestampMillis,
                    riskScore = result.riskScore0to100,
                    verdictName = result.verdict.name,
                    verdictShortBadge = result.verdict.shortBadge,
                    confidencePercent = result.confidencePercent,
                    coverageChecked = 4,
                    coverageTotal = 4,
                    indicatorCount = 0,
                    highSeverityCount = 0,
                    activeMicAppsCount = 1,
                    activeCameraAppsCount = 1,
                    activeLocationAppsCount = 0,
                    enabledSensorsCount = 10,
                    topFindingTitle = "Synthetic Tracker",
                    shareableReportText = "Report"
                )
            )
            return result
        }
    }
}
