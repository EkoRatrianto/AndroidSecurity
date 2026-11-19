package com.example.data

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.PrivacyPreferences
import com.example.data.local.PrivacyPreferencesRepository
import com.example.data.local.ScanRecordEntity
import com.example.data.scanner.SecurityAssessmentCoordinator
import com.example.domain.model.IndicatorSeverity
import com.example.domain.model.ScanAssessmentResult
import kotlinx.coroutines.flow.Flow

class SecurityRepository(context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val scanRecordDao = database.scanRecordDao()
    private val prefsRepo = PrivacyPreferencesRepository(context)
    private val coordinator = SecurityAssessmentCoordinator(context)

    val privacyPreferences: Flow<PrivacyPreferences> = prefsRepo.preferencesFlow
    val scanHistory: Flow<List<ScanRecordEntity>> = scanRecordDao.observeAllRecords()

    suspend fun acceptDisclosure() = prefsRepo.acceptPrivacyDisclosure()

    suspend fun setIncludeAppScan(enabled: Boolean) = prefsRepo.setIncludeAppMetadataScan(enabled)

    suspend fun setIncludeNetworkScan(enabled: Boolean) = prefsRepo.setIncludeNetworkMetadataScan(enabled)

    suspend fun toggleGuidedCheck(checkId: String) = prefsRepo.toggleGuidedCheck(checkId)

    suspend fun runAndPersistAssessment(
        includeAppScan: Boolean,
        includeNetworkScan: Boolean,
        onStepProgress: (String, Float) -> Unit
    ): ScanAssessmentResult {
        val result = coordinator.runFullAssessment(
            includeAppScan = includeAppScan,
            includeNetworkScan = includeNetworkScan,
            onStepProgress = onStepProgress
        )

        val highCount = result.indicators.count {
            it.severity == IndicatorSeverity.HIGH || it.severity == IndicatorSeverity.CRITICAL
        }
        val topTitle = result.indicators.firstOrNull()?.title
            ?: "Tidak ada indikator risiko menonjol"

        val sp = result.sensorAndPermissionSummary
        val entity = ScanRecordEntity(
            scanId = result.scanId,
            timestampMillis = result.timestampMillis,
            riskScore = result.riskScore0to100,
            verdictName = result.verdict.name,
            verdictShortBadge = result.verdict.shortBadge,
            confidencePercent = result.confidencePercent,
            coverageChecked = result.coverageCheckedModules,
            coverageTotal = result.coverageTotalModules,
            indicatorCount = result.indicators.size,
            highSeverityCount = highCount,
            activeMicAppsCount = sp.appsWithMicPermission,
            activeCameraAppsCount = sp.appsWithCameraPermission,
            activeLocationAppsCount = sp.appsWithLocationPermission,
            enabledSensorsCount = sp.totalHardwareSensors,
            topFindingTitle = topTitle,
            shareableReportText = coordinator.formatShareableReport(result)
        )
        scanRecordDao.insertRecord(entity)
        return result
    }

    fun formatShareableReport(result: ScanAssessmentResult): String =
        coordinator.formatShareableReport(result)

    suspend fun deleteHistoryItem(scanId: String) = scanRecordDao.deleteRecordById(scanId)

    suspend fun clearAllLocalData() {
        scanRecordDao.clearAllRecords()
        prefsRepo.resetAllPreferences()
    }
}
