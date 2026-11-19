package com.example.data.scanner

import android.content.Context
import com.example.domain.engine.RiskScoringEngine
import com.example.domain.model.GuidedCheckItem
import com.example.domain.model.ScanAssessmentResult
import com.example.domain.model.ScanScopeLimitation
import com.example.domain.model.ThreatCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SecurityAssessmentCoordinator(
    private val context: Context,
    private val riskScoringEngine: RiskScoringEngine = RiskScoringEngine()
) {
    private val deviceCollector = DeviceHealthCollector(context)
    private val configAssessor = ConfigurationAssessor(context)
    private val appAnalyzer = AppInventoryAnalyzer(context)
    private val networkAnalyzer = NetworkMetadataAnalyzer(context)

    suspend fun runFullAssessment(
        includeAppScan: Boolean,
        includeNetworkScan: Boolean,
        onStepProgress: (String, Float) -> Unit = { _, _ -> }
    ): ScanAssessmentResult = withContext(Dispatchers.IO) {
        onStepProgress("Memeriksa kesehatan sistem & kunci keamanan...", 0.20f)
        val deviceOut = deviceCollector.collect()
        delay(180)

        onStepProgress("Menilai Aksesibilitas, Admin Perangkat & Sertifikat CA...", 0.45f)
        val configOut = configAssessor.assess()
        delay(180)

        onStepProgress("Menganalisis aplikasi & izin sensitif yang terlihat...", 0.70f)
        val appOut = appAnalyzer.analyze(configOut, includeAppScan)
        delay(180)

        onStepProgress("Memeriksa metadata jaringan (VPN, Proksi, DNS)...", 0.90f)
        val networkOut = networkAnalyzer.analyze(configOut.userCaSubjects, includeNetworkScan)
        delay(120)

        val allIndicators = (
            deviceOut.indicators +
                configOut.indicators +
                appOut.indicators +
                networkOut.indicators
            ).sortedByDescending { it.severity.weight }

        val totalModules = 4
        val checkedModules = listOf(
            deviceOut.moduleExecuted,
            configOut.moduleExecuted,
            appOut.moduleExecuted,
            networkOut.moduleExecuted
        ).count { it }

        val scoring = riskScoringEngine.evaluate(
            indicators = allIndicators,
            analyzedApps = appOut.apps,
            coverageCheckedModules = checkedModules,
            coverageTotalModules = totalModules,
            hasUserCaCerts = configOut.userCaSubjects.isNotEmpty(),
            hasProxyOrVpn = networkOut.summary.hasHttpProxy || networkOut.summary.isVpnActive
        )

        val limitations = buildList {
            add(
                ScanScopeLimitation(
                    title = "Visibilitas Paket Tanpa QUERY_ALL_PACKAGES",
                    explanation = "Demi kepatuhan privasi Google Play, pemindaian hanya melihat aplikasi berikon peluncur serta layanan Aksesibilitas, Pendengar Notifikasi, dan Admin Perangkat yang terdaftar. Aplikasi tersembunyi tanpa ikon tidak selalu terlihat."
                )
            )
            add(
                ScanScopeLimitation(
                    title = "Lalu Lintas Terenkripsi & Intersepsi Seluler",
                    explanation = "Aplikasi Android biasa tanpa root tidak dapat membaca isi paket HTTPS, aktivitas baseband/operator seluler, atau eksploit kernel/zero-click."
                )
            )
            if (!includeAppScan) {
                add(
                    ScanScopeLimitation(
                        title = "Audit Aplikasi Dinonaktifkan Pengguna",
                        explanation = "Anda menonaktifkan modul audit aplikasi di Kontrol Privasi; indikator dari daftar aplikasi tidak diperiksa."
                    )
                )
            }
            if (!includeNetworkScan) {
                add(
                    ScanScopeLimitation(
                        title = "Audit Jaringan Dinonaktifkan Pengguna",
                        explanation = "Anda menonaktifkan modul metadata jaringan di Kontrol Privasi; status VPN/Proksi/Private DNS tidak diperiksa."
                    )
                )
            }
        }

        onStepProgress("Menyusun skor & laporan lokal...", 1.0f)

        ScanAssessmentResult(
            scanId = UUID.randomUUID().toString(),
            timestampMillis = System.currentTimeMillis(),
            riskScore0to100 = scoring.riskScore0to100,
            verdict = scoring.verdict,
            confidencePercent = scoring.confidencePercent,
            coverageCheckedModules = checkedModules,
            coverageTotalModules = totalModules,
            deviceHealth = deviceOut.summary,
            networkMetadata = networkOut.summary,
            analyzedApps = appOut.apps,
            indicators = allIndicators,
            limitations = limitations
        )
    }

    fun formatShareableReport(result: ScanAssessmentResult): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.forLanguageTag("id-ID"))
        val timestampStr = dateFormat.format(Date(result.timestampMillis))
        return buildString {
            appendLine("=== LAPORAN AUDIT KEAMANAN JAGAPERANGKAT ===")
            appendLine("Waktu Pemeriksaan : $timestampStr")
            appendLine("Perangkat         : ${result.deviceHealth.manufacturer} ${result.deviceHealth.model} (Android ${result.deviceHealth.osVersion})")
            appendLine("Patch Keamanan    : ${result.deviceHealth.securityPatch} (${result.deviceHealth.patchAgeStatus})")
            appendLine("Status Kesimpulan : ${result.verdict.title}")
            appendLine("Skor Risiko Lokal : ${result.riskScore0to100}/100 (Keyakinan: ${result.confidencePercent}%)")
            appendLine("Cakupan Modul     : ${result.coverageCheckedModules}/${result.coverageTotalModules}")
            appendLine()
            appendLine("--- RINGKASAN TEMUAN (${result.indicators.size}) ---")
            if (result.indicators.isEmpty()) {
                appendLine("Tidak ditemukan indikator risiko menonjol pada modul yang diperiksa.")
            } else {
                result.indicators.forEachIndexed { idx, ind ->
                    appendLine("${idx + 1}. [${ind.severity.label}] ${ind.title}")
                    appendLine("   Bukti: ${ind.observedEvidence}")
                    appendLine("   Konteks Normal: ${ind.normalExplanation}")
                }
            }
            appendLine()
            appendLine("--- KETERBATASAN PEMERIKSAAN ---")
            result.limitations.forEach { lim ->
                appendLine("• ${lim.title}: ${lim.explanation}")
            }
            appendLine()
            appendLine("Catatan: Dihasilkan 100% secara lokal di perangkat. Tidak adanya indikator bukan jaminan mutlak bebas penyadapan.")
        }
    }
}

object GuidedCheckCatalog {
    val items: List<GuidedCheckItem> = listOf(
        GuidedCheckItem(
            id = "guide_google_devices",
            category = ThreatCategory.C_ACCOUNT_LINKED_SESSIONS,
            title = "Periksa Perangkat Tertaut di Akun Google",
            instruction = "Buka Kelola Akun Google Anda > Keamanan > Perangkat Anda. Pastikan tidak ada ponsel atau browser asing yang masih masuk ke akun Anda.",
            normalContext = "Sesi lama dari ponsel pribadi sebelumnya atau komputer kerja sering tertinggal dan perlu Anda keluarkan secara berkala.",
            actionLabel = "Buka Keamanan Akun",
            actionUriOrIntent = "https://myaccount.google.com/security-checkup"
        ),
        GuidedCheckItem(
            id = "guide_chat_linked_devices",
            category = ThreatCategory.C_ACCOUNT_LINKED_SESSIONS,
            title = "Audit Perangkat Tertaut (WhatsApp / Telegram)",
            instruction = "Di WhatsApp buka menu Titik Tiga > Perangkat Tertaut. Di Telegram buka Pengaturan > Perangkat. Keluarkan sesi desktop/web yang tidak Anda kenali.",
            normalContext = "Login WhatsApp Web di kantor atau warnet yang lupa logout sering menjadi celah pengintipan tanpa memasang spyware di ponsel.",
            actionLabel = null,
            actionUriOrIntent = null
        ),
        GuidedCheckItem(
            id = "guide_call_forwarding",
            category = ThreatCategory.C_ACCOUNT_LINKED_SESSIONS,
            title = "Cek Pengalihan Panggilan (Call Forwarding)",
            instruction = "Buka aplikasi Telepon dan periksa apakah nomor Anda mengalihkan panggilan masuk ke nomor tak dikenal melalui Setelan Panggilan > Pengalihan Panggilan (atau kode MMI *#21# di aplikasi telepon).",
            normalContext = "Sebagian operator menggunakan pengalihan bawaan menuju layanan kotak suara (voicemail) resmi mereka.",
            actionLabel = "Buka Pengaturan Panggilan",
            actionUriOrIntent = "android.telecom.action.CHANGE_PHONE_ACCOUNTS"
        ),
        GuidedCheckItem(
            id = "guide_sim_swap_2g",
            category = ThreatCategory.D_CELLULAR_SIM_RADIO,
            title = "Waspadai Tanda SIM Swap & Penurunan Sinyal ke 2G",
            instruction = "Jika kartu SIM tiba-tiba kehilangan sinyal ('Tidak Ada Layanan') padahal area normal, segera hubungi operator resmi. Pada Android 12+, matikan opsi 'Izinkan 2G' di Setelan SIM bila tersedia.",
            normalContext = "Gangguan menara pemancar lokal atau area terpencil dapat menyebabkan penurunan sinyal sementara.",
            actionLabel = "Buka Setelan Jaringan Seluler",
            actionUriOrIntent = "android.settings.NETWORK_OPERATOR_SETTINGS"
        ),
        GuidedCheckItem(
            id = "guide_privacy_dashboard",
            category = ThreatCategory.A_SPYWARE_STALKERWARE,
            title = "Tinjau Dasbor Privasi Android (Kamera, Mikrofon, Lokasi)",
            instruction = "Buka Dasbor Privasi bawaan Android untuk melihat lini masa aplikasi mana saja yang mengakses mikrofon, kamera, dan lokasi dalam 24 jam terakhir.",
            normalContext = "Aplikasi peta, panggilan video, atau kamera wajar muncul saat Anda memakainya, tetapi akses di jam tidur patut dicermati.",
            actionLabel = "Buka Dasbor Privasi",
            actionUriOrIntent = "android.intent.action.REVIEW_PERMISSION_USAGE"
        ),
        GuidedCheckItem(
            id = "guide_forensic_mvt",
            category = ThreatCategory.E_OS_FIRMWARE_EXPLOIT,
            title = "Kesiapan Forensik Lanjutan (MVT & AndroidQF)",
            instruction = "Untuk jurnalis, aktivis, atau pengguna berisiko tinggi yang menduga spyware canggih tingkat OS/zero-click, gunakan Amnesty Mobile Verification Toolkit (MVT) dan AndroidQF melalui komputer yang tepercaya.",
            normalContext = "Langkah ini tidak diperlukan untuk pengguna umum kecuali ada indikasi ancaman bertarget yang kuat.",
            actionLabel = "Pelajari Dokumentasi MVT",
            actionUriOrIntent = "https://docs.mvt.re"
        )
    )
}
