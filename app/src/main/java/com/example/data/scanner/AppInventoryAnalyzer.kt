package com.example.data.scanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.example.domain.model.AppRiskSummary
import com.example.domain.model.DetectionReliability
import com.example.domain.model.EvidenceStrength
import com.example.domain.model.IndicatorSeverity
import com.example.domain.model.SecurityIndicator
import com.example.domain.model.ThreatCategory

class AppInventoryAnalyzer(private val context: Context) {

    data class Output(
        val apps: List<AppRiskSummary>,
        val indicators: List<SecurityIndicator>,
        val moduleExecuted: Boolean
    )

    private val trustedStoreInstallers = setOf(
        "com.android.vending", // Google Play Store
        "com.sec.android.app.samsungapps", // Samsung Galaxy Store
        "com.xiaomi.mipicks", // Xiaomi GetApps
        "com.oppo.market", // OPPO App Market
        "com.heytap.market",
        "com.huawei.appmarket",
        "com.amazon.venezia",
        "com.vivo.appstore"
    )

    private val sensitivePermissionsMap = linkedMapOf(
        Manifest.permission.RECORD_AUDIO to "Mikrofon",
        Manifest.permission.CAMERA to "Kamera",
        Manifest.permission.ACCESS_FINE_LOCATION to "Lokasi Presisi",
        "android.permission.ACCESS_BACKGROUND_LOCATION" to "Lokasi Latar Belakang",
        Manifest.permission.READ_SMS to "Baca SMS",
        Manifest.permission.RECEIVE_SMS to "Terima SMS",
        Manifest.permission.READ_CALL_LOG to "Riwayat Panggilan",
        Manifest.permission.SYSTEM_ALERT_WINDOW to "Hamparan Layar (Overlay)"
    )

    fun analyze(
        configSignals: ConfigurationAssessor.ConfigSignals,
        includeAppScan: Boolean
    ): Output {
        if (!includeAppScan) {
            return Output(apps = emptyList(), indicators = emptyList(), moduleExecuted = false)
        }

        val pm = context.packageManager
        val discoveredPackages = linkedSetOf<String>()

        // 1. Launcher apps (visible under <queries> ACTION_MAIN + CATEGORY_LAUNCHER)
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        queryActivityPackages(pm, launcherIntent).forEach { discoveredPackages += it }

        // 2. Scoped query for AccessibilityService, NotificationListenerService, DeviceAdmin
        queryServicePackages(pm, Intent("android.accessibilityservice.AccessibilityService"))
            .forEach { discoveredPackages += it }
        queryServicePackages(pm, Intent("android.service.notification.NotificationListenerService"))
            .forEach { discoveredPackages += it }
        queryReceiverPackages(pm, Intent("android.app.action.DEVICE_ADMIN_ENABLED"))
            .forEach { discoveredPackages += it }

        // 3. Merge active privileged packages discovered directly from OS managers
        discoveredPackages += configSignals.accessibilityPackages
        discoveredPackages += configSignals.notificationListenerPackages
        discoveredPackages += configSignals.deviceAdminPackages

        // Exclude self from risk flagging
        discoveredPackages.remove(context.packageName)

        val appSummaries = mutableListOf<AppRiskSummary>()
        val indicators = mutableListOf<SecurityIndicator>()

        for (pkg in discoveredPackages) {
            val pkgInfo = getPackageInfoSafely(pm, pkg) ?: continue
            val appInfo = pkgInfo.applicationInfo ?: continue

            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            val isDebuggable = (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            val isTestOnly = (appInfo.flags and ApplicationInfo.FLAG_TEST_ONLY) != 0

            val installer = getInstallerPackage(pm, pkg)
            val isSideloaded = !isSystem && (installer == null || installer !in trustedStoreInstallers)
            val installerLabel = when {
                isSystem -> "Bawaan Sistem"
                installer == "com.android.vending" -> "Google Play Store"
                installer in trustedStoreInstallers -> "Toko Pabrikan ($installer)"
                installer == "com.google.android.packageinstaller" || installer == "com.android.packageinstaller" ->
                    "Sideload (Penginstal Paket Lokal)"
                installer != null -> "Sumber Lain ($installer)"
                else -> "Tidak Diketahui / Sideload"
            }

            val grantedSensitive = mutableListOf<String>()
            val requestedPerms = pkgInfo.requestedPermissions.orEmpty()
            for (perm in requestedPerms) {
                val label = sensitivePermissionsMap[perm] ?: continue
                if (pm.checkPermission(perm, pkg) == PackageManager.PERMISSION_GRANTED) {
                    grantedSensitive += label
                }
            }

            val hasAccessibility = pkg in configSignals.accessibilityPackages
            val hasNotifAccess = pkg in configSignals.notificationListenerPackages
            val isDeviceAdmin = pkg in configSignals.deviceAdminPackages

            val flags = mutableListOf<String>()
            var points = 0
            if (isSideloaded) {
                flags += "Sideload / Non-Play Store"
                points += 15
            }
            if (isDebuggable) {
                flags += "Build Debuggable"
                points += 15
            }
            if (isTestOnly) {
                flags += "Build Test-Only"
                points += 10
            }
            if (hasAccessibility && !isSystem) {
                flags += "Aksesibilitas Aktif"
                points += 30
            }
            if (hasNotifAccess && !isSystem) {
                flags += "Akses Notifikasi Aktif"
                points += 20
            }
            if (isDeviceAdmin && !isSystem) {
                flags += "Device Admin Aktif"
                points += 25
            }
            if (!isSystem && grantedSensitive.size >= 4) {
                flags += "${grantedSensitive.size} Izin Sensitif Aktif"
                points += 15
            }

            val appLabel = runCatching { pm.getApplicationLabel(appInfo).toString() }.getOrDefault(pkg)

            val summary = AppRiskSummary(
                packageName = pkg,
                appLabel = appLabel,
                installerSourceLabel = installerLabel,
                isSideloaded = isSideloaded,
                isSystemApp = isSystem,
                isDebuggable = isDebuggable,
                isTestOnly = isTestOnly,
                hasActiveAccessibility = hasAccessibility,
                hasActiveNotificationAccess = hasNotifAccess,
                isActiveDeviceAdmin = isDeviceAdmin,
                dangerousPermissions = grantedSensitive,
                riskFlags = flags,
                localRiskPoints = points.coerceAtMost(100)
            )
            appSummaries += summary

            // Generate indicators for sideloaded or high-privilege combinations (avoiding weak noise)
            if (isSideloaded && (hasAccessibility || isDeviceAdmin || hasNotifAccess || grantedSensitive.size >= 3)) {
                indicators += SecurityIndicator(
                    id = "app_sideload_high_priv_$pkg",
                    title = "Aplikasi non-toko resmi memiliki hak istimewa sensitif: $appLabel",
                    category = ThreatCategory.A_SPYWARE_STALKERWARE,
                    severity = if (hasAccessibility || isDeviceAdmin) IndicatorSeverity.HIGH else IndicatorSeverity.MEDIUM,
                    evidenceStrength = EvidenceStrength.MODERATE,
                    reliability = DetectionReliability.MEDIUM_HEURISTIC,
                    observedEvidence = "Paket '$pkg' berasal dari '$installerLabel' dengan tanda: ${flags.joinToString(", ")} " +
                        if (grantedSensitive.isNotEmpty()) "serta izin aktif: ${grantedSensitive.joinToString(", ")}." else ".",
                    normalExplanation = "Dapat normal jika Anda menginstal aplikasi sumber terbuka (F-Droid), APK pengujian resmi, atau utilitas perusahaan secara manual.",
                    confidenceNote = "Sedang — kombinasi instalasi di luar toko resmi dan hak akses sensitif merupakan pola umum stalkerware, tetapi belum membuktikan niat jahat tanpa analisis paket.",
                    remediationSteps = listOf(
                        "Pastikan Anda mengenali siapa yang memasang '$appLabel' dan kapan dipasang.",
                        "Tinjau izin sensitif aplikasi ini di Setelan > Aplikasi.",
                        "Jika mencurigai pengawasan oleh orang dekat, baca panduan keselamatan sebelum menghapusnya."
                    ),
                    requiresStalkerwareSafetyCare = true,
                    settingsIntentAction = Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    relatedPackageName = pkg
                )
            } else if (!isSystem && isDebuggable) {
                indicators += SecurityIndicator(
                    id = "app_debuggable_$pkg",
                    title = "Aplikasi berstatus Debuggable terpasang: $appLabel",
                    category = ThreatCategory.B_SUSPICIOUS_CONFIG_NETWORK,
                    severity = IndicatorSeverity.MEDIUM,
                    evidenceStrength = EvidenceStrength.STRONG,
                    reliability = DetectionReliability.HIGH_DIRECT_OS_API,
                    observedEvidence = "ApplicationInfo.FLAG_DEBUGGABLE aktif pada '$pkg'. Aplikasi produksi Google Play tidak diperbolehkan berstatus debuggable.",
                    normalExplanation = "Normal jika Anda sendiri adalah pengembang Android yang sedang menguji aplikasi buatan sendiri.",
                    confidenceNote = "Tinggi — flag debuggable menandakan aplikasi bukan rilis toko standar dan mudah disisipi inspeksi.",
                    remediationSteps = listOf(
                        "Jika Anda bukan pengembang aplikasi ini, hapus aplikasi atau ganti dengan versi resmi dari Play Store."
                    ),
                    settingsIntentAction = Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    relatedPackageName = pkg
                )
            }
        }

        val sortedApps = appSummaries.sortedWith(
            compareByDescending<AppRiskSummary> { it.localRiskPoints }
                .thenByDescending { it.dangerousPermissions.size }
                .thenBy { it.appLabel.lowercase() }
        )

        return Output(apps = sortedApps, indicators = indicators, moduleExecuted = true)
    }

    private fun queryActivityPackages(pm: PackageManager, intent: Intent): List<String> {
        return runCatching {
            val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, 0)
            }
            list.mapNotNull { it.activityInfo?.packageName }
        }.getOrDefault(emptyList())
    }

    private fun queryServicePackages(pm: PackageManager, intent: Intent): List<String> {
        return runCatching {
            val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentServices(intent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentServices(intent, 0)
            }
            list.mapNotNull { it.serviceInfo?.packageName }
        }.getOrDefault(emptyList())
    }

    private fun queryReceiverPackages(pm: PackageManager, intent: Intent): List<String> {
        return runCatching {
            val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryBroadcastReceivers(intent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.queryBroadcastReceivers(intent, 0)
            }
            list.mapNotNull { it.activityInfo?.packageName }
        }.getOrDefault(emptyList())
    }

    private fun getPackageInfoSafely(pm: PackageManager, packageName: String): PackageInfo? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            }
        }.getOrNull()
    }

    private fun getInstallerPackage(pm: PackageManager, packageName: String): String? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(packageName)
            }
        }.getOrNull()
    }
}
