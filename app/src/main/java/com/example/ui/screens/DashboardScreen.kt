package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.ThreatCategory
import com.example.ui.components.InfoBannerCard
import com.example.ui.components.RiskScoreGaugeCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StalkerwareSafetyBanner
import com.example.ui.theme.SlateOutline
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.StatusClear
import com.example.ui.theme.StatusHighRisk
import com.example.ui.theme.StatusReview
import com.example.ui.viewmodel.ScanExecutionState
import com.example.ui.viewmodel.SecurityUiState

@Composable
fun DashboardScreen(
    uiState: SecurityUiState,
    onRunQuickScan: () -> Unit,
    onOpenDetailedAssessment: () -> Unit,
    onOpenFindings: () -> Unit,
    onOpenNetworkAnalysis: () -> Unit,
    onOpenGuidedRecommendations: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .widthIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            SectionHeader(
                title = "Status Keamanan Perangkat",
                subtitle = "Pemeriksaan lokal berbasis API resmi Android • Tanpa unggah otomatis"
            )

            when (val scan = uiState.scanState) {
                is ScanExecutionState.Idle -> {
                    IdleScanCard(
                        lastHistoryCount = uiState.history.size,
                        onStartScan = onRunQuickScan
                    )
                }

                is ScanExecutionState.Scanning -> {
                    QuickScanProgressCard(
                        stepLabel = scan.stepLabel,
                        progress = scan.progress
                    )
                }

                is ScanExecutionState.Error -> {
                    ErrorScanCard(
                        message = scan.message,
                        onRetry = onRunQuickScan
                    )
                }

                is ScanExecutionState.Completed -> {
                    val result = scan.result
                    RiskScoreGaugeCard(
                        riskScore = result.riskScore0to100,
                        verdict = result.verdict,
                        confidencePercent = result.confidencePercent,
                        coverageChecked = result.coverageCheckedModules,
                        coverageTotal = result.coverageTotalModules
                    )

                    Button(
                        onClick = onRunQuickScan,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_rescan_quick")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Jalankan Pemeriksaan Ulang")
                    }

                    val hasCategoryA = result.indicators.any {
                        it.category == ThreatCategory.A_SPYWARE_STALKERWARE
                    }
                    if (hasCategoryA) {
                        StalkerwareSafetyBanner()
                    }

                    // 2x2 Summary Module Grid
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        QuickModuleCard(
                            icon = Icons.Filled.PhoneAndroid,
                            title = "Sistem & OS",
                            value = "Android ${result.deviceHealth.osVersion}",
                            statusHint = if (result.deviceHealth.isScreenLockSecure) "Kunci Aktif" else "Tanpa Kunci Layar",
                            accent = if (result.deviceHealth.isScreenLockSecure) StatusClear else StatusHighRisk,
                            onClick = onOpenDetailedAssessment,
                            modifier = Modifier.weight(1f),
                            testTag = "card_module_device"
                        )
                        QuickModuleCard(
                            icon = Icons.Filled.Apps,
                            title = "Aplikasi Terlihat",
                            value = "${result.analyzedApps.size} Paket",
                            statusHint = "${result.analyzedApps.count { it.isSideloaded }} non-toko resmi",
                            accent = if (result.analyzedApps.any { it.isSideloaded && it.localRiskPoints >= 30 }) StatusReview else StatusClear,
                            onClick = onOpenDetailedAssessment,
                            modifier = Modifier.weight(1f),
                            testTag = "card_module_apps"
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        QuickModuleCard(
                            icon = Icons.Filled.Warning,
                            title = "Temuan Indikator",
                            value = "${result.indicators.size} Catatan",
                            statusHint = "Ketuk untuk tinjau bukti",
                            accent = if (result.indicators.isEmpty()) StatusClear else StatusReview,
                            onClick = onOpenFindings,
                            modifier = Modifier.weight(1f),
                            testTag = "card_module_findings"
                        )
                        QuickModuleCard(
                            icon = Icons.Filled.NetworkCheck,
                            title = "Jaringan & CA",
                            value = result.networkMetadata.transportLabel,
                            statusHint = "${result.networkMetadata.userCaCertCount} CA pengguna",
                            accent = if (result.networkMetadata.hasHttpProxy || result.networkMetadata.userCaCertCount > 0) StatusReview else StatusClear,
                            onClick = onOpenNetworkAnalysis,
                            modifier = Modifier.weight(1f),
                            testTag = "card_module_network"
                        )
                    }
                }
            }

            // Guided Check & History Navigation Shortcuts
            SectionHeader(
                title = "Pemeriksaan di Luar Jangkauan Aplikasi",
                subtitle = "Ancaman akun, pengalihan panggilan, dan sinyal seluler perlu verifikasi mandiri"
            )

            ActionListCard(
                icon = Icons.Filled.Checklist,
                title = "Panduan Cek Akun, WhatsApp, SIM & Forensik",
                subtitle = "${uiState.preferences.completedGuidedCheckIds.size} dari 6 langkah mandiri telah ditinjau",
                onClick = onOpenGuidedRecommendations,
                testTag = "card_shortcut_guided"
            )

            ActionListCard(
                icon = Icons.Filled.History,
                title = "Riwayat & Ekspor Laporan Lokal",
                subtitle = "${uiState.history.size} laporan tersimpan di perangkat ini",
                onClick = onOpenHistory,
                testTag = "card_shortcut_history"
            )

            InfoBannerCard(
                title = "Prinsip Kejujuran Deteksi",
                body = "Hasil 'Tidak Ada Indikator Signifikan' berarti tidak ditemukan anomali pada API yang dapat diakses. Aplikasi pihak ketiga tidak dapat memastikan ketiadaan eksploit OS atau intersepsi jaringan operator."
            )
        }
    }
}

@Composable
private fun IdleScanCard(
    lastHistoryCount: Int,
    onStartScan: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SlateOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Siap Memeriksa Perangkat",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = if (lastHistoryCount > 0) "$lastHistoryCount riwayat tersedia" else "Belum ada pemeriksaan sesi ini",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Pemeriksaan cepat akan mengecek kunci layar, patch keamanan, ADB, layanan Aksesibilitas, Pendengar Notifikasi, Admin Perangkat, Sertifikat CA pengguna, metadata jaringan, dan aplikasi terlihat.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onStartScan,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_quick_scan")
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mulai Pemeriksaan Cepat")
            }
        }
    }
}

@Composable
private fun QuickScanProgressCard(
    stepLabel: String,
    progress: Float
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SlateOutline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_scanning_progress")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Pemeriksaan Sedang Berjalan...",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
            Text(
                text = stepLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorScanCard(
    message: String,
    onRetry: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, StatusHighRisk.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Pemeriksaan Tidak Dapat Diselesaikan",
                style = MaterialTheme.typography.titleMedium,
                color = StatusHighRisk
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Text("Coba Lagi")
            }
        }
    }
}

@Composable
private fun QuickModuleCard(
    icon: ImageVector,
    title: String,
    value: String,
    statusHint: String,
    accent: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SlateOutline),
        modifier = modifier
            .testTag(testTag)
            .clickable(onClick = onClick)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = statusHint,
                style = MaterialTheme.typography.bodySmall,
                color = accent
            )
        }
    }
}

@Composable
private fun ActionListCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, SlateOutline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Buka"
            )
        }
    }
}
