package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.AssessmentVerdict
import com.example.domain.model.IndicatorSeverity
import com.example.domain.model.SecurityIndicator
import com.example.ui.theme.SlateOutline
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.StatusClear
import com.example.ui.theme.StatusClearContainer
import com.example.ui.theme.StatusHighRisk
import com.example.ui.theme.StatusHighRiskContainer
import com.example.ui.theme.StatusInconclusive
import com.example.ui.theme.StatusInconclusiveContainer
import com.example.ui.theme.StatusReview
import com.example.ui.theme.StatusReviewContainer

data class StatusColors(
    val content: Color,
    val container: Color
)

fun verdictStatusColors(verdict: AssessmentVerdict): StatusColors = when (verdict) {
    AssessmentVerdict.NO_SIGNIFICANT_INDICATORS ->
        StatusColors(StatusClear, StatusClearContainer)
    AssessmentVerdict.REVIEW_RECOMMENDED ->
        StatusColors(StatusReview, StatusReviewContainer)
    AssessmentVerdict.FURTHER_INVESTIGATION_NEEDED ->
        StatusColors(StatusReview, StatusReviewContainer)
    AssessmentVerdict.STRONG_INDICATORS_FORENSIC_NEEDED ->
        StatusColors(StatusHighRisk, StatusHighRiskContainer)
    AssessmentVerdict.INCONCLUSIVE_LIMITED_COVERAGE ->
        StatusColors(StatusInconclusive, StatusInconclusiveContainer)
}

fun severityStatusColors(severity: IndicatorSeverity): StatusColors = when (severity) {
    IndicatorSeverity.LOW -> StatusColors(StatusInconclusive, StatusInconclusiveContainer)
    IndicatorSeverity.MEDIUM -> StatusColors(StatusReview, StatusReviewContainer)
    IndicatorSeverity.HIGH, IndicatorSeverity.CRITICAL -> StatusColors(StatusHighRisk, StatusHighRiskContainer)
}

@Composable
fun VerdictBadge(
    verdict: AssessmentVerdict,
    modifier: Modifier = Modifier
) {
    val colors = verdictStatusColors(verdict)
    Surface(
        color = colors.container,
        shape = RoundedCornerShape(999.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = when (verdict) {
                    AssessmentVerdict.NO_SIGNIFICANT_INDICATORS -> Icons.Filled.VerifiedUser
                    AssessmentVerdict.STRONG_INDICATORS_FORENSIC_NEEDED -> Icons.Filled.Warning
                    else -> Icons.Filled.Info
                },
                contentDescription = null,
                tint = colors.content,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = verdict.shortBadge,
                style = MaterialTheme.typography.labelLarge,
                color = colors.content
            )
        }
    }
}

@Composable
fun RiskScoreGaugeCard(
    riskScore: Int,
    verdict: AssessmentVerdict,
    confidencePercent: Int,
    coverageChecked: Int,
    coverageTotal: Int,
    modifier: Modifier = Modifier
) {
    val colors = verdictStatusColors(verdict)
    val animatedProgress by animateFloatAsState(
        targetValue = (riskScore.coerceIn(0, 100)) / 100f,
        animationSpec = tween(durationMillis = 700),
        label = "risk_gauge_progress"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SlateOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(96.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 10.dp.toPx()
                        drawArc(
                            color = SlateOutline,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = colors.content,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress.coerceAtLeast(0.03f),
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$riskScore",
                            style = MaterialTheme.typography.headlineMedium,
                            color = colors.content,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "/ 100",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    VerdictBadge(verdict = verdict)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = verdict.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = verdict.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MetricPill(
                    label = "Keyakinan",
                    value = "$confidencePercent%",
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "Cakupan Modul",
                    value = "$coverageChecked / $coverageTotal",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SlateSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SlateOutline),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun StalkerwareSafetyBanner(modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StatusReviewContainer.copy(alpha = 0.55f)),
        border = BorderStroke(1.dp, StatusReview.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.HealthAndSafety,
                contentDescription = "Peringatan keselamatan korban stalkerware",
                tint = StatusReview,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Pertimbangan Keselamatan Sebelum Menghapus Aplikasi",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Jika Anda menduga perangkat dipantau oleh orang dekat (stalkerware), jangan langsung mencopot aplikasi secara mendadak sebelum menyiapkan rencana keselamatan pribadi. Penghapusan tiba-tiba dapat memicu peringatan ke pelaku.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SecurityIndicatorCard(
    indicator: SecurityIndicator,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable(indicator.id) { mutableStateOf(false) }
    val context = LocalContext.current
    val sevColors = severityStatusColors(indicator.severity)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SlateOutline),
        modifier = modifier
            .fillMaxWidth()
            .testTag("indicator_card_${indicator.id}")
    ) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = sevColors.container,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = indicator.severity.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = sevColors.content,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        color = SlateSurface,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, SlateOutline)
                    ) {
                        Text(
                            text = "Kat. ${indicator.category.code}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Tutup rincian" else "Lihat rincian",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = indicator.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Kekuatan bukti: ${indicator.evidenceStrength.label} • ${indicator.reliability.label}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Surface(
                        color = SlateSurface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Bukti yang Teramati",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = indicator.observedEvidence,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Kemungkinan Penyebab Normal",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = indicator.normalExplanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Catatan Keyakinan",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = indicator.confidenceNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Langkah Tindak Lanjut Aman",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    indicator.remediationSteps.forEachIndexed { idx, step ->
                        Text(
                            text = "${idx + 1}. $step",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }

                    if (indicator.requiresStalkerwareSafetyCare) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Catatan keselamatan: Jika Anda menduga pengawasan oleh orang dekat, tinjau rencana keselamatan sebelum melakukan penghapusan langsung.",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusReview,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    indicator.settingsIntentAction?.let { action ->
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                openSystemTarget(
                                    context = context,
                                    actionOrUrl = action,
                                    packageName = indicator.relatedPackageName
                                )
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Buka Setelan Android Terkait")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InfoBannerCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, SlateOutline),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(14.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun openSystemTarget(
    context: Context,
    actionOrUrl: String,
    packageName: String? = null
) {
    runCatching {
        val intent = when {
            actionOrUrl.startsWith("https://") -> {
                Intent(Intent.ACTION_VIEW, Uri.parse(actionOrUrl))
            }
            actionOrUrl == Settings.ACTION_APPLICATION_DETAILS_SETTINGS && !packageName.isNullOrBlank() -> {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
            }
            else -> Intent(actionOrUrl)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }.onFailure {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure {
            Toast.makeText(
                context,
                "Tidak dapat membuka menu sistem tersebut pada perangkat ini.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
