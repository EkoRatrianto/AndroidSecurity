package com.example.domain.engine

import com.example.domain.model.AppRiskSummary
import com.example.domain.model.AssessmentVerdict
import com.example.domain.model.EvidenceStrength
import com.example.domain.model.IndicatorSeverity
import com.example.domain.model.SecurityIndicator
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * Mesin Penilaian Risiko Transparan (Berbasis Aturan Terjelaskan, Tanpa Klaim AI Palsu).
 *
 * Formula (bobot awal bersifat expert judgment — ditandai belum terkalibrasi di laboratorium):
 * 1. Kontribusi per indikator:
 *    w_i = Severity.weight * EvidenceStrength.factor * DetectionReliability.factor
 * 2. Bonus korelasi (C_corr):
 *    +4.0 jika terdapat aplikasi non-sistem dengan kombinasi (Accessibility + Notification/Admin)
 *    +3.5 jika terdapat aplikasi sideload yang aktif memakai Accessibility atau Device Admin
 *    +3.0 jika Sertifikat CA Pengguna terpasang bersamaan dengan Proksi HTTP atau VPN aktif
 * 3. Normalisasi ke rentang 0..100 dengan diminishing returns:
 *    Score = round(100 * (1 - exp(-0.085 * (sum(w_i) + C_corr))))
 * 4. Confidence score memperhitungkan rasio cakupan modul pemeriksaan aktif & kekuatan bukti.
 */
class RiskScoringEngine {

    data class ScoringOutput(
        val riskScore0to100: Int,
        val verdict: AssessmentVerdict,
        val confidencePercent: Int,
        val rawWeightedSum: Double,
        val correlationBonus: Double,
        val supportingSummary: String,
        val weakeningSummary: String
    )

    fun evaluate(
        indicators: List<SecurityIndicator>,
        analyzedApps: List<AppRiskSummary>,
        coverageCheckedModules: Int,
        coverageTotalModules: Int,
        hasUserCaCerts: Boolean,
        hasProxyOrVpn: Boolean
    ): ScoringOutput {
        val coverageRatio = if (coverageTotalModules > 0) {
            coverageCheckedModules.toDouble() / coverageTotalModules.toDouble()
        } else {
            0.0
        }

        // Hitung skor berbobot tiap indikator
        val rawSum = indicators.sumOf { indicator ->
            indicator.severity.weight *
                indicator.evidenceStrength.factor *
                indicator.reliability.factor
        }

        // Hitung bonus korelasi antarindikator independen
        var correlationBonus = 0.0
        val hasMultiPrivilegeNonSystemApp = analyzedApps.any { app ->
            !app.isSystemApp && (
                (app.hasActiveAccessibility && app.hasActiveNotificationAccess) ||
                    (app.hasActiveAccessibility && app.isActiveDeviceAdmin) ||
                    (app.hasActiveNotificationAccess && app.isActiveDeviceAdmin)
                )
        }
        if (hasMultiPrivilegeNonSystemApp) {
            correlationBonus += 4.0
        }

        val hasSideloadWithHighPrivilege = analyzedApps.any { app ->
            app.isSideloaded && (app.hasActiveAccessibility || app.isActiveDeviceAdmin || app.hasActiveNotificationAccess)
        }
        if (hasSideloadWithHighPrivilege) {
            correlationBonus += 3.5
        }

        if (hasUserCaCerts && hasProxyOrVpn) {
            correlationBonus += 3.0
        }

        val totalScore = rawSum + correlationBonus
        val normalizedScore = if (totalScore <= 0.0) {
            0
        } else {
            (100.0 * (1.0 - exp(-0.085 * totalScore))).roundToInt().coerceIn(1, 100)
        }

        val highOrCriticalCount = indicators.count {
            it.severity == IndicatorSeverity.HIGH || it.severity == IndicatorSeverity.CRITICAL
        }
        val strongEvidenceCount = indicators.count {
            it.evidenceStrength == EvidenceStrength.STRONG
        }

        // Klasifikasi keluaran yang jujur dan menghindari kepastian palsu
        val verdict = when {
            coverageRatio < 0.50 -> AssessmentVerdict.INCONCLUSIVE_LIMITED_COVERAGE
            highOrCriticalCount >= 2 && (hasMultiPrivilegeNonSystemApp || hasSideloadWithHighPrivilege || strongEvidenceCount >= 2) ->
                AssessmentVerdict.STRONG_INDICATORS_FORENSIC_NEEDED
            highOrCriticalCount >= 1 || indicators.size >= 3 || normalizedScore >= 40 ->
                AssessmentVerdict.FURTHER_INVESTIGATION_NEEDED
            indicators.isNotEmpty() || normalizedScore >= 12 ->
                AssessmentVerdict.REVIEW_RECOMMENDED
            else ->
                AssessmentVerdict.NO_SIGNIFICANT_INDICATORS
        }

        // Confidence dipengaruhi kelengkapan cakupan modul yang diizinkan pengguna
        val baseConfidence = (coverageRatio * 75.0)
        val evidenceBonus = if (indicators.isEmpty()) {
            10.0
        } else {
            (indicators.map { it.reliability.factor }.average() * 20.0)
        }
        val confidencePercent = (baseConfidence + evidenceBonus).roundToInt().coerceIn(25, 92)

        val supportingSummary = if (indicators.isEmpty()) {
            "Seluruh pemeriksaan API lokal yang aktif ($coverageCheckedModules/$coverageTotalModules modul) tidak menemukan indikator berisiko tinggi."
        } else {
            "Ditemukan ${indicators.size} indikator pada $coverageCheckedModules/$coverageTotalModules modul aktif " +
                "(termasuk $highOrCriticalCount indikator tingkat Tinggi/Sangat Tinggi)."
        }

        val weakeningSummary = buildString {
            append("Pemeriksaan tanpa root & tanpa QUERY_ALL_PACKAGES memiliki keterbatasan visibilitas terhadap aplikasi tersembunyi tanpa ikon, intersepsi seluler, dan eksploit OS/firmware.")
            if (indicators.any { it.evidenceStrength == EvidenceStrength.WEAK || it.evidenceStrength == EvidenceStrength.MODERATE }) {
                append(" Sebagian indikator dapat berasal dari aplikasi sah (mis. antivirus, VPN kerja, atau fitur aksesibilitas).")
            }
        }

        return ScoringOutput(
            riskScore0to100 = normalizedScore,
            verdict = verdict,
            confidencePercent = confidencePercent,
            rawWeightedSum = rawSum,
            correlationBonus = correlationBonus,
            supportingSummary = supportingSummary,
            weakeningSummary = weakeningSummary
        )
    }
}
