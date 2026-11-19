package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.ScanRecordEntity
import com.example.domain.model.AssessmentVerdict
import com.example.domain.model.ScanAssessmentResult
import com.example.domain.model.SensorAndPermissionSummary
import com.example.domain.model.ThreatCategory
import com.example.ui.components.InfoBannerCard
import com.example.ui.components.RiskScoreGaugeCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.SecurityIndicatorCard
import com.example.ui.components.StalkerwareSafetyBanner
import com.example.ui.components.openSystemTarget
import com.example.ui.theme.SlateOutline
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.StatusClear
import com.example.ui.theme.StatusHighRisk
import com.example.ui.theme.StatusReview
import com.example.ui.viewmodel.ScanExecutionState
import com.example.ui.viewmodel.SecurityUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val latestRoomRecord = uiState.history.firstOrNull()

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
                title = "Dasbor Keamanan Perangkat",
                subtitle = "Ringkasan izin aktif, sensor perangkat, risiko potensial, & riwayat Room lokal"
            )

            when (val scan = uiState.scanState) {
                is ScanExecutionState.Idle -> {
                    if (latestRoomRecord != null) {
                        RestoredRoomStatusSection(
                            record = latestRoomRecord,
                            onRunQuickScan = onRunQuickScan,
                            onOpenHistory = onOpenHistory
                        )
                    } else {
                        IdleScanCard(
                            lastHistoryCount = uiState.history.size,
                            onStartScan = onRunQuickScan
                        )
                    }
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
                    CompletedDashboardSection(
                        result = result,
                        onRunQuickScan = onRunQuickScan,
                        onOpenDetailedAssessment = onOpenDetailedAssessment,
                        onOpenFindings = onOpenFindings,
                        onOpenNetworkAnalysis = onOpenNetworkAnalysis
                    )
                }
            }

            // Room-persisted Recent Scan History directly visible on Main Dashboard
            RecentRoomHistoryDashboardCard(
                history = uiState.history,
                onOpenAllHistory = onOpenHistory
            )

            // Guided Check Shortcut for Categories C, D, E
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

            InfoBannerCard(
                title = "Prinsip Kejujuran Deteksi",
                body = "Hasil 'Tidak Ada Indikator Signifikan' berarti tidak ditemukan anomali pada API yang dapat diakses. Aplikasi pihak ketiga tidak dapat memastikan ketiadaan eksploit OS atau intersepsi jaringan operator."
            )
        }
    }
}

@Composable
private fun CompletedDashboardSection(
    result: ScanAssessmentResult,
    onRunQuickScan: () -> Unit,
    onOpenDetailedAssessment: () -> Unit,
    onOpenFindings: () -> Unit,
    onOpenNetworkAnalysis: () -> Unit
) {
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

    if (result.indicators.any { it.category == ThreatCategory.A_SPYWARE_STALKERWARE }) {
        StalkerwareSafetyBanner()
    }

    // 1. Active Permissions Summary Card
    ActivePermissionsSummaryCard(
        summary = result.sensorAndPermissionSummary,
        onInspectApps = onOpenDetailedAssessment
    )

    // 2. Enabled Sensors & Hardware Privacy Readiness Card
    EnabledSensorsSummaryCard(
        summary = result.sensorAndPermissionSummary
    )

    // 3. Potential Risks Summary Card (directly on Dashboard)
    PotentialRisksSummaryCard(
        result = result,
        onOpenAllFindings = onOpenFindings
    )

    // 4. Quick OS & Network Overview Grid
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

@Composable
private fun RestoredRoomStatusSection(
    record: ScanRecordEntity,
    onRunQuickScan: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val verdict = runCatching { AssessmentVerdict.valueOf(record.verdictName) }
        .getOrDefault(AssessmentVerdict.REVIEW_RECOMMENDED)
    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"))
        .format(Date(record.timestampMillis))

    RiskScoreGaugeCard(
        riskScore = record.riskScore,
        verdict = verdict,
        confidencePercent = record.confidencePercent,
        coverageChecked = record.coverageChecked,
        coverageTotal = record.coverageTotal
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, SlateOutline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_restored_room_snapshot")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Ringkasan Terakhir (Room DB)",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Izin aktif tercatat: Mikrofon (${record.activeMicAppsCount} app), Kamera (${record.activeCameraAppsCount} app), Lokasi (${record.activeLocationAppsCount} app) • Sensor perangkat: ${record.enabledSensorsCount}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Temuan utama: ${record.topFindingTitle}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onRunQuickScan,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_quick_scan")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pindai Ulang")
                }
                OutlinedButton(
                    onClick = onOpenHistory,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Riwayat")
                }
            }
        }
    }
}

@Composable
private fun ActivePermissionsSummaryCard(
    summary: SensorAndPermissionSummary,
    onInspectApps: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SlateOutline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_active_permissions_summary")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Apps,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Ringkasan Izin Aktif",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                TextButton(onClick = onInspectApps) {
                    Text("Rincian")
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PermissionStatTile(
                    icon = Icons.Filled.Mic,
                    label = "Mikrofon",
                    count = summary.appsWithMicPermission,
                    modifier = Modifier.weight(1f)
                )
                PermissionStatTile(
                    icon = Icons.Filled.CameraAlt,
                    label = "Kamera",
                    count = summary.appsWithCameraPermission,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PermissionStatTile(
                    icon = Icons.Filled.LocationOn,
                    label = "Lokasi",
                    count = summary.appsWithLocationPermission,
                    modifier = Modifier.weight(1f)
                )
                PermissionStatTile(
                    icon = Icons.Filled.Sms,
                    label = "SMS / Telp",
                    count = summary.appsWithSmsOrCallPermission,
                    modifier = Modifier.weight(1f)
                )
            }

            Surface(
                color = SlateSurface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, SlateOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AdminPanelSettings,
                        contentDescription = null,
                        tint = if (summary.activeAccessibilityCount + summary.activeDeviceAdminCount > 0) StatusReview else StatusClear,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Layanan Istimewa Aktif: Aksesibilitas (${summary.activeAccessibilityCount}) • Notifikasi (${summary.activeNotificationListenerCount}) • Device Admin (${summary.activeDeviceAdminCount})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionStatTile(
    icon: ImageVector,
    label: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SlateSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SlateOutline),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = "$count app",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EnabledSensorsSummaryCard(
    summary: SensorAndPermissionSummary
) {
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SlateOutline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_enabled_sensors_summary")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Sensors,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Sensor Perangkat & Perlindungan Sensor",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Text(
                text = "Terdeteksi ${summary.totalHardwareSensors} sensor perangkat keras aktif via SensorManager.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SensorChip(label = "Kamera: ${if (summary.hasCameraHardware) "Ada" else "Tidak"}")
                SensorChip(label = "Mikrofon: ${if (summary.hasMicrophoneHardware) "Ada" else "Tidak"}")
                SensorChip(label = "GPS: ${if (summary.hasGpsHardware) "Ada" else "Tidak"}")
                summary.supportsMicPrivacyToggle?.let {
                    SensorChip(label = "Tombol Privasi Mic: ${if (it) "Didukung" else "Tidak"}")
                }
                summary.supportsCameraPrivacyToggle?.let {
                    SensorChip(label = "Tombol Privasi Kamera: ${if (it) "Didukung" else "Tidak"}")
                }
            }

            if (summary.sampleSensorNames.isNotEmpty()) {
                Text(
                    text = "Contoh sensor aktif: ${summary.sampleSensorNames.joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = { openSystemTarget(context, "android.intent.action.REVIEW_PERMISSION_USAGE") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Cek Riwayat Akses Sensor (Dasbor Privasi)")
            }
        }
    }
}

@Composable
private fun SensorChip(label: String) {
    Surface(
        color = SlateSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, SlateOutline)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun PotentialRisksSummaryCard(
    result: ScanAssessmentResult,
    onOpenAllFindings: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SlateOutline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_potential_risks_summary")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (result.indicators.isEmpty()) Icons.Filled.VerifiedUser else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = if (result.indicators.isEmpty()) StatusClear else StatusReview,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Risiko Potensial (${result.indicators.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                TextButton(onClick = onOpenAllFindings) {
                    Text("Semua Temuan")
                }
            }

            if (result.indicators.isEmpty()) {
                Text(
                    text = "Belum ada indikator risiko menonjol yang teramati pada modul aktif. Tetap lakukan pemeriksaan mandiri akun & jaringan secara berkala.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                result.indicators.take(3).forEach { indicator ->
                    SecurityIndicatorCard(indicator = indicator)
                }
                if (result.indicators.size > 3) {
                    OutlinedButton(
                        onClick = onOpenAllFindings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Lihat ${result.indicators.size - 3} Indikator Lainnya")
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentRoomHistoryDashboardCard(
    history: List<ScanRecordEntity>,
    onOpenAllHistory: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.forLanguageTag("id-ID"))

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SlateOutline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_room_history_dashboard")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Riwayat Pemindaian (Room)",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                TextButton(
                    onClick = onOpenAllHistory,
                    modifier = Modifier.testTag("btn_dashboard_open_history")
                ) {
                    Text("Lihat Semua (${history.size})")
                }
            }

            if (history.isEmpty()) {
                Text(
                    text = "Belum ada riwayat tersimpan di basis data lokal Room. Jalankan pemeriksaan untuk menyimpan catatan pertama.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                history.take(3).forEachIndexed { index, record ->
                    if (index > 0) {
                        HorizontalDivider(color = SlateOutline)
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenAllHistory() }
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = record.verdictShortBadge,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${dateFormat.format(Date(record.timestampMillis))} • ${record.indicatorCount} temuan • ${record.enabledSensorsCount} sensor",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Surface(
                            color = SlateSurface,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SlateOutline)
                        ) {
                            Text(
                                text = "${record.riskScore}/100",
                                style = MaterialTheme.typography.labelLarge,
                                color = when {
                                    record.riskScore >= 50 -> StatusHighRisk
                                    record.riskScore >= 15 -> StatusReview
                                    else -> StatusClear
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
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
                        text = if (lastHistoryCount > 0) "$lastHistoryCount riwayat tersedia" else "Belum ada pemeriksaan tersimpan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Pemeriksaan cepat akan mengevaluasi kunci layar, patch keamanan, sensor aktif, izin mikrofon/kamera/lokasi, layanan Aksesibilitas, Pendengar Notifikasi, Admin Perangkat, Sertifikat CA, dan metadata jaringan.",
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
