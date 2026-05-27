package com.oplus.groupimaging.domain.repository

import com.oplus.groupimaging.domain.CaptureSession
import com.oplus.groupimaging.domain.DeviceProfile
import com.oplus.groupimaging.domain.FilterSpec
import com.oplus.groupimaging.domain.HomeSummary
import com.oplus.groupimaging.domain.InsightBucket
import com.oplus.groupimaging.domain.InsightDimension
import com.oplus.groupimaging.domain.MoveExecutionOutcome
import com.oplus.groupimaging.domain.MovePreview
import com.oplus.groupimaging.domain.RuleGroupSummary
import com.oplus.groupimaging.domain.ScanJob
import com.oplus.groupimaging.domain.ScanType
import com.oplus.groupimaging.domain.FailedItem
import com.oplus.groupimaging.domain.AssetPreview
import kotlinx.coroutines.flow.Flow

interface OplusInsightRepository {
    suspend fun refreshLibrary(scanType: ScanType = ScanType.FULL, extraRoots: Set<String> = emptySet())
    suspend fun scheduleScan(scanType: ScanType = ScanType.FULL, extraRoots: Set<String> = emptySet())
    suspend fun loadExtraScanDirectories(): List<String>
    suspend fun saveExtraScanDirectories(directories: List<String>): List<String>
    suspend fun loadHomeSummary(filters: FilterSpec = FilterSpec()): HomeSummary
    suspend fun loadCalendarSummary(filters: FilterSpec = FilterSpec()): List<InsightBucket>
    suspend fun loadInsightBreakdown(
        dimension: InsightDimension,
        filters: FilterSpec = FilterSpec(),
    ): List<InsightBucket>
    suspend fun loadRuleGroups(): List<RuleGroupSummary>
    suspend fun loadRulePreview(filters: FilterSpec): List<AssetPreview>
    suspend fun previewMove(ruleName: String, targetFolder: String, filters: FilterSpec): MovePreview
    suspend fun executeMove(previewToken: String, writeAccessGranted: Boolean = false): MoveExecutionOutcome
    suspend fun latestScanJob(): ScanJob?
    fun latestScanJobFlow(): Flow<ScanJob?>
    suspend fun loadFailedItems(): List<FailedItem>
    suspend fun loadDeviceProfiles(): List<DeviceProfile>
}
