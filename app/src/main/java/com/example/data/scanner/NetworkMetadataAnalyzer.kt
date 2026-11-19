package com.example.data.scanner

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import com.example.domain.model.DetectionReliability
import com.example.domain.model.EvidenceStrength
import com.example.domain.model.IndicatorSeverity
import com.example.domain.model.NetworkMetadataSummary
import com.example.domain.model.SecurityIndicator
import com.example.domain.model.ThreatCategory

class NetworkMetadataAnalyzer(private val context: Context) {

    data class Output(
        val summary: NetworkMetadataSummary,
        val indicators: List<SecurityIndicator>,
        val moduleExecuted: Boolean
    )

    fun analyze(
        userCaSubjects: List<String>,
        includeNetworkScan: Boolean
    ): Output {
        if (!includeNetworkScan) {
            return Output(
                summary = NetworkMetadataSummary(
                    isConnected = false,
                    transportLabel = "Pemeriksaan dinonaktifkan oleh pengguna",
                    isVpnActive = false,
                    hasHttpProxy = false,
                    proxyDescription = null,
                    isPrivateDnsActive = null,
                    privateDnsServer = null,
                    userCaCertCount = userCaSubjects.size,
                    userCaSubjects = userCaSubjects
                ),
                indicators = emptyList(),
                moduleExecuted = false
            )
        }

        val indicators = mutableListOf<SecurityIndicator>()
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val capabilities = activeNetwork?.let { cm.getNetworkCapabilities(it) }
        val linkProperties = activeNetwork?.let { cm.getLinkProperties(it) }

        val isConnected = activeNetwork != null && capabilities != null
        val isVpn = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        val transportLabel = when {
            !isConnected -> "Tidak terhubung ke jaringan"
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true && isVpn -> "Wi-Fi + Terowongan VPN"
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true && isVpn -> "Seluler + Terowongan VPN"
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi"
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Data Seluler"
            isVpn -> "VPN Aktif"
            else -> "Jaringan Lainnya"
        }

        val httpProxy = linkProperties?.httpProxy
        val hasHttpProxy = httpProxy != null && !httpProxy.host.isNullOrBlank()
        val proxyDesc = if (hasHttpProxy) "${httpProxy?.host}:${httpProxy?.port}" else null

        val isPrivateDnsActive: Boolean? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            linkProperties?.isPrivateDnsActive
        } else {
            null
        }
        val privateDnsServer: String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            linkProperties?.privateDnsServerName
        } else {
            null
        }

        if (hasHttpProxy) {
            indicators += SecurityIndicator(
                id = "net_http_proxy_active",
                title = "Proksi HTTP aktif pada jaringan ($proxyDesc)",
                category = ThreatCategory.B_SUSPICIOUS_CONFIG_NETWORK,
                severity = IndicatorSeverity.HIGH,
                evidenceStrength = EvidenceStrength.STRONG,
                reliability = DetectionReliability.HIGH_DIRECT_OS_API,
                observedEvidence = "LinkProperties.httpProxy mengarah ke $proxyDesc pada koneksi aktif ($transportLabel).",
                normalExplanation = "Biasa digunakan pada jaringan Wi-Fi kantor/kampus atau saat pengembang memakai alat inspeksi jaringan.",
                confidenceNote = "Tinggi — pengaturan proksi aktif tercatat langsung pada antarmuka jaringan saat ini.",
                remediationSteps = listOf(
                    "Buka Setelan Wi-Fi jaringan yang sedang tersambung > Ubah jaringan > Opsi lanjutan > Proksi.",
                    "Jika bukan jaringan kantor yang Anda kenali, ubah Proksi ke 'Tidak ada'."
                ),
                settingsIntentAction = Settings.ACTION_WIFI_SETTINGS
            )
        }

        if (isVpn) {
            indicators += SecurityIndicator(
                id = "net_vpn_active",
                title = "Koneksi VPN aktif terdeteksi saat pemeriksaan",
                category = ThreatCategory.B_SUSPICIOUS_CONFIG_NETWORK,
                severity = IndicatorSeverity.LOW,
                evidenceStrength = EvidenceStrength.MODERATE,
                reliability = DetectionReliability.HIGH_DIRECT_OS_API,
                observedEvidence = "NetworkCapabilities.TRANSPORT_VPN bernilai true.",
                normalExplanation = "Sangat normal jika Anda mengaktifkan aplikasi VPN privasi, VPN kantor, atau pemblokir iklan berbasis VPN lokal.",
                confidenceNote = "Informasi — aplikasi tidak dapat melihat isi lalu lintas VPN maupun memverifikasi kepemilikan server VPN pihak ketiga.",
                remediationSteps = listOf(
                    "Pastikan ikon kunci/VPN di bilah status memang berasal dari aplikasi VPN yang Anda aktifkan sendiri.",
                    "Periksa di Setelan > Jaringan & Internet > VPN untuk memastikan tidak ada profil VPN tak dikenal."
                ),
                settingsIntentAction = Settings.ACTION_VPN_SETTINGS
            )
        }

        return Output(
            summary = NetworkMetadataSummary(
                isConnected = isConnected,
                transportLabel = transportLabel,
                isVpnActive = isVpn,
                hasHttpProxy = hasHttpProxy,
                proxyDescription = proxyDesc,
                isPrivateDnsActive = isPrivateDnsActive,
                privateDnsServer = privateDnsServer,
                userCaCertCount = userCaSubjects.size,
                userCaSubjects = userCaSubjects
            ),
            indicators = indicators,
            moduleExecuted = true
        )
    }
}
