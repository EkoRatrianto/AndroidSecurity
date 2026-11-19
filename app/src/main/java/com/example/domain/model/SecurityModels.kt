package com.example.domain.model

enum class ThreatCategory(
    val code: String,
    val label: String,
    val detectabilityNote: String
) {
    A_SPYWARE_STALKERWARE(
        code = "A",
        label = "Indikator Spyware / Stalkerware Lokal",
        detectabilityNote = "Sebagian dapat diamati langsung melalui izin dan layanan sistem yang terlihat."
    ),
    B_SUSPICIOUS_CONFIG_NETWORK(
        code = "B",
        label = "Konfigurasi & Jaringan Mencurigakan",
        detectabilityNote = "Menunjukkan anomali konfigurasi atau jalur jaringan, bukan bukti penyadapan langsung."
    ),
    C_ACCOUNT_LINKED_SESSIONS(
        code = "C",
        label = "Akun Digital, Perangkat Tertaut & Pengalihan",
        detectabilityNote = "Tidak terlihat lewat API lokal; diperiksa melalui panduan interaktif pemilik akun."
    ),
    D_CELLULAR_SIM_RADIO(
        code = "D",
        label = "Jaringan Seluler, SIM Swap & Intersepsi Radio",
        detectabilityNote = "Di luar jangkauan API aplikasi Android; membutuhkan verifikasi operator atau sinyal."
    ),
    E_OS_FIRMWARE_EXPLOIT(
        code = "E",
        label = "Kompromi OS, Firmware & Baseband Canggih",
        detectabilityNote = "Tidak dapat dipastikan aplikasi biasa; membutuhkan forensik khusus (mis. MVT/AndroidQF)."
    )
}

enum class IndicatorSeverity(val label: String, val weight: Double) {
    LOW("Rendah", 1.0),
    MEDIUM("Sedang", 2.5),
    HIGH("Tinggi", 5.0),
    CRITICAL("Sangat Tinggi", 8.0)
}

enum class EvidenceStrength(val label: String, val factor: Double) {
    WEAK("Lemah (Heuristik)", 0.45),
    MODERATE("Sedang (Konfigurasi/Izin)", 0.75),
    STRONG("Kuat (Indikator Sistem Teramati)", 1.0),
    INCONCLUSIVE("Belum Dapat Disimpulkan", 0.25)
}

enum class DetectionReliability(val label: String, val factor: Double) {
    HIGH_DIRECT_OS_API("Tinggi — API OS Langsung", 1.0),
    MEDIUM_HEURISTIC("Sedang — Kombinasi Metadata", 0.75),
    LOW_METADATA_ONLY("Terbatas — Metadata Parsial", 0.50),
    USER_GUIDED_CHECK("Verifikasi Mandiri Pengguna", 0.40)
}

enum class AssessmentVerdict(
    val title: String,
    val shortBadge: String,
    val description: String
) {
    NO_SIGNIFICANT_INDICATORS(
        title = "Tidak ditemukan indikator signifikan pada pemeriksaan yang tersedia",
        shortBadge = "Tidak Ada Indikator Signifikan",
        description = "Pemeriksaan API lokal yang tersedia tidak menemukan anomali mencurigakan yang menonjol. Hasil ini bukan jaminan mutlak bahwa perangkat bebas dari seluruh jenis penyadapan."
    ),
    REVIEW_RECOMMENDED(
        title = "Ditemukan konfigurasi atau perilaku yang perlu ditinjau",
        shortBadge = "Perlu Ditinjau",
        description = "Terdapat pengaturan sistem, izin aplikasi, atau konfigurasi jaringan yang berisiko atau tidak biasa, namun dapat pula berasal dari penggunaan normal."
    ),
    FURTHER_INVESTIGATION_NEEDED(
        title = "Ditemukan beberapa indikator yang memerlukan investigasi lanjutan",
        shortBadge = "Investigasi Lanjutan",
        description = "Terdapat kombinasi beberapa indikator sensitif (misalnya layanan aksesibilitas, sertifikat pengguna, atau aplikasi sideload) yang perlu diperiksa lebih cermat."
    ),
    STRONG_INDICATORS_FORENSIC_NEEDED(
        title = "Ditemukan bukti kuat kompromi yang memerlukan verifikasi forensik",
        shortBadge = "Perlu Verifikasi Forensik",
        description = "Teramati kombinasi indikator berisiko tinggi pada konfigurasi dan izin sistem. Disarankan melakukan verifikasi lanjutan dengan tenaga ahli atau perangkat forensik."
    ),
    INCONCLUSIVE_LIMITED_COVERAGE(
        title = "Tidak dapat disimpulkan karena keterbatasan pemeriksaan",
        shortBadge = "Cakupan Terbatas",
        description = "Sebagian modul pemeriksaan dinonaktifkan atau dibatasi oleh sistem Android, sehingga kesimpulan risiko belum dapat dinilai secara memadai."
    )
}

data class SecurityIndicator(
    val id: String,
    val title: String,
    val category: ThreatCategory,
    val severity: IndicatorSeverity,
    val evidenceStrength: EvidenceStrength,
    val reliability: DetectionReliability,
    val observedEvidence: String,
    val normalExplanation: String,
    val confidenceNote: String,
    val remediationSteps: List<String>,
    val requiresStalkerwareSafetyCare: Boolean = false,
    val settingsIntentAction: String? = null,
    val relatedPackageName: String? = null,
    val detectedAtMillis: Long = System.currentTimeMillis()
)

data class DeviceHealthSummary(
    val manufacturer: String,
    val model: String,
    val osVersion: String,
    val sdkInt: Int,
    val securityPatch: String,
    val isScreenLockSecure: Boolean,
    val isAdbEnabled: Boolean,
    val isDevOptionsEnabled: Boolean,
    val isTestKeysBuild: Boolean,
    val suBinaryPathsFound: List<String>,
    val isAdvancedProtectionEnabled: Boolean?,
    val patchAgeStatus: String
)

data class NetworkMetadataSummary(
    val isConnected: Boolean,
    val transportLabel: String,
    val isVpnActive: Boolean,
    val hasHttpProxy: Boolean,
    val proxyDescription: String?,
    val isPrivateDnsActive: Boolean?,
    val privateDnsServer: String?,
    val userCaCertCount: Int,
    val userCaSubjects: List<String>
)

data class AppRiskSummary(
    val packageName: String,
    val appLabel: String,
    val installerSourceLabel: String,
    val isSideloaded: Boolean,
    val isSystemApp: Boolean,
    val isDebuggable: Boolean,
    val isTestOnly: Boolean,
    val hasActiveAccessibility: Boolean,
    val hasActiveNotificationAccess: Boolean,
    val isActiveDeviceAdmin: Boolean,
    val dangerousPermissions: List<String>,
    val riskFlags: List<String>,
    val localRiskPoints: Int
)

data class ScanScopeLimitation(
    val title: String,
    val explanation: String
)

data class GuidedCheckItem(
    val id: String,
    val category: ThreatCategory,
    val title: String,
    val instruction: String,
    val normalContext: String,
    val actionLabel: String? = null,
    val actionUriOrIntent: String? = null
)

data class ScanAssessmentResult(
    val scanId: String,
    val timestampMillis: Long,
    val riskScore0to100: Int,
    val verdict: AssessmentVerdict,
    val confidencePercent: Int,
    val coverageCheckedModules: Int,
    val coverageTotalModules: Int,
    val deviceHealth: DeviceHealthSummary,
    val networkMetadata: NetworkMetadataSummary,
    val analyzedApps: List<AppRiskSummary>,
    val indicators: List<SecurityIndicator>,
    val limitations: List<ScanScopeLimitation>
)
