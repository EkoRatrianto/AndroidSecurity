package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_records")
data class ScanRecordEntity(
    @PrimaryKey val scanId: String,
    val timestampMillis: Long,
    val riskScore: Int,
    val verdictName: String,
    val verdictShortBadge: String,
    val confidencePercent: Int,
    val coverageChecked: Int,
    val coverageTotal: Int,
    val indicatorCount: Int,
    val highSeverityCount: Int,
    val activeMicAppsCount: Int,
    val activeCameraAppsCount: Int,
    val activeLocationAppsCount: Int,
    val enabledSensorsCount: Int,
    val topFindingTitle: String,
    val shareableReportText: String
)
