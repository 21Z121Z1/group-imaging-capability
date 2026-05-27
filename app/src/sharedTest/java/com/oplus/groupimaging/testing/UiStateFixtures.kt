package com.oplus.groupimaging.testing

import com.oplus.groupimaging.ui.album.groups.AlbumGroupsUiState
import com.oplus.groupimaging.ui.album.preview.MoveConfirmUiState
import com.oplus.groupimaging.ui.album.preview.MoveProgressUiState
import com.oplus.groupimaging.ui.album.preview.RulePreviewUiState
import com.oplus.groupimaging.ui.scan.onboarding.ScanOnboardingUiState
import com.oplus.groupimaging.ui.scan.progress.ScanProgressUiState
import com.oplus.groupimaging.ui.scan.progress.ScanStage
import com.oplus.groupimaging.ui.settings.DirectoryManagerUiState
import com.oplus.groupimaging.ui.settings.SettingsUiState

object UiStateFixtures {
    fun albumGroups(): AlbumGroupsUiState = AlbumGroupsUiState(
        isLoading = false,
        rules = InsightTestFixtures.ruleGroups(),
    )

    fun settings(): SettingsUiState = SettingsUiState(
        isLoading = false,
        latestScan = InsightTestFixtures.completedScanJob(),
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

    fun directoryManager(): DirectoryManagerUiState = DirectoryManagerUiState(
        isLoading = false,
        pendingDirectory = "",
        extraDirectories = listOf("DCIM/WeChat/", "Pictures/Screenshots/"),
    )
}
