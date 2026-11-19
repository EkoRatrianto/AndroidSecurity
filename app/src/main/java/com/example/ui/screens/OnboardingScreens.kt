package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.PrivacyPreferences
import com.example.ui.theme.SlateOutline
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.StatusClear

@Composable
fun WelcomeDisclosureScreen(
    onContinueToConsent: () -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 4.dp) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = onContinueToConsent,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_welcome_continue")
                    ) {
                        Text(
                            text = "Lanjutkan ke Persetujuan Privasi",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_security_hero_1795069709321),
                    contentDescription = "Ilustrasi perlindungan privasi dan keamanan perangkat JagaPerangkat",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                        .clip(RoundedCornerShape(20.dp))
                )

                Text(
                    text = "JagaPerangkat",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Pemeriksaan keamanan & indikator pengawasan yang jujur, tenang, dan 100% lokal.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                DisclosureRow(
                    icon = Icons.Filled.Lock,
                    title = "Diproses Sepenuhnya di Perangkat Anda",
                    body = "Riwayat hasil audit disimpan secara lokal. Tidak ada daftar aplikasi, log jaringan, atau laporan yang dikirim otomatis ke server."
                )

                DisclosureRow(
                    icon = Icons.Filled.CheckCircle,
                    title = "Yang Dapat Diperiksa Secara Langsung",
                    body = "Kunci layar, patch OS, USB Debugging, layanan Aksesibilitas aktif, Admin Perangkat, Pendengar Notifikasi, Sertifikat CA pengguna, Proksi/VPN aktif, serta izin aplikasi terlihat."
                )

                DisclosureRow(
                    icon = Icons.Filled.VisibilityOff,
                    title = "Batasan Penting (Tanpa Janji Palsu)",
                    body = "Aplikasi Android biasa tanpa root tidak dapat memastikan ada tidaknya penyadapan operator seluler, eksploit OS/baseband canggih, atau aplikasi tersembunyi di luar visibilitas sistem."
                )
            }
        }
    }
}

@Composable
fun ConsentPermissionScreen(
    preferences: PrivacyPreferences,
    onToggleAppScan: (Boolean) -> Unit,
    onToggleNetworkScan: (Boolean) -> Unit,
    onBack: () -> Unit,
    onAcceptAndStart: () -> Unit
) {
    BackHandler { onBack() }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 4.dp) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = onAcceptAndStart,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_accept_consent")
                    ) {
                        Text(
                            text = "Setujui & Mulai Pemeriksaan Pertama",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke layar sambutan"
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Persetujuan & Penjelasan Izin",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = BorderStroke(1.dp, SlateOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PrivacyTip,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Prinsip Hak Akses Minimal",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "JagaPerangkat tidak meminta izin kamera, mikrofon, lokasi, kontak, SMS, maupun penyimpanan file. Hanya izin jaringan dasar (ACCESS_NETWORK_STATE) dan kueri aplikasi terlingkup (<queries>) yang digunakan secara lokal.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                ConsentToggleCard(
                    title = "Audit Metadata Aplikasi Terlihat",
                    description = "Memeriksa sumber instalasi (Play Store vs Sideload) dan izin sensitif pada aplikasi berikon peluncur serta layanan khusus aktif.",
                    checked = preferences.includeAppMetadataScan,
                    onCheckedChange = onToggleAppScan,
                    testTag = "switch_consent_app_scan"
                )

                ConsentToggleCard(
                    title = "Audit Metadata Jaringan & Sertifikat",
                    description = "Memeriksa status VPN aktif, Proksi HTTP, Private DNS, dan Sertifikat CA buatan pengguna tanpa menyadap isi lalu lintas Anda.",
                    checked = preferences.includeNetworkMetadataScan,
                    onCheckedChange = onToggleNetworkScan,
                    testTag = "switch_consent_network_scan"
                )

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SlateOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Dengan menekan tombol di bawah, Anda menyatakan sebagai pemilik atau pihak berwenang atas perangkat ini dan menyetujui pemrosesan audit lokal sesuai UU PDP No. 27 Tahun 2022.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DisclosureRow(
    icon: ImageVector,
    title: String,
    body: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SlateOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = StatusClear,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConsentToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}
