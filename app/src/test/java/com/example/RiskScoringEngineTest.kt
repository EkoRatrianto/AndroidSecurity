package com.example

import com.example.domain.engine.RiskScoringEngine
import com.example.domain.model.AppRiskSummary
import com.example.domain.model.AssessmentVerdict
import com.example.domain.model.DetectionReliability
import com.example.domain.model.EvidenceStrength
import com.example.domain.model.IndicatorSeverity
import com.example.domain.model.SecurityIndicator
import com.example.domain.model.ThreatCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskScoringEngineTest {

    private val engine = RiskScoringEngine()

    @Test
    fun `zero indicators with full coverage yields NO_SIGNIFICANT_INDICATORS and score 0`() {
        val output = engine.evaluate(
            indicators = emptyList(),
            analyzedApps = emptyList(),
            coverageCheckedModules = 4,
            coverageTotalModules = 4,
            hasUserCaCerts = false,
            hasProxyOrVpn = false
        )

        assertEquals(0, output.riskScore0to100)
        assertEquals(AssessmentVerdict.NO_SIGNIFICANT_INDICATORS, output.verdict)
        assertTrue(output.confidencePercent in 70..92)
    }

    @Test
    fun `low module coverage yields INCONCLUSIVE_LIMITED_COVERAGE`() {
        val output = engine.evaluate(
            indicators = emptyList(),
            analyzedApps = emptyList(),
            coverageCheckedModules = 1,
            coverageTotalModules = 4,
            hasUserCaCerts = false,
            hasProxyOrVpn = false
        )

        assertEquals(AssessmentVerdict.INCONCLUSIVE_LIMITED_COVERAGE, output.verdict)
    }

    @Test
    fun `sideloaded app with accessibility and device admin yields STRONG_INDICATORS_FORENSIC_NEEDED`() {
        val indicators = listOf(
            SecurityIndicator(
                id = "ind_1",
                title = "Accessibility non-sistem aktif",
                category = ThreatCategory.A_SPYWARE_STALKERWARE,
                severity = IndicatorSeverity.HIGH,
                evidenceStrength = EvidenceStrength.STRONG,
                reliability = DetectionReliability.HIGH_DIRECT_OS_API,
                observedEvidence = "Accessibility active",
                normalExplanation = "Password manager",
                confidenceNote = "High",
                remediationSteps = listOf("Check app")
            ),
            SecurityIndicator(
                id = "ind_2",
                title = "Device Admin non-sistem aktif",
                category = ThreatCategory.A_SPYWARE_STALKERWARE,
                severity = IndicatorSeverity.HIGH,
                evidenceStrength = EvidenceStrength.STRONG,
                reliability = DetectionReliability.HIGH_DIRECT_OS_API,
                observedEvidence = "Device admin active",
                normalExplanation = "MDM",
                confidenceNote = "High",
                remediationSteps = listOf("Check admin")
            )
        )
        val suspiciousApp = AppRiskSummary(
            packageName = "com.synthetic.tracker",
            appLabel = "System Update Service",
            installerSourceLabel = "Sideload",
            isSideloaded = true,
            isSystemApp = false,
            isDebuggable = false,
            isTestOnly = false,
            hasActiveAccessibility = true,
            hasActiveNotificationAccess = false,
            isActiveDeviceAdmin = true,
            dangerousPermissions = listOf("Mikrofon", "Lokasi Presisi"),
            riskFlags = listOf("Sideload", "Aksesibilitas Aktif", "Device Admin Aktif"),
            localRiskPoints = 70
        )

        val output = engine.evaluate(
            indicators = indicators,
            analyzedApps = listOf(suspiciousApp),
            coverageCheckedModules = 4,
            coverageTotalModules = 4,
            hasUserCaCerts = false,
            hasProxyOrVpn = false
        )

        assertEquals(AssessmentVerdict.STRONG_INDICATORS_FORENSIC_NEEDED, output.verdict)
        assertTrue(output.correlationBonus >= 7.5)
        assertTrue(output.riskScore0to100 in 60..100)
    }
}
