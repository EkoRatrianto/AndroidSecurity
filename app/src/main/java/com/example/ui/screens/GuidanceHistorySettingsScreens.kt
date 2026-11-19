package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.ScanRecordEntity
import com.example.data.scanner.GuidedCheckCatalog
import com.example.ui.components.InfoBannerCard
import com.example.ui.components.SectionHeader
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
fun GuidanceScreen(
    uiState: SecurityUiState,
    onToggleGuidedCheck: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .widthIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            SectionHeader(
                title = "Panduan Keamanan & Cek Mandiri",
                subtitle = "F07 — Langkah bertahap untuk ancaman Kategori C, D, dan E yang tidak terbaca API lokal"
            )

            StalkerwareSafetyBanner()

            if (uiState.scanState is ScanExecutionState.Completed && uiState.scanState.result.indicators.isNotEmpty()) {
                SectionHeader(
                    title = "Prioritas Tindak Lanjut Temuan Anda",
                    subtitle = "Berdasarkan hasil pemeriksaan lokal terbaru"
                )
                uiState.scanState.result.indicators.take(5).forEach { ind ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, SlateOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Text(ind.title, style = MaterialTheme.typography.titleMedium)
                            ind.remediationSteps.forEachIndexed { idx, step ->
                                Text(
                                    text = "${idx + 1}. $step",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            SectionHeader(
                title = "Daftar Periksa Mandiri (Kategori C, D, E)",
                subtitle = "Centang langkah yang sudah Anda pastikan aman"
            )

            GuidedCheckCatalog.items.forEach { item ->
                val checked = item.id in uiState.preferences.completedGuidedCheckIds
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (checked) SlateSurface else Color.White
                    ),
                    border = BorderStroke(1.dp, SlateOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("guided_item_${item.id}")
                        .clickable { onToggleGuidedCheck(item.id) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { onToggleGuidedCheck(item.id) }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = SlateSurface,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, SlateOutline)
                                ) {
                                    Text(
                                        text = "Kategori ${item.category.code} • ${item.category.label}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = item.instruction,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Konteks normal: ${item.normalContext}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (!item.actionLabel.isNullOrBlank() && !item.actionUriOrIntent.isNullOrBlank()) {
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { openSystemTarget(context, item.actionUriOrIntent) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(item.actionLabel)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoricalReportsScreen(
    history: List<ScanRecordEntity>,
    onDeleteRecord: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"))

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .widthIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            SectionHeader(
                title = "Riwayat Laporan Lokal",
                subtitle = "F08 — Disimpan terisolasi di basis data lokal perangkat"
            )

            if (history.isEmpty()) {
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
                            imageVector = Icons.Filled.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Text("Belum Ada Riwayat Pemeriksaan", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Setiap pemeriksaan yang selesai akan tercatat di sini agar Anda bisa membandingkan perubahan skor risiko antarwaktu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                history.forEachIndexed { index, record ->
                    val previous = history.getOrNull(index + 1)
                    val delta = previous?.let { record.riskScore - it.riskScore }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, SlateOutline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_card_${record.scanId}")
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
                                Column {
                                    Text(
                                        text = dateFormat.format(Date(record.timestampMillis)),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = record.verdictShortBadge,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Surface(
                                    color = SlateSurface,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, SlateOutline)
                                ) {
                                    Text(
                                        text = "Skor ${record.riskScore}/100",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = when {
                                            record.riskScore >= 50 -> StatusHighRisk
                                            record.riskScore >= 15 -> StatusReview
                                            else -> StatusClear
                                        },
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Temuan utama: ${record.topFindingTitle}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${record.indicatorCount} indikator • Cakupan ${record.coverageChecked}/${record.coverageTotal}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (delta != null) {
                                    val deltaStr = when {
                                        delta > 0 -> "+$delta dibanding sebelumnya"
                                        delta < 0 -> "$delta dibanding sebelumnya"
                                        else -> "Stabil (0 perubahan)"
                                    }
                                    Text(
                                        text = "• $deltaStr",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "Laporan Keamanan JagaPerangkat")
                                            putExtra(Intent.EXTRA_TEXT, record.shareableReportText)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(shareIntent, "Bagikan Laporan Lokal")
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Ekspor Teks")
                                }

                                IconButton(
                                    onClick = { onDeleteRecord(record.scanId) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Hapus laporan ini",
                                        tint = StatusHighRisk
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsPrivacyScreen(
    uiState: SecurityUiState,
    onToggleAppScan: (Boolean) -> Unit,
    onToggleNetworkScan: (Boolean) -> Unit,
    onClearAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Hapus Seluruh Data Lokal?") },
            text = {
                Text("Tindakan ini akan menghapus permanen seluruh riwayat laporan dan daftar periksa mandiri di perangkat Anda, serta mereset persetujuan privasi.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmClear = false
                        onClearAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusHighRisk)
                ) {
                    Text("Hapus Permanen")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .widthIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            SectionHeader(
                title = "Pengaturan & Kontrol Privasi",
                subtitle = "Kepatuhan prinsip minimisasi data dan hak penghapusan (UU PDP 27/2022)"
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SlateOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Audit Metadata Aplikasi Terlihat", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Menilai sumber instalasi dan izin sensitif aplikasi berikon peluncur & layanan aktif.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.preferences.includeAppMetadataScan,
                        onCheckedChange = onToggleAppScan,
                        modifier = Modifier.testTag("switch_settings_app_scan")
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SlateOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Audit Metadata Jaringan", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Memeriksa status VPN, Proksi HTTP, Private DNS, dan Sertifikat CA pengguna.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.preferences.includeNetworkMetadataScan,
                        onCheckedChange = onToggleNetworkScan,
                        modifier = Modifier.testTag("switch_settings_network_scan")
                    )
                }
            }

            InfoBannerCard(
                title = "Transparansi Formula Penilaian Risiko",
                body = "Skor 0–100 dihitung secara deterministik dari bobot keparahan × kekuatan bukti × keandalan metode deteksi + bonus korelasi antarindikator, dinormalisasi dengan kurva diminishing returns: Score = 100 × (1 − exp(−0.085 × Total)). Bobot awal merupakan penilaian ahli dan belum dikalibrasi di dataset malware laboratorium."
            )

            InfoBannerCard(
                title = "Privasi & Pengelolaan Data (UU No. 27 Tahun 2022)",
                body = "JagaPerangkat tidak mengumpulkan identitas pribadi, tidak mengirim telemetri, dan menonaktifkan backup cloud Android (allowBackup=false) agar laporan keamanan tetap berada di kontrol penuh Anda."
            )

            OutlinedButton(
                onClick = { confirmClear = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusHighRisk),
                border = BorderStroke(1.dp, StatusHighRisk.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_clear_all_local_data")
            ) {
                Text("Hapus Seluruh Riwayat & Data Lokal")
            }
        }
    }
}
