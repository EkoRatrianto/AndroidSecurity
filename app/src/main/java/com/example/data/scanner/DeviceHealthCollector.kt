package com.example.data.scanner

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.example.domain.model.DetectionReliability
import com.example.domain.model.DeviceHealthSummary
import com.example.domain.model.EvidenceStrength
import com.example.domain.model.IndicatorSeverity
import com.example.domain.model.SecurityIndicator
import com.example.domain.model.ThreatCategory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DeviceHealthCollector(private val context: Context) {

    data class Output(
        val summary: DeviceHealthSummary,
        val indicators: List<SecurityIndicator>,
        val moduleExecuted: Boolean
    )

    fun collect(): Output {
        val indicators = mutableListOf<SecurityIndicator>()

        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isScreenLockSecure = keyguardManager?.isDeviceSecure ?: false

        val adbEnabled = runCatching {
            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        }.getOrDefault(false)

        val devOptionsEnabled = runCatching {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                0
            ) == 1
        }.getOrDefault(false)

        val isTestKeys = Build.TAGS?.contains("test-keys", ignoreCase = true) == true

        val candidateSuPaths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/app/Superuser.apk",
            "/data/local/xbin/su",
            "/data/local/bin/su"
        )
        val suPathsFound = candidateSuPaths.filter { path ->
            runCatching { File(path).exists() }.getOrDefault(false)
        }

        val advancedProtectionStatus = checkAdvancedProtectionMode()

        val securityPatch = Build.VERSION.SECURITY_PATCH
        val patchAgeDays = computePatchAgeDays(securityPatch)
        val patchAgeStatus = when {
            patchAgeDays == null -> "Tanggal patch ($securityPatch) tidak dapat diuraikan"
            patchAgeDays <= 90 -> "Relatif mutakhir ($patchAgeDays hari lalu)"
            patchAgeDays <= 180 -> "Menua ($patchAgeDays hari lalu)"
            else -> "Tertinggal ($patchAgeDays hari lalu — berisiko rentan)"
        }

        if (!isScreenLockSecure) {
            indicators += SecurityIndicator(
                id = "dev_no_screen_lock",
                title = "Kunci layar aman (PIN/Kata Sandi/Biometrik) tidak aktif",
                category = ThreatCategory.B_SUSPICIOUS_CONFIG_NETWORK,
                severity = IndicatorSeverity.HIGH,
                evidenceStrength = EvidenceStrength.STRONG,
                reliability = DetectionReliability.HIGH_DIRECT_OS_API,
                observedEvidence = "KeyguardManager.isDeviceSecure() mengembalikan false.",
                normalExplanation = "Perangkat mungkin baru disetel ulang atau pengguna sengaja menonaktifkan kunci layar demi kenyamanan.",
                confidenceNote = "Tinggi — diperiksa langsung melalui API sistem Android.",
                remediationSteps = listOf(
                    "Buka Setelan > Keamanan > Kunci Layar.",
                    "Aktifkan PIN minimal 6 digit atau kata sandi alfanumerik yang kuat."
                ),
                settingsIntentAction = Settings.ACTION_SECURITY_SETTINGS
            )
        }

        if (adbEnabled) {
            indicators += SecurityIndicator(
                id = "dev_adb_enabled",
                title = "USB Debugging (ADB) dalam keadaan aktif",
                category = ThreatCategory.B_SUSPICIOUS_CONFIG_NETWORK,
                severity = IndicatorSeverity.MEDIUM,
                evidenceStrength = EvidenceStrength.STRONG,
                reliability = DetectionReliability.HIGH_DIRECT_OS_API,
                observedEvidence = "Settings.Global.ADB_ENABLED = 1.",
                normalExplanation = "Normal jika Anda pengembang aplikasi atau baru menghubungkan perangkat ke komputer untuk debugging/forensik.",
                confidenceNote = "Tinggi — ADB aktif mempermudah pemasangan aplikasi atau ekstraksi data bila akses fisik/USB terbuka.",
                remediationSteps = listOf(
                    "Jika Anda tidak sedang melakukan pengembangan atau pemeriksaan ADB, matikan USB Debugging di Setelan > Opsi Developer.",
                    "Cabut otorisasi komputer USB yang tidak dikenal melalui menu 'Cabut otorisasi debugging USB'."
                ),
                settingsIntentAction = Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
            )
        }

        if (isTestKeys || suPathsFound.isNotEmpty()) {
            indicators += SecurityIndicator(
                id = "dev_root_or_test_keys",
                title = "Terdeteksi indikasi akses Root atau Build Test-Keys",
                category = ThreatCategory.E_OS_FIRMWARE_EXPLOIT,
                severity = IndicatorSeverity.HIGH,
                evidenceStrength = EvidenceStrength.MODERATE,
                reliability = DetectionReliability.MEDIUM_HEURISTIC,
                observedEvidence = buildString {
                    if (isTestKeys) append("Build.TAGS mengandung 'test-keys'. ")
                    if (suPathsFound.isNotEmpty()) append("Jalur biner su ditemukan: ${suPathsFound.joinToString()}.")
                }.trim(),
                normalExplanation = "Dapat terjadi secara sengaja pada perangkat dengan custom ROM, perangkat penguji, atau emulator.",
                confidenceNote = "Sedang — pemeriksaan read-only tanpa hak root tidak dapat mengonfirmasi modifikasi kernel yang tersembunyi.",
                remediationSteps = listOf(
                    "Pastikan apakah Anda sendiri yang melakukan root atau membuka bootloader.",
                    "Jika tidak pernah merasa melakukan root, segera cadangkan data penting dan konsultasikan pemeriksaan OS/firmware resmi."
                ),
                settingsIntentAction = Settings.ACTION_DEVICE_INFO_SETTINGS
            )
        }

        if (patchAgeDays != null && patchAgeDays > 180) {
            indicators += SecurityIndicator(
                id = "dev_outdated_security_patch",
                title = "Patch keamanan Android tertinggal lebih dari 6 bulan",
                category = ThreatCategory.B_SUSPICIOUS_CONFIG_NETWORK,
                severity = IndicatorSeverity.MEDIUM,
                evidenceStrength = EvidenceStrength.STRONG,
                reliability = DetectionReliability.HIGH_DIRECT_OS_API,
                observedEvidence = "Security Patch Level: $securityPatch (usia sekitar $patchAgeDays hari).",
                normalExplanation = "Perangkat model lama mungkin sudah tidak menerima pembaruan keamanan dari produsen.",
                confidenceNote = "Tinggi — pembaruan keamanan yang tertinggal membuat perangkat lebih rentan terhadap eksploit yang telah dipublikasikan.",
                remediationSteps = listOf(
                    "Periksa pembaruan sistem di Setelan > Sistem > Pembaruan Perangkat Lunak.",
                    "Jika produsen sudah menghentikan pembaruan, terapkan kehati-hatian ekstra terhadap tautan dan aplikasi di luar Play Store."
                ),
                settingsIntentAction = Settings.ACTION_SETTINGS
            )
        }

        val summary = DeviceHealthSummary(
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            model = Build.MODEL,
            osVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            securityPatch = securityPatch,
            isScreenLockSecure = isScreenLockSecure,
            isAdbEnabled = adbEnabled,
            isDevOptionsEnabled = devOptionsEnabled,
            isTestKeysBuild = isTestKeys,
            suBinaryPathsFound = suPathsFound,
            isAdvancedProtectionEnabled = advancedProtectionStatus,
            patchAgeStatus = patchAgeStatus
        )

        return Output(summary = summary, indicators = indicators, moduleExecuted = true)
    }

    private fun checkAdvancedProtectionMode(): Boolean? {
        if (Build.VERSION.SDK_INT < 36) return null
        return runCatching {
            val service = context.getSystemService("advanced_protection") ?: return@runCatching null
            val method = service.javaClass.getMethod("isAdvancedProtectionEnabled")
            method.invoke(service) as? Boolean
        }.getOrNull()
    }

    private fun computePatchAgeDays(patchDateStr: String): Long? {
        return runCatching {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val patchDate: Date = sdf.parse(patchDateStr) ?: return null
            val diffMillis = System.currentTimeMillis() - patchDate.time
            if (diffMillis < 0) 0L else TimeUnit.MILLISECONDS.toDays(diffMillis)
        }.getOrNull()
    }
}
