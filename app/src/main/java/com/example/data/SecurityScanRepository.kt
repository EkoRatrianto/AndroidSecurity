package com.example.data

import android.content.Context
import com.example.data.local.PrivacyPreferences
import com.example.data.local.ScanRecordEntity
import com.example.data.scanner.AppInventoryAnalyzer
import com.example.data.scanner.ConfigurationAssessor
import com.example.data.scanner.DeviceHealthCollector
import com.example.domain.model.AppRiskSummary
import com.example.domain.model.ScanAssessmentResult
import com.example.domain.model.SecurityIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class AppAuditSnapshot(
    val totalVisibleApps: Int,
    val nonSystemAppsCount: Int,
    val sideloadedAppsCount: Int,
    val debuggableAppsCount: Int,
    val privilegedServicesCount: Int,
    val auditedApps: List<AppRiskSummary>,
    val indicators: List<SecurityIndicator>
)

data class DangerousPermissionFinding(
    val packageName: String,
    val appLabel: String,
    val isSideloaded: Boolean,
    val grantedPermissions: List<String>,
    val hasPrivilegedBinding: Boolean,
    val riskPoints: Int
)

data class DangerousPermissionReport(
    val totalFlaggedApps: Int,
    val micPermissionAppsCount: Int,
    val cameraPermissionAppsCount: Int,
    val locationPermissionAppsCount: Int,
    val smsOrCallPermissionAppsCount: Int,
    val overlayPermissionAppsCount: Int,
    val findings: List<DangerousPermissionFinding>
)

data class DeviceSensorStatus(
    val totalHardwareSensors: Int,
    val hasCameraHardware: Boolean,
    val hasMicrophoneHardware: Boolean,
    val hasGpsHardware: Boolean,
    val supportsMicPrivacyToggle: Boolean?,
    val supportsCameraPrivacyToggle: Boolean?,
    val activeSensorNames: List<String>
)

interface SecurityScanDataSource {
    val scanHistory: Flow<List<ScanRecordEntity>>
    val privacyPreferences: Flow<PrivacyPreferences>

    suspend fun auditVisibleApplications(): AppAuditSnapshot
    suspend fun identifyDangerousPermissions(apps: List<AppRiskSummary>): DangerousPermissionReport
    suspend fun checkDeviceSensorStatus(): DeviceSensorStatus
    suspend fun runAndPersistFullScan(
        includeAppScan: Boolean,
        includeNetworkScan: Boolean,
        onStepProgress: (String, Float) -> Unit
    ): ScanAssessmentResult
}

/**
 * Repository handling installed/visible application auditing, dangerous permission analysis,
 * device sensor status inspection, and Room-backed scan history persistence.
 */
class SecurityScanRepository(
    context: Context,
    private val securityRepository: SecurityRepository = SecurityRepository(context.applicationContext),
    private val deviceHealthCollector: DeviceHealthCollector = DeviceHealthCollector(context.applicationContext),
    private val configurationAssessor: ConfigurationAssessor = ConfigurationAssessor(context.applicationContext),
    private val appInventoryAnalyzer: AppInventoryAnalyzer = AppInventoryAnalyzer(context.applicationContext)
) : SecurityScanDataSource {

    override val scanHistory: Flow<List<ScanRecordEntity>> = securityRepository.scanHistory
    override val privacyPreferences: Flow<PrivacyPreferences> = securityRepository.privacyPreferences

    override suspend fun auditVisibleApplications(): AppAuditSnapshot = withContext(Dispatchers.IO) {
        val configSignals = configurationAssessor.assess()
        val appOutput = appInventoryAnalyzer.analyze(
            configSignals = configSignals,
            includeAppScan = true
        )

        val nonSystemApps = appOutput.apps.filter { !it.isSystemApp }
        val sideloadedCount = nonSystemApps.count { it.isSideloaded }
        val debuggableCount = nonSystemApps.count { it.isDebuggable }
        val privilegedCount = nonSystemApps.count {
            it.hasActiveAccessibility || it.hasActiveNotificationAccess || it.isActiveDeviceAdmin
        }

        AppAuditSnapshot(
            totalVisibleApps = appOutput.apps.size,
            nonSystemAppsCount = nonSystemApps.size,
            sideloadedAppsCount = sideloadedCount,
            debuggableAppsCount = debuggableCount,
            privilegedServicesCount = privilegedCount,
            auditedApps = appOutput.apps,
            indicators = configSignals.indicators + appOutput.indicators
        )
    }

    override suspend fun identifyDangerousPermissions(
        apps: List<AppRiskSummary>
    ): DangerousPermissionReport = withContext(Dispatchers.Default) {
        val nonSystemApps = apps.filter { !it.isSystemApp }
        val findings = nonSystemApps
            .filter {
                it.dangerousPermissions.isNotEmpty() ||
                    it.hasActiveAccessibility ||
                    it.hasActiveNotificationAccess ||
                    it.isActiveDeviceAdmin
            }
            .map { app ->
                DangerousPermissionFinding(
                    packageName = app.packageName,
                    appLabel = app.appLabel,
                    isSideloaded = app.isSideloaded,
                    grantedPermissions = app.dangerousPermissions,
                    hasPrivilegedBinding = app.hasActiveAccessibility ||
                        app.hasActiveNotificationAccess ||
                        app.isActiveDeviceAdmin,
                    riskPoints = app.localRiskPoints
                )
            }
            .sortedByDescending { it.riskPoints }

        DangerousPermissionReport(
            totalFlaggedApps = findings.size,
            micPermissionAppsCount = nonSystemApps.count { "Mikrofon" in it.dangerousPermissions },
            cameraPermissionAppsCount = nonSystemApps.count { "Kamera" in it.dangerousPermissions },
            locationPermissionAppsCount = nonSystemApps.count {
                "Lokasi Presisi" in it.dangerousPermissions ||
                    "Lokasi Latar Belakang" in it.dangerousPermissions
            },
            smsOrCallPermissionAppsCount = nonSystemApps.count {
                "Baca SMS" in it.dangerousPermissions ||
                    "Terima SMS" in it.dangerousPermissions ||
                    "Riwayat Panggilan" in it.dangerousPermissions
            },
            overlayPermissionAppsCount = nonSystemApps.count {
                "Hamparan Layar (Overlay)" in it.dangerousPermissions
            },
            findings = findings
        )
    }

    override suspend fun checkDeviceSensorStatus(): DeviceSensorStatus = withContext(Dispatchers.IO) {
        val sensorInfo = deviceHealthCollector.collectSensorStatus()
        DeviceSensorStatus(
            totalHardwareSensors = sensorInfo.totalHardwareSensors,
            hasCameraHardware = sensorInfo.hasCameraHardware,
            hasMicrophoneHardware = sensorInfo.hasMicrophoneHardware,
            hasGpsHardware = sensorInfo.hasGpsHardware,
            supportsMicPrivacyToggle = sensorInfo.supportsMicPrivacyToggle,
            supportsCameraPrivacyToggle = sensorInfo.supportsCameraPrivacyToggle,
            activeSensorNames = sensorInfo.sampleSensorNames
        )
    }

    override suspend fun runAndPersistFullScan(
        includeAppScan: Boolean,
        includeNetworkScan: Boolean,
        onStepProgress: (String, Float) -> Unit
    ): ScanAssessmentResult = securityRepository.runAndPersistAssessment(
        includeAppScan = includeAppScan,
        includeNetworkScan = includeNetworkScan,
        onStepProgress = onStepProgress
    )
}
