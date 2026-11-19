package com.example.data.scanner

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.example.domain.model.DetectionReliability
import com.example.domain.model.EvidenceStrength
import com.example.domain.model.IndicatorSeverity
import com.example.domain.model.SecurityIndicator
import com.example.domain.model.ThreatCategory
import java.security.KeyStore
import java.security.cert.X509Certificate

class ConfigurationAssessor(private val context: Context) {

    data class ConfigSignals(
        val accessibilityPackages: Set<String>,
        val notificationListenerPackages: Set<String>,
        val deviceAdminPackages: Set<String>,
        val userCaSubjects: List<String>,
        val indicators: List<SecurityIndicator>,
        val moduleExecuted: Boolean
    )

    fun assess(): ConfigSignals {
        val indicators = mutableListOf<SecurityIndicator>()
        val pm = context.packageManager

        // 1. Active Accessibility Services (High-abuse vector for stalkerware & keylogging)
        val accessibilityManager =
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val enabledAccessibility = runCatching {
            accessibilityManager?.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            ).orEmpty()
        }.getOrDefault(emptyList())

        val accessibilityPackages = mutableSetOf<String>()
        for (serviceInfo in enabledAccessibility) {
            val servicePkg = serviceInfo.resolveInfo?.serviceInfo?.packageName ?: continue
            accessibilityPackages += servicePkg
            val appLabel = resolveAppLabel(pm, servicePkg)
            val isSystem = isSystemPackage(pm, servicePkg)

            if (!isSystem) {
                val canReadWindow =
                    (serviceInfo.capabilities and AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT) != 0
                val canPerformGestures =
                    (serviceInfo.capabilities and AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES) != 0
                val canFilterKeys =
                    (serviceInfo.capabilities and AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_FILTER_KEY_EVENTS) != 0

                val capabilityDetails = buildList {
                    if (canReadWindow) add("membaca isi layar")
                    if (canPerformGestures) add("melakukan ketukan otomatis")
                    if (canFilterKeys) add("mencegat input tombol")
                }.ifEmpty { listOf("aksesibilitas aktif") }

                indicators += SecurityIndicator(
                    id = "cfg_accessibility_$servicePkg",
                    title = "Layanan Aksesibilitas non-sistem aktif: $appLabel",
                    category = ThreatCategory.A_SPYWARE_STALKERWARE,
                    severity = if (canReadWindow || canFilterKeys) IndicatorSeverity.HIGH else IndicatorSeverity.MEDIUM,
                    evidenceStrength = EvidenceStrength.STRONG,
                    reliability = DetectionReliability.HIGH_DIRECT_OS_API,
                    observedEvidence = "Paket '$servicePkg' aktif di AccessibilityManager dengan kemampuan: ${capabilityDetails.joinToString(", ")}.",
                    normalExplanation = "Aplikasi sah seperti pembaca layar, pengelola kata sandi (password manager), atau utilitas otomasi yang Anda instal memang membutuhkan izin ini.",
                    confidenceNote = "Tinggi — API AccessibilityManager melaporkan layanan aktif secara langsung, namun intensi penggunaannya perlu Anda verifikasi.",
                    remediationSteps = listOf(
                        "Periksa apakah Anda mengenal aplikasi '$appLabel' ($servicePkg) dan sengaja mengaktifkannya.",
                        "Jika tidak dikenal, buka Setelan > Aksesibilitas dan nonaktifkan layanan tersebut sebelum meninjau pencopotan aplikasi."
                    ),
                    requiresStalkerwareSafetyCare = true,
                    settingsIntentAction = Settings.ACTION_ACCESSIBILITY_SETTINGS,
                    relatedPackageName = servicePkg
                )
            }
        }

        // 2. Active Notification Listeners (Captures WhatsApp/Telegram/SMS OTP previews)
        val rawListeners = runCatching {
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        }.getOrNull().orEmpty()

        val notificationPackages = rawListeners.split(":")
            .mapNotNull { ComponentName.unflattenFromString(it)?.packageName }
            .filter { it.isNotBlank() }
            .toSet()

        for (pkg in notificationPackages) {
            if (!isSystemPackage(pm, pkg) && pkg != context.packageName) {
                val appLabel = resolveAppLabel(pm, pkg)
                indicators += SecurityIndicator(
                    id = "cfg_notif_listener_$pkg",
                    title = "Akses baca seluruh notifikasi aktif: $appLabel",
                    category = ThreatCategory.A_SPYWARE_STALKERWARE,
                    severity = IndicatorSeverity.MEDIUM,
                    evidenceStrength = EvidenceStrength.STRONG,
                    reliability = DetectionReliability.HIGH_DIRECT_OS_API,
                    observedEvidence = "Paket '$pkg' terdaftar pada enabled_notification_listeners sehingga dapat membaca pratinjau pesan dan kode OTP masuk.",
                    normalExplanation = "Wajar untuk aplikasi smartwatch/wearable, peluncur Android khusus, atau asisten notifikasi yang Anda gunakan.",
                    confidenceNote = "Tinggi — daftar pendengar notifikasi terbaca dari pengaturan sistem.",
                    remediationSteps = listOf(
                        "Pastikan '$appLabel' memang aplikasi pendamping perangkat atau utilitas yang Anda percayai.",
                        "Cabut izin melalui Setelan > Aplikasi > Akses Aplikasi Khusus > Akses Notifikasi."
                    ),
                    requiresStalkerwareSafetyCare = true,
                    settingsIntentAction = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS",
                    relatedPackageName = pkg
                )
            }
        }

        // 3. Active Device Administrators (Anti-uninstall & policy lock vector)
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val activeAdmins = runCatching { dpm?.activeAdmins.orEmpty() }.getOrDefault(emptyList())
        val adminPackages = activeAdmins.map { it.packageName }.toSet()

        for (adminComponent in activeAdmins) {
            val pkg = adminComponent.packageName
            if (!isSystemPackage(pm, pkg)) {
                val appLabel = resolveAppLabel(pm, pkg)
                indicators += SecurityIndicator(
                    id = "cfg_device_admin_$pkg",
                    title = "Admin Perangkat non-sistem aktif: $appLabel",
                    category = ThreatCategory.A_SPYWARE_STALKERWARE,
                    severity = IndicatorSeverity.HIGH,
                    evidenceStrength = EvidenceStrength.STRONG,
                    reliability = DetectionReliability.HIGH_DIRECT_OS_API,
                    observedEvidence = "Komponen '${adminComponent.flattenToShortString()}' aktif sebagai Device Administrator.",
                    normalExplanation = "Aplikasi MDM kantor (perangkat kerja), Google Find My Device, atau kontrol orang tua resmi menggunakan hak ini.",
                    confidenceNote = "Tinggi — aplikasi dengan status Device Admin tidak dapat dihapus sebelum status adminnya dinonaktifkan.",
                    remediationSteps = listOf(
                        "Jika ini ponsel pribadi dan Anda tidak menggunakan manajemen kantor, tinjau status admin tersebut.",
                        "Buka Setelan > Keamanan > Aplikasi Admin Perangkat untuk meninjau atau menonaktifkannya."
                    ),
                    requiresStalkerwareSafetyCare = true,
                    settingsIntentAction = Settings.ACTION_SECURITY_SETTINGS,
                    relatedPackageName = pkg
                )
            }
        }

        // 4. User-installed CA Certificates (HTTPS Interception risk)
        val userCaSubjects = inspectUserCaCertificates()
        if (userCaSubjects.isNotEmpty()) {
            indicators += SecurityIndicator(
                id = "cfg_user_ca_certs",
                title = "Sertifikat CA tepercaya buatan pengguna terpasang (${userCaSubjects.size})",
                category = ThreatCategory.B_SUSPICIOUS_CONFIG_NETWORK,
                severity = IndicatorSeverity.HIGH,
                evidenceStrength = EvidenceStrength.STRONG,
                reliability = DetectionReliability.HIGH_DIRECT_OS_API,
                observedEvidence = "Ditemukan sertifikat pada AndroidCAStore dengan awalan 'user:': ${userCaSubjects.take(3).joinToString("; ")}.",
                normalExplanation = "Biasa dipasang oleh jaringan VPN korporat/kampus, proksi inspeksi pengembang (Charles/mitmproxy), atau aplikasi pemblokir iklan lokal tertentu.",
                confidenceNote = "Tinggi — sertifikat CA pengguna memungkinkan pihak yang memegang kunci privatnya memeriksa lalu lintas HTTPS pada aplikasi yang memercayainya.",
                remediationSteps = listOf(
                    "Buka Setelan > Keamanan > Enkripsi & Kredensial > Kredensial Tepercaya > tab Pengguna.",
                    "Hapus sertifikat apa pun yang tidak Anda pasang sendiri atau bukan milik organisasi Anda."
                ),
                settingsIntentAction = Settings.ACTION_SECURITY_SETTINGS
            )
        }

        return ConfigSignals(
            accessibilityPackages = accessibilityPackages,
            notificationListenerPackages = notificationPackages,
            deviceAdminPackages = adminPackages,
            userCaSubjects = userCaSubjects,
            indicators = indicators,
            moduleExecuted = true
        )
    }

    private fun inspectUserCaCertificates(): List<String> {
        return runCatching {
            val ks = KeyStore.getInstance("AndroidCAStore")
            ks.load(null, null)
            val aliases = ks.aliases()
            val subjects = mutableListOf<String>()
            while (aliases.hasMoreElements()) {
                val alias = aliases.nextElement()
                if (alias.startsWith("user:")) {
                    val cert = ks.getCertificate(alias) as? X509Certificate
                    val subject = cert?.subjectX500Principal?.name ?: alias
                    subjects += subject
                }
            }
            subjects
        }.getOrDefault(emptyList())
    }

    private fun resolveAppLabel(pm: PackageManager, packageName: String): String {
        return runCatching {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName)
    }

    private fun isSystemPackage(pm: PackageManager, packageName: String): Boolean {
        return runCatching {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
        }.getOrDefault(false)
    }
}
