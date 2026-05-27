package com.oplus.groupimaging.testing

import com.oplus.groupimaging.domain.LensClass
import com.oplus.groupimaging.ui.album.groups.AlbumGroupsUiState
import com.oplus.groupimaging.ui.album.preview.MoveConfirmUiState
import com.oplus.groupimaging.ui.album.preview.MoveProgressUiState
import com.oplus.groupimaging.ui.album.preview.RulePreviewUiState
import com.oplus.groupimaging.ui.calendar.CalendarMode
import com.oplus.groupimaging.ui.calendar.CalendarUiState
import com.oplus.groupimaging.ui.deviceprofiles.DeviceProfilesUiState
import com.oplus.groupimaging.ui.failed.FailedItemsUiState
import com.oplus.groupimaging.ui.home.HomeUiState
import com.oplus.groupimaging.ui.insight.FocalRangeUi
import com.oplus.groupimaging.ui.insight.InsightFiltersUi
import com.oplus.groupimaging.ui.insight.InsightUiState
import com.oplus.groupimaging.ui.scan.onboarding.ScanOnboardingUiState
import com.oplus.groupimaging.ui.scan.progress.ScanProgressUiState
import com.oplus.groupimaging.ui.scan.progress.ScanStage
import com.oplus.groupimaging.ui.settings.DirectoryManagerUiState
import com.oplus.groupimaging.ui.settings.ScopeExplanationUiState
import com.oplus.groupimaging.ui.settings.SettingsUiState
import java.time.YearMonth

object UiStateFixtures {
    fun homeIndexed(): HomeUiState {
        val latestScan = InsightTestFixtures.completedScanJob()
        return HomeUiState(
            isLoading = false,
            hasIndex = true,
            summary = InsightTestFixtures.indexedHomeSummary(latestScan),
            scanStatus = latestScan,
            error = null,
        )
    }

    fun homeEmpty(): HomeUiState = HomeUiState(
        isLoading = false,
        hasIndex = false,
        summary = InsightTestFixtures.emptyHomeSummary(),
        scanStatus = null,
        error = null,
    )

    fun calendar(): CalendarUiState = CalendarUiState(
        isLoading = false,
        mode = CalendarMode.MONTH,
        selectedDate = null,
        monthData = InsightTestFixtures.calendarBuckets(),
        visibleBuckets = listOf(InsightTestFixtures.bucket("2026-03", 89)),
        daySummary = InsightTestFixtures.bucket("2026-03", 89),
    )

    fun insight(): InsightUiState = InsightUiState(
        isLoading = false,
        filters = InsightFiltersUi(
            lenses = setOf(LensClass.TELE),
            focalRanges = setOf(FocalRangeUi("71mm+")),
            months = setOf(YearMonth.of(2026, 3)),
        ),
        resultCount = 12_004,
        lensStats = InsightTestFixtures.insightResults().getValue(com.oplus.groupimaging.domain.InsightDimension.LENS),
        focalStats = InsightTestFixtures.insightResults().getValue(com.oplus.groupimaging.domain.InsightDimension.FOCAL_EQ),
        liveStats = InsightTestFixtures.insightResults().getValue(com.oplus.groupimaging.domain.InsightDimension.LIVE),
        rawStats = InsightTestFixtures.insightResults().getValue(com.oplus.groupimaging.domain.InsightDimension.RAW),
        deviceStats = InsightTestFixtures.insightResults().getValue(com.oplus.groupimaging.domain.InsightDimension.DEVICE),
        monthlyTrend = InsightTestFixtures.insightResults().getValue(com.oplus.groupimaging.domain.InsightDimension.MONTH),
        yearlyTrend = InsightTestFixtures.insightResults().getValue(com.oplus.groupimaging.domain.InsightDimension.YEAR),
    )

    fun albumGroups(): AlbumGroupsUiState = AlbumGroupsUiState(
        isLoading = false,
        rules = InsightTestFixtures.ruleGroups(),
    )

    fun settings(): SettingsUiState = SettingsUiState(
        isLoading = false,
        parserVersion = "v3",
        latestScan = InsightTestFixtures.completedScanJob(),
        deviceProfileCount = InsightTestFixtures.deviceProfiles().size,
        extraDirectories = listOf("DCIM/WeChat/", "Pictures/Screenshots/"),
    )

    fun scanOnboarding(): ScanOnboardingUiState = ScanOnboardingUiState(
        selectedDirectories = listOf("DCIM/Camera", "DCIM/OPPO", "Pictures/OPPO", "DCIM/WeChat/"),
    )

    fun scanProgressDone(): ScanProgressUiState = ScanProgressUiState(
        stage = ScanStage.DONE,
        progress = 1f,
        scannedCount = 49_509,
        totalCount = 49_509,
        currentFileName = "扫描完成",
        successCount = 49_509,
        failedCount = 0,
        isCompleted = true,
    )

    fun rulePreview(): RulePreviewUiState = RulePreviewUiState(
        isLoading = false,
        ruleName = "主摄",
        matchedCount = 3_384,
        assets = InsightTestFixtures.assetPreviews(),
        latestPreview = InsightTestFixtures.movePreview(),
    )

    fun moveConfirm(): MoveConfirmUiState = MoveConfirmUiState(
        targetFolderName = "/storage/emulated/0/DCIM/myalbums/主摄/",
        totalFiles = 3_384,
        includesRawPairs = false,
        conflictCount = 0,
        warningText = "移动会修改原文件在系统媒体库中的位置。继续前请确认目标目录、RAW 伴随项和冲突改名数量。",
    )

    fun moveProgress(): MoveProgressUiState = MoveProgressUiState(
        progress = 1f,
        movedCount = 3_384,
        failedCount = 0,
        totalCount = 3_384,
        currentFileName = null,
        isCompleted = true,
    )

    fun deviceProfiles(): DeviceProfilesUiState = DeviceProfilesUiState(
        isLoading = false,
        items = InsightTestFixtures.deviceProfiles(),
    )

    fun failedItems(): FailedItemsUiState = FailedItemsUiState(
        isLoading = false,
        items = InsightTestFixtures.failedItems(),
    )

    fun directoryManager(): DirectoryManagerUiState = DirectoryManagerUiState(
        isLoading = false,
        pendingDirectory = "",
        extraDirectories = listOf("DCIM/WeChat/", "Pictures/Screenshots/"),
    )

    fun scopeExplanation(): ScopeExplanationUiState = ScopeExplanationUiState()
}
