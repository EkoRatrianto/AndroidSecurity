package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SecurityRepository
import com.example.data.local.PrivacyPreferences
import com.example.data.local.ScanRecordEntity
import com.example.domain.model.IndicatorSeverity
import com.example.domain.model.ScanAssessmentResult
import com.example.domain.model.ThreatCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ScanExecutionState {
    data object Idle : ScanExecutionState
    data class Scanning(val stepLabel: String, val progress: Float) : ScanExecutionState
    data class Completed(val result: ScanAssessmentResult) : ScanExecutionState
    data class Error(val message: String) : ScanExecutionState
}

data class SecurityUiState(
    val preferences: PrivacyPreferences = PrivacyPreferences(),
    val scanState: ScanExecutionState = ScanExecutionState.Idle,
    val history: List<ScanRecordEntity> = emptyList(),
    val selectedSeverityFilter: IndicatorSeverity? = null,
    val selectedCategoryFilter: ThreatCategory? = null,
    val isConsentStepVisible: Boolean = false
)

class SecurityViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SecurityRepository(application.applicationContext)

    private val _scanState = MutableStateFlow<ScanExecutionState>(ScanExecutionState.Idle)
    private val _severityFilter = MutableStateFlow<IndicatorSeverity?>(null)
    private val _categoryFilter = MutableStateFlow<ThreatCategory?>(null)
    private val _isConsentStepVisible = MutableStateFlow(false)

    val uiState: StateFlow<SecurityUiState> = combine(
        repository.privacyPreferences,
        _scanState,
        repository.scanHistory,
        _severityFilter,
        _categoryFilter,
        _isConsentStepVisible
    ) { values ->
        val prefs = values[0] as PrivacyPreferences
        val scan = values[1] as ScanExecutionState
        @Suppress("UNCHECKED_CAST")
        val hist = values[2] as List<ScanRecordEntity>
        val sev = values[3] as IndicatorSeverity?
        val cat = values[4] as ThreatCategory?
        val consentVisible = values[5] as Boolean
        SecurityUiState(
            preferences = prefs,
            scanState = scan,
            history = hist,
            selectedSeverityFilter = sev,
            selectedCategoryFilter = cat,
            isConsentStepVisible = consentVisible
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SecurityUiState()
    )

    fun showConsentScreen() {
        _isConsentStepVisible.value = true
    }

    fun backToWelcome() {
        _isConsentStepVisible.value = false
    }

    fun acceptDisclosureAndStart() {
        viewModelScope.launch {
            repository.acceptDisclosure()
            _isConsentStepVisible.value = false
            runSecurityAudit()
        }
    }

    fun runSecurityAudit() {
        if (_scanState.value is ScanExecutionState.Scanning) return
        viewModelScope.launch {
            val prefs = uiState.value.preferences
            _scanState.value = ScanExecutionState.Scanning("Memulai pemeriksaan lokal...", 0.05f)
            runCatching {
                repository.runAndPersistAssessment(
                    includeAppScan = prefs.includeAppMetadataScan,
                    includeNetworkScan = prefs.includeNetworkMetadataScan
                ) { label, progress ->
                    _scanState.value = ScanExecutionState.Scanning(label, progress)
                }
            }.onSuccess { result ->
                _scanState.value = ScanExecutionState.Completed(result)
            }.onFailure { throwable ->
                _scanState.value = ScanExecutionState.Error(
                    throwable.message ?: "Terjadi kendala saat membaca status konfigurasi perangkat."
                )
            }
        }
    }

    fun setAppScanEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setIncludeAppScan(enabled) }
    }

    fun setNetworkScanEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setIncludeNetworkScan(enabled) }
    }

    fun toggleGuidedCheck(checkId: String) {
        viewModelScope.launch { repository.toggleGuidedCheck(checkId) }
    }

    fun setSeverityFilter(severity: IndicatorSeverity?) {
        _severityFilter.value = severity
    }

    fun setCategoryFilter(category: ThreatCategory?) {
        _categoryFilter.value = category
    }

    fun deleteHistoryRecord(scanId: String) {
        viewModelScope.launch { repository.deleteHistoryItem(scanId) }
    }

    fun clearAllLocalData() {
        viewModelScope.launch {
            repository.clearAllLocalData()
            _scanState.value = ScanExecutionState.Idle
            _isConsentStepVisible.value = false
        }
    }

    fun buildShareableReport(result: ScanAssessmentResult): String =
        repository.formatShareableReport(result)
}
