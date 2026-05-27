package com.oplus.groupimaging.testing

import android.content.IntentSender
import com.oplus.groupimaging.data.normalizeExtraScanDirectories
import com.oplus.groupimaging.domain.AssetPreview
import com.oplus.groupimaging.domain.DeviceProfile
import com.oplus.groupimaging.domain.FailedItem
import com.oplus.groupimaging.domain.FilterSpec
import com.oplus.groupimaging.domain.HomeSummary
import com.oplus.groupimaging.domain.InsightBucket
import com.oplus.groupimaging.domain.InsightDimension
import com.oplus.groupimaging.domain.MoveExecutionOutcome
import com.oplus.groupimaging.domain.MovePreview
import com.oplus.groupimaging.domain.MoveResult
import com.oplus.groupimaging.domain.RuleGroupSummary
import com.oplus.groupimaging.domain.ScanJob
import com.oplus.groupimaging.domain.ScanStatus
import com.oplus.groupimaging.domain.ScanType
import com.oplus.groupimaging.domain.repository.OplusInsightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeInsightRepository : OplusInsightRepository {
    var homeSummary: HomeSummary = InsightTestFixtures.emptyHomeSummary()
    var calendarSummary: List<InsightBucket> = emptyList()
    var insightResults: Map<InsightDimension, List<InsightBucket>> = emptyMap()
    var ruleGroups: List<RuleGroupSummary> = emptyList()
    var rulePreview: List<AssetPreview> = emptyList()
    var movePreview: MovePreview = InsightTestFixtures.movePreview(count = 0)
    var moveResult: MoveResult = InsightTestFixtures.moveResult(movedCount = 0)
    var moveExecutionOutcome: MoveExecutionOutcome = MoveExecutionOutcome.Completed(moveResult)
    private val latestScanJobState = MutableStateFlow<ScanJob?>(null)
    var latestScanJobValue: ScanJob?
        get() = latestScanJobState.value
        set(value) {
            latestScanJobState.value = value
        }
    var failedItems: List<FailedItem> = emptyList()
    var deviceProfiles: List<DeviceProfile> = emptyList()
    var extraScanDirectories: List<String> = emptyList()
    var normalizeExtraDirectoriesOnSave: Boolean = false
    var refreshFailure: Throwable? = null
    var homeSummaryFailure: Throwable? = null
    var scanDirectoriesFailure: Throwable? = null
    var calendarSummaryFailure: Throwable? = null
    var ruleGroupsFailure: Throwable? = null
    var failedItemsFailure: Throwable? = null
    var deviceProfilesFailure: Throwable? = null
    var onRefreshSuccess: (FakeInsightRepository.() -> Unit)? = null

    var refreshCallCount = 0
    var lastRequestedScanType: ScanType? = null
    var lastRequestedExtraRoots: Set<String> = emptySet()
    val insightRequests = mutableListOf<Pair<InsightDimension, FilterSpec>>()
    val previewRequests = mutableListOf<Triple<String, String, FilterSpec>>()
    val executeRequests = mutableListOf<Pair<String, Boolean>>()

    fun reset() {
        homeSummary = InsightTestFixtures.emptyHomeSummary()
        calendarSummary = emptyList()
        insightResults = emptyMap()
        ruleGroups = emptyList()
        rulePreview = emptyList()
        movePreview = InsightTestFixtures.movePreview(count = 0)
        moveResult = InsightTestFixtures.moveResult(movedCount = 0)
        moveExecutionOutcome = MoveExecutionOutcome.Completed(moveResult)
        latestScanJobValue = null
        failedItems = emptyList()
        deviceProfiles = emptyList()
        extraScanDirectories = emptyList()
        normalizeExtraDirectoriesOnSave = false
        refreshFailure = null
        homeSummaryFailure = null
        scanDirectoriesFailure = null
        calendarSummaryFailure = null
        ruleGroupsFailure = null
        failedItemsFailure = null
        deviceProfilesFailure = null
        onRefreshSuccess = null
        refreshCallCount = 0
        lastRequestedScanType = null
        lastRequestedExtraRoots = emptySet()
        insightRequests.clear()
        previewRequests.clear()
        executeRequests.clear()
    }

    fun seedIndexedLibrary() {
        val latestScan = InsightTestFixtures.completedScanJob()
        homeSummary = InsightTestFixtures.indexedHomeSummary(latestScan)
        calendarSummary = InsightTestFixtures.calendarBuckets()
        insightResults = InsightTestFixtures.insightResults()
        ruleGroups = InsightTestFixtures.ruleGroups()
        rulePreview = InsightTestFixtures.assetPreviews()
        movePreview = InsightTestFixtures.movePreview()
        moveResult = InsightTestFixtures.moveResult()
        moveExecutionOutcome = MoveExecutionOutcome.Completed(moveResult)
        latestScanJobValue = latestScan
        failedItems = InsightTestFixtures.failedItems()
        deviceProfiles = InsightTestFixtures.deviceProfiles()
    }

    fun seedEmptyLibrary() {
        homeSummary = InsightTestFixtures.emptyHomeSummary()
        calendarSummary = emptyList()
        insightResults = InsightTestFixtures.insightResults().mapValues { emptyList() }
        ruleGroups = InsightTestFixtures.ruleGroups().map { it.copy(count = 0) }
        rulePreview = emptyList()
        movePreview = InsightTestFixtures.movePreview(count = 0)
        moveResult = InsightTestFixtures.moveResult(movedCount = 0)
        latestScanJobValue = null
        failedItems = emptyList()
        deviceProfiles = InsightTestFixtures.deviceProfiles()
    }

    override suspend fun refreshLibrary(scanType: ScanType, extraRoots: Set<String>) {
        refreshCallCount += 1
        lastRequestedScanType = scanType
        lastRequestedExtraRoots = extraRoots
        val startedAt = System.currentTimeMillis()
        latestScanJobValue = InsightTestFixtures.runningScanJob(scanType = scanType).copy(startedAt = startedAt)
        refreshFailure?.let {
            latestScanJobValue = latestScanJobValue?.copy(
                status = ScanStatus.FAILED,
                failureCount = 1,
                finishedAt = startedAt + 1,
            )
            throw it
        }
        onRefreshSuccess?.invoke(this)
        latestScanJobValue = if (latestScanJobValue?.status == ScanStatus.COMPLETED) {
            latestScanJobValue?.copy(
                startedAt = startedAt,
                finishedAt = startedAt + 1,
            )
        } else {
            latestScanJobValue?.copy(
                status = ScanStatus.COMPLETED,
                totalCount = 100,
                scannedCount = 100,
                finishedAt = startedAt + 1,
            )
        }
    }

    override suspend fun scheduleScan(scanType: ScanType, extraRoots: Set<String>) {
        refreshLibrary(scanType, extraRoots)
    }

    override suspend fun loadHomeSummary(filters: FilterSpec): HomeSummary {
        homeSummaryFailure?.let { throw it }
        return homeSummary
    }

    override suspend fun loadExtraScanDirectories(): List<String> {
        scanDirectoriesFailure?.let { throw it }
        return extraScanDirectories
    }

    override suspend fun saveExtraScanDirectories(directories: List<String>): List<String> {
        extraScanDirectories = if (normalizeExtraDirectoriesOnSave) {
            normalizeExtraScanDirectories(directories)
        } else {
            directories
        }
        return extraScanDirectories
    }

    override suspend fun loadCalendarSummary(filters: FilterSpec): List<InsightBucket> {
        calendarSummaryFailure?.let { throw it }
        return calendarSummary
    }

    override suspend fun loadInsightBreakdown(
        dimension: InsightDimension,
        filters: FilterSpec,
    ): List<InsightBucket> {
        insightRequests += dimension to filters
        return insightResults[dimension].orEmpty()
    }

    override suspend fun loadRuleGroups(): List<RuleGroupSummary> {
        ruleGroupsFailure?.let { throw it }
        return ruleGroups
    }

    override suspend fun loadRulePreview(filters: FilterSpec): List<AssetPreview> = rulePreview

    override suspend fun previewMove(ruleName: String, targetFolder: String, filters: FilterSpec): MovePreview {
        previewRequests += Triple(ruleName, targetFolder, filters)
        return movePreview
    }

    override suspend fun executeMove(previewToken: String, writeAccessGranted: Boolean): MoveExecutionOutcome {
        executeRequests += previewToken to writeAccessGranted
        return when (val outcome = moveExecutionOutcome) {
            is MoveExecutionOutcome.Completed -> outcome.copy(result = moveResult)
            is MoveExecutionOutcome.RequiresWriteAccess -> outcome
        }
    }

    override suspend fun latestScanJob(): ScanJob? = latestScanJobValue

    override fun latestScanJobFlow(): Flow<ScanJob?> = latestScanJobState

    override suspend fun loadFailedItems(): List<FailedItem> {
        failedItemsFailure?.let { throw it }
        return failedItems
    }

    override suspend fun loadDeviceProfiles(): List<DeviceProfile> {
        deviceProfilesFailure?.let { throw it }
        return deviceProfiles
    }

    companion object {
        fun intentSenderForTest(): IntentSender {
            val ctor = IntentSender::class.java.declaredConstructors.single()
            ctor.isAccessible = true
            return ctor.newInstance(*arrayOfNulls<Any>(ctor.parameterCount)) as IntentSender
        }
    }
}
