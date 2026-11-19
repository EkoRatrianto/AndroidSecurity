package com.example.ui.screens

import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppRiskSummary
import com.example.domain.model.IndicatorSeverity
import com.example.domain.model.ScanAssessmentResult
import com.example.domain.model.ThreatCategory
import com.example.ui.components.InfoBannerCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.SecurityIndicatorCard
import com.example.ui.components.openSystemTarget
import com.example.ui.theme.SlateOutline
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.StatusClear
import com.example.ui.theme.StatusHighRisk
import com.example.ui.theme.StatusReview
import com.example.ui.viewmodel.ScanExecutionState
import com.example.ui.viewmodel.SecurityUiState

@Composable
fun DetailedAssessmentScreen(
    uiState: SecurityUiState,
    onRunScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Temuan", "Kesehatan OS", "Aplikasi & Izin", "Jaringan")

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        modifier = Modifier.testTag("tab_detail_$index")
                    )
                }
            }

            when (val scan = uiState.scanState) {
                is ScanExecutionState.Completed -> {
                    val result = scan.result
                    when (selectedTab) {
                        0 -> SuspiciousFindingsContent(result = result)
                        1 -> DeviceHealthDetailContent(result = result)
                        2 -> AppInventoryDetailContent(result = result)
                        3 -> NetworkAnalysisContent(result = result)
                    }
                }
                else -> {
                    EmptyAssessmentPlaceholder(onRunScan = onRunScan)
                }
            }
        }
    }
}

@Composable
fun SuspiciousFindingsContent(result: ScanAssessmentResult) {
    var selectedSeverity by rememberSaveable { mutableIntStateOf(-1) }
    val severities = listOf(
        IndicatorSeverity.HIGH,
        IndicatorSeverity.MEDIUM,
        IndicatorSeverity.LOW
    )

    val filtered = if (selectedSeverity == -1) {
        result.indicators
    } else {
        val target = severities[selectedSeverity]
        result.indicators.filter {
            it.severity == target || (target == IndicatorSeverity.HIGH && it.severity == IndicatorSeverity.CRITICAL)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        SectionHeader(
            title = "Temuan & Indikator (${result.indicators.size})",
            subtitle = "Ketuk kartu untuk melihat bukti teknis, kemungkinan penyebab normal, dan langkah aman"
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            FilterChip(
                selected = selectedSeverity == -1,
                onClick = { selectedSeverity = -1 },
                label = { Text("Semua (${result.indicators.size})") }
            )
            severities.forEachIndexed { idx, sev ->
                val count = result.indicators.count {
                    it.severity == sev || (sev == IndicatorSeverity.HIGH && it.severity == IndicatorSeverity.CRITICAL)
                }
                FilterChip(
                    selected = selectedSeverity == idx,
                    onClick = { selectedSeverity = idx },
                    label = { Text("${sev.label} ($count)") }
                )
            }
        }

        if (filtered.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, SlateOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = StatusClear,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Tidak Ada Indikator pada Filter Ini",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Tidak ada temuan yang cocok. Tetap tinjau daftar keterbatasan cakupan serta panduan cek akun & seluler.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            filtered.forEach { indicator ->
                SecurityIndicatorCard(indicator = indicator)
            }
        }

        SectionHeader(
            title = "Keterbatasan Pemeriksaan",
            subtitle = "Transparansi cakupan agar tidak menimbulkan rasa aman semu"
        )
        result.limitations.forEach { lim ->
            InfoBannerCard(title = lim.title, body = lim.explanation)
        }
    }
}

@Composable
private fun DeviceHealthDetailContent(result: ScanAssessmentResult) {
    val health = result.deviceHealth
    val context = LocalContext.current

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        SectionHeader(
            title = "Kesehatan Sistem & Konfigurasi OS",
            subtitle = "F01 & F05 — Status sistem yang terbaca langsung dari API Android"
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SlateOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                KeyValueRow("Perangkat", "${health.manufacturer} ${health.model}")
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SlateOutline)
                KeyValueRow("Versi OS", "Android ${health.osVersion} (API ${health.sdkInt})")
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SlateOutline)
                KeyValueRow("Patch Keamanan", "${health.securityPatch}\n${health.patchAgeStatus}")
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SlateOutline)
                KeyValueRow(
                    "Kunci Layar Aman",
                    if (health.isScreenLockSecure) "Aktif" else "Tidak Aktif (Berisiko)",
                    valueColor = if (health.isScreenLockSecure) StatusClear else StatusHighRisk
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SlateOutline)
                KeyValueRow(
                    "USB Debugging (ADB)",
                    if (health.isAdbEnabled) "Aktif" else "Nonaktif",
                    valueColor = if (health.isAdbEnabled) StatusReview else StatusClear
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SlateOutline)
                KeyValueRow(
                    "Indikasi Root / Test-Keys",
                    if (health.isTestKeysBuild || health.suBinaryPathsFound.isNotEmpty()) "Terdeteksi" else "Tidak Teramati",
                    valueColor = if (health.isTestKeysBuild || health.suBinaryPathsFound.isNotEmpty()) StatusHighRisk else StatusClear
                )
                if (health.isAdvancedProtectionEnabled != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SlateOutline)
                    KeyValueRow(
                        "Android Advanced Protection",
                        if (health.isAdvancedProtectionEnabled) "Aktif" else "Nonaktif"
                    )
                }
            }
        }

        OutlinedButton(
            onClick = {
                openSystemTarget(context, "android.intent.action.REVIEW_PERMISSION_USAGE")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Buka Dasbor Privasi Android (Kamera/Mikrofon/Lokasi)")
        }
    }
}

@Composable
private fun AppInventoryDetailContent(result: ScanAssessmentResult) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        SectionHeader(
            title = "Audit Aplikasi Terlihat (${result.analyzedApps.size})",
            subtitle = "F02 & F03 — Diurutkan berdasarkan skor indikator lokal tertinggi"
        )

        InfoBannerCard(
            title = "Cakupan Kueri Aplikasi",
            body = "Hanya menampilkan aplikasi berikon peluncur serta layanan Aksesibilitas, Pendengar Notifikasi, dan Admin Perangkat yang terlihat sesuai aturan Google Play (<queries>)."
        )

        val nonSystemApps = result.analyzedApps.filter { !it.isSystemApp || it.localRiskPoints > 0 }
        if (nonSystemApps.isEmpty()) {
            InfoBannerCard(
                title = "Tidak Ada Aplikasi Non-Sistem Terlihat",
                body = "Modul audit aplikasi mungkin dinonaktifkan atau hanya paket sistem yang terdeteksi."
            )
        } else {
            nonSystemApps.take(40).forEach { app ->
                AppSummaryCard(app = app)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppSummaryCard(app: AppRiskSummary) {
    val context = LocalContext.current
    val badgeColor = when {
        app.localRiskPoints >= 35 -> StatusHighRisk
        app.localRiskPoints >= 15 -> StatusReview
        else -> StatusClear
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SlateOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appLabel,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = SlateSurface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "Poin ${app.localRiskPoints}",
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "Sumber instalasi: ${app.installerSourceLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (app.riskFlags.isNotEmpty() || app.dangerousPermissions.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    app.riskFlags.forEach { flag ->
                        TagPill(text = flag, tint = StatusReview)
                    }
                    app.dangerousPermissions.forEach { perm ->
                        TagPill(text = perm, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    openSystemTarget(
                        context = context,
                        actionOrUrl = Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        packageName = app.packageName
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Periksa Izin di Setelan Android")
            }
        }
    }
}

@Composable
private fun NetworkAnalysisContent(result: ScanAssessmentResult) {
    val net = result.networkMetadata
    val context = LocalContext.current

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        SectionHeader(
            title = "Analisis Metadata Jaringan & Kredensial",
            subtitle = "F04 — Tidak membaca atau mendekripsi isi lalu lintas Anda"
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SlateOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.NetworkCheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Ringkasan Koneksi Aktif", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(12.dp))
                KeyValueRow("Jalur Jaringan", net.transportLabel)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SlateOutline)
                KeyValueRow(
                    "VPN Aktif",
                    if (net.isVpnActive) "Ya (Terdeteksi)" else "Tidak",
                    valueColor = if (net.isVpnActive) StatusReview else StatusClear
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SlateOutline)
                KeyValueRow(
                    "Proksi HTTP",
                    net.proxyDescription ?: "Tidak Ada",
                    valueColor = if (net.hasHttpProxy) StatusHighRisk else StatusClear
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SlateOutline)
                KeyValueRow(
                    "Private DNS (DoT)",
                    when (net.isPrivateDnsActive) {
                        true -> net.privateDnsServer ?: "Aktif (Otomatis/Khusus)"
                        false -> "Nonaktif"
                        null -> "Tidak tersedia pada versi Android ini"
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SlateOutline)
                KeyValueRow(
                    "Sertifikat CA Pengguna",
                    if (net.userCaCertCount == 0) "0 terpasang" else "${net.userCaCertCount} terpasang",
                    valueColor = if (net.userCaCertCount > 0) StatusHighRisk else StatusClear
                )
            }
        }

        if (net.userCaSubjects.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SlateOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Daftar Sertifikat CA Pengguna", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    net.userCaSubjects.forEach { subj ->
                        Text(
                            text = "• $subj",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = { openSystemTarget(context, Settings.ACTION_VPN_SETTINGS) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Tinjau Setelan VPN Perangkat")
        }

        InfoBannerCard(
            title = "Batasan Analisis Jaringan",
            body = "Pemeriksaan ini hanya membaca metadata konfigurasi jaringan (VPN, proksi, DNS, sertifikat CA). JagaPerangkat tidak mencegat paket terenkripsi dan tidak mendeteksi serangan tingkat operator/SS7."
        )
    }
}

@Composable
private fun EmptyAssessmentPlaceholder(onRunScan: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Belum Ada Hasil Pemeriksaan Aktif",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Jalankan pemeriksaan cepat terlebih dahulu untuk melihat rincian sistem, aplikasi, izin, dan jaringan.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRunScan) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Mulai Pemeriksaan Cepat")
        }
    }
}

@Composable
private fun KeyValueRow(
    key: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.45f)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            modifier = Modifier.weight(0.55f)
        )
    }
}

@Composable
private fun TagPill(text: String, tint: Color) {
    Surface(
        color = tint.copy(alpha = 0.08f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.25f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
