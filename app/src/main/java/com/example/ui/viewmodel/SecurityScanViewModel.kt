package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppAuditSnapshot
import com.example.data.DangerousPermissionFinding
import com.example.data.DangerousPermissionReport
import com.example.data.DeviceSensorStatus
import com.example.data.SecurityScanDataSource
import com.example.data.SecurityScanRepository
import com.example.data.local.ScanRecordEntity
import com.example.domain.model.ScanAssessmentResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SecurityScanUiState(
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val scanStepMessage: String = "Siap melakukan pemindaian keamanan lokal",
    val appAuditSnapshot: AppAuditSnapshot? = null,
    val dangerousPermissionReport: DangerousPermissionReport? = null,
    val selectedPermissionFilter: String? = null,
    val filteredPermissionFindings: List<DangerousPermissionFinding> = emptyList(),
    val deviceSensorStatus: DeviceSensorStatus? = null,
    val latestAssessment: ScanAssessmentResult? = null,
    val scanHistory: List<ScanRecordEntity> = emptyList(),
    val errorMessage: String? = null
)

/**
 * ViewModel orchestrating application audits, dangerous permission identification,
 * device sensor checks, and Room-persisted scan history observation.
 */
class SecurityScanViewModel(
    application: Application,
    private val scanRepository: SecurityScanDataSource = SecurityScanRepository(application.applicationContext)
) : AndroidViewModel(application) {

    private data class InternalScanState(
        val isScanning: Boolean = false,
        val scanProgress: Float = 0f,
        val scanStepMessage: String = "Siap melakukan pemindaian keamanan lokal",
        val appAuditSnapshot: AppAuditSnapshot? = null,
        val dangerousPermissionReport: DangerousPermissionReport? = null,
        val selectedPermissionFilter: String? = null,
        val deviceSensorStatus: DeviceSensorStatus? = null,
        val latestAssessment: ScanAssessmentResult? = null,
        val errorMessage: String? = null
    )

    private val _internalState = MutableStateFlow(InternalScanState())

    val uiState: StateFlow<SecurityScanUiState> = combine(
        _internalState,
        scanRepository.scanHistory
    ) { internal, history ->
        val allFindings = internal.dangerousPermissionReport?.findings.orEmpty()
        val filter = internal.selectedPermissionFilter
        val filtered = if (filter.isNullOrBlank()) {
            allFindings
        } else {
            allFindings.filter { finding ->
                finding.grantedPermissions.any { it.contains(filter, ignoreCase = true) }
            }
        }

        SecurityScanUiState(
            isScanning = internal.isScanning,
            scanProgress = internal.scanProgress,
            scanStepMessage = internal.scanStepMessage,
            appAuditSnapshot = internal.appAuditSnapshot,
            dangerousPermissionReport = internal.dangerousPermissionReport,
            selectedPermissionFilter = filter,
            filteredPermissionFindings = filtered,
            deviceSensorStatus = internal.deviceSensorStatus,
            latestAssessment = internal.latestAssessment,
            scanHistory = history,
            errorMessage = internal.errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SecurityScanUiState()
    )

    fun auditApplicationsAndPermissions() {
        if (_internalState.value.isScanning) return
        viewModelScope.launch {
            _internalState.value = _internalState.value.copy(
                isScanning = true,
                scanProgress = 0.25f,
                scanStepMessage = "Mengaudit aplikasi terlihat & izin berbahaya...",
                errorMessage = null
            )
            runCatching {
                val appSnapshot = scanRepository.auditVisibleApplications()
                val permReport = scanRepository.identifyDangerousPermissions(appSnapshot.auditedApps)
                appSnapshot to permReport
            }.onSuccess { (appSnapshot, permReport) ->
                _internalState.value = _internalState.value.copy(
                    isScanning = false,
                    scanProgress = 1f,
                    scanStepMessage = "Audit aplikasi selesai (${appSnapshot.nonSystemAppsCount} app non-sistem)",
                    appAuditSnapshot = appSnapshot,
                    dangerousPermissionReport = permReport
                )
            }.onFailure { throwable ->
                _internalState.value = _internalState.value.copy(
                    isScanning = false,
                    errorMessage = throwable.message ?: "Gagal mengaudit aplikasi dan izin perangkat."
                )
            }
        }
    }

    fun refreshDeviceSensorStatus() {
        viewModelScope.launch {
            runCatching {
                scanRepository.checkDeviceSensorStatus()
            }.onSuccess { sensorStatus ->
                _internalState.value = _internalState.value.copy(
                    deviceSensorStatus = sensorStatus,
                    errorMessage = null
                )
            }.onFailure { throwable ->
                _internalState.value = _internalState.value.copy(
                    errorMessage = throwable.message ?: "Gagal membaca status sensor perangkat."
                )
            }
        }
    }

    fun runCompleteSecurityScan(
        includeAppScan: Boolean = true,
        includeNetworkScan: Boolean = true
    ) {
        if (_internalState.value.isScanning) return
        viewModelScope.launch {
            _internalState.value = _internalState.value.copy(
                isScanning = true,
                scanProgress = 0.05f,
                scanStepMessage = "Memulai pemindaian keamanan perangkat...",
                errorMessage = null
            )
            runCatching {
                val assessment = scanRepository.runAndPersistFullScan(
                    includeAppScan = includeAppScan,
                    includeNetworkScan = includeNetworkScan
                ) { stepLabel, progress ->
                    _internalState.value = _internalState.value.copy(
                        scanProgress = progress * 0.85f,
                        scanStepMessage = stepLabel
                    )
                }
                val permReport = scanRepository.identifyDangerousPermissions(assessment.analyzedApps)
                val sensorStatus = scanRepository.checkDeviceSensorStatus()
                val nonSystemApps = assessment.analyzedApps.filter { !it.isSystemApp }
                val appSnapshot = AppAuditSnapshot(
                    totalVisibleApps = assessment.analyzedApps.size,
                    nonSystemAppsCount = nonSystemApps.size,
                    sideloadedAppsCount = nonSystemApps.count { it.isSideloaded },
                    debuggableAppsCount = nonSystemApps.count { it.isDebuggable },
                    privilegedServicesCount = nonSystemApps.count {
                        it.hasActiveAccessibility || it.hasActiveNotificationAccess || it.isActiveDeviceAdmin
                    },
                    auditedApps = assessment.analyzedApps,
                    indicators = assessment.indicators
                )
                Triple(assessment, Pair(appSnapshot, permReport), sensorStatus)
            }.onSuccess { (assessment, appAndPerms, sensorStatus) ->
                _internalState.value = _internalState.value.copy(
                    isScanning = false,
                    scanProgress = 1f,
                    scanStepMessage = "Pemindaian selesai & disimpan ke Room",
                    latestAssessment = assessment,
                    appAuditSnapshot = appAndPerms.first,
                    dangerousPermissionReport = appAndPerms.second,
                    deviceSensorStatus = sensorStatus
                )
            }.onFailure { throwable ->
                _internalState.value = _internalState.value.copy(
                    isScanning = false,
                    errorMessage = throwable.message ?: "Terjadi kesalahan saat pemindaian keamanan."
                )
            }
        }
    }

    fun setPermissionFilter(permissionLabel: String?) {
        _internalState.value = _internalState.value.copy(selectedPermissionFilter = permissionLabel)
    }

    fun clearError() {
        _internalState.value = _internalState.value.copy(errorMessage = null)
    }

    class Factory(
        private val application: Application,
        private val scanRepository: SecurityScanDataSource
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SecurityScanViewModel::class.java)) {
                return SecurityScanViewModel(application, scanRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
