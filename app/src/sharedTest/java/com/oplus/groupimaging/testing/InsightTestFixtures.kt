package com.oplus.groupimaging.testing

import com.oplus.groupimaging.domain.AssetPreview
import com.oplus.groupimaging.domain.DeviceProfile
import com.oplus.groupimaging.domain.FailedItem
import com.oplus.groupimaging.domain.FilterSpec
import com.oplus.groupimaging.domain.FocalEqRange
import com.oplus.groupimaging.domain.HomeSummary
import com.oplus.groupimaging.domain.InsightBucket
import com.oplus.groupimaging.domain.InsightDimension
import com.oplus.groupimaging.domain.LensClass
import com.oplus.groupimaging.domain.MoveCandidate
import com.oplus.groupimaging.domain.MovePreview
import com.oplus.groupimaging.domain.MoveResult
import com.oplus.groupimaging.domain.RuleGroupSummary
import com.oplus.groupimaging.domain.ScanJob
import com.oplus.groupimaging.domain.ScanStatus
import com.oplus.groupimaging.domain.ScanType
import java.time.LocalDate

object InsightTestFixtures {
    fun bucket(label: String, count: Int): InsightBucket = InsightBucket(label = label, count = count)

    fun completedScanJob(
        scanType: ScanType = ScanType.FULL,
        totalCount: Int = 49_509,
        scannedCount: Int = totalCount,
        failureCount: Int = 0,
        parseVersion: Int = 3,
    ): ScanJob = ScanJob(
        jobId = "scan-job-$scanType",
        type = scanType,
        status = ScanStatus.COMPLETED,
        totalCount = totalCount,
        scannedCount = scannedCount,
        etaSeconds = null,
        startedAt = 1_710_000_000_000,
        finishedAt = 1_710_000_030_000,
        parseVersion = parseVersion,
        failureCount = failureCount,
    )

    fun runningScanJob(
        scanType: ScanType = ScanType.FULL,
        totalCount: Int = 49_509,
        scannedCount: Int = 650,
        failureCount: Int = 0,
        parseVersion: Int = 3,
    ): ScanJob = ScanJob(
        jobId = "scan-job-$scanType",
        type = scanType,
        status = ScanStatus.RUNNING,
        totalCount = totalCount,
        scannedCount = scannedCount,
        etaSeconds = 42,
        startedAt = 1_710_000_000_000,
        finishedAt = null,
        parseVersion = parseVersion,
        failureCount = failureCount,
    )

    fun emptyHomeSummary(latestScanJob: ScanJob? = null): HomeSummary = HomeSummary(
        totalCaptures = 0,
        averageDailyCaptures = 0.0,
        liveRatio = 0.0,
        rawRatio = 0.0,
        lensBreakdown = emptyList(),
        focalBreakdown = emptyList(),
        deviceBreakdown = emptyList(),
        monthlyTrend = emptyList(),
        yearlyTrend = emptyList(),
        latestScanJob = latestScanJob,
    )

    fun indexedHomeSummary(
        latestScanJob: ScanJob = completedScanJob(),
    ): HomeSummary = HomeSummary(
        totalCaptures = 12_004,
        averageDailyCaptures = 32.0,
        liveRatio = 9.2,
        rawRatio = 0.0,
        lensBreakdown = listOf(
            bucket(LensClass.MAIN.name, 3_384),
            bucket(LensClass.ULTRA_WIDE.name, 768),
            bucket(LensClass.TELE.name, 7_852),
        ),
        focalBreakdown = listOf(
            bucket("71mm+", 4_709),
            bucket("35-70mm", 3_911),
            bucket("19-34mm", 3_384),
        ),
        deviceBreakdown = listOf(
            bucket("OPPO Find X8 Ultra", 8_544),
            bucket("OPPO Find X7 Ultra", 2_290),
            bucket("OPPO Find X6 Pro", 1_088),
        ),
        monthlyTrend = listOf(
            bucket("2026-01", 2_800),
            bucket("2026-02", 4_200),
            bucket("2026-03", 5_004),
        ),
        yearlyTrend = listOf(
            bucket("2024", 1_200),
            bucket("2025", 4_800),
            bucket("2026", 6_004),
        ),
        latestScanJob = latestScanJob,
    )

    fun calendarBuckets(): List<InsightBucket> = listOf(
        bucket("2026-03-21", 12),
        bucket("2026-03-22", 28),
        bucket("2026-03-23", 31),
        bucket("2026-03-24", 18),
    )

    fun calendarBucketsSpanningYears(): List<InsightBucket> = listOf(
        bucket("2025-12-31", 4),
        bucket("2026-03-21", 12),
        bucket("2026-03-22", 28),
        bucket("2026-04-01", 6),
    )

    fun insightResults(): Map<InsightDimension, List<InsightBucket>> = mapOf(
        InsightDimension.LENS to listOf(
            bucket("TELE", 7_852),
            bucket("MAIN", 3_384),
            bucket("ULTRA_WIDE", 768),
        ),
        InsightDimension.FOCAL_EQ to listOf(
            bucket("71mm+", 4_709),
            bucket("35-70mm", 3_911),
            bucket("19-34mm", 3_384),
        ),
        InsightDimension.LIVE to listOf(
            bucket("Live", 1_101),
            bucket("Static", 10_903),
        ),
        InsightDimension.RAW to listOf(
            bucket("RAW", 0),
            bucket("非 RAW", 12_004),
        ),
        InsightDimension.DEVICE to listOf(
            bucket("OPPO Find X8 Ultra", 8_544),
            bucket("OPPO Find X7 Ultra", 2_290),
            bucket("OPPO Find X6 Pro", 1_088),
        ),
        InsightDimension.MONTH to indexedHomeSummary().monthlyTrend,
        InsightDimension.YEAR to indexedHomeSummary().yearlyTrend,
        InsightDimension.MODE to listOf(
            bucket("大师", 4_972),
            bucket("普通", 7_032),
        ),
    )

    fun ruleGroups(): List<RuleGroupSummary> = listOf(
        RuleGroupSummary("主摄", 3_384, FilterSpec(lensClasses = setOf(LensClass.MAIN))),
        RuleGroupSummary("超广角", 768, FilterSpec(lensClasses = setOf(LensClass.ULTRA_WIDE))),
        RuleGroupSummary("长焦", 7_852, FilterSpec(lensClasses = setOf(LensClass.TELE))),
        RuleGroupSummary("Live Photo", 1_101, FilterSpec(liveOnly = true)),
        RuleGroupSummary("RAW", 0, FilterSpec(rawOnly = true)),
        RuleGroupSummary("大师", 4_972, FilterSpec(captureModes = setOf("大师"))),
    )

    fun assetPreviews(): List<AssetPreview> = listOf(
        assetPreview("asset-1", "IMG20260323200232.jpg"),
        assetPreview("asset-2", "IMG20260323200226.jpg"),
        assetPreview("asset-3", "IMG20260323200011.jpg"),
    )

    fun assetPreview(
        assetId: String,
        fileName: String,
        captureDate: LocalDate? = LocalDate.of(2026, 3, 23),
    ): AssetPreview = AssetPreview(
        assetId = assetId,
        assetUri = "content://$assetId",
        fileName = fileName,
        captureDate = captureDate,
        deviceModel = "OPPO Find X8 Ultra",
        focalLengthEq = 23,
        captureModeLabel = "大师",
        isLivePhoto = false,
        isRaw = false,
    )

    fun movePreview(
        count: Int = 3_384,
        conflicts: Int = 0,
        token: String = "preview-token",
        targetFolder: String = "/storage/emulated/0/DCIM/myalbums/主摄/",
    ): MovePreview = MovePreview(
        token = token,
        ruleName = "主摄",
        targetFolder = targetFolder,
        count = count,
        conflicts = conflicts,
        candidates = listOf(
            MoveCandidate(
                assetId = "asset-1",
                sourcePath = "/storage/emulated/0/DCIM/Camera/IMG20260323200232.jpg",
                sourceUri = "content://asset-1",
                fileName = "IMG20260323200232.jpg",
                destinationFileName = "IMG20260323200232.jpg",
                destinationRelativePath = "DCIM/myalbums/主摄/",
                destinationPath = "${targetFolder}IMG20260323200232.jpg",
                conflict = false,
            ),
        ),
    )

    fun moveResult(
        token: String = "preview-token",
        movedCount: Int = 3_384,
        failureCount: Int = 0,
    ): MoveResult = MoveResult(
        token = token,
        movedCount = movedCount,
        failureCount = failureCount,
        failures = emptyList(),
    )

    fun deviceProfiles(): List<DeviceProfile> = listOf(
        DeviceProfile(
            deviceModel = "OPPO Find X8 Ultra",
            aliases = listOf("PKJ110"),
            focalEqRanges = listOf(
                FocalEqRange("23mm", 19, 34, LensClass.MAIN),
                FocalEqRange("135mm", 71, 200, LensClass.TELE),
            ),
            cameraIdMap = mapOf(0 to LensClass.MAIN, 3 to LensClass.TELE),
            profileVersion = 5,
        ),
        DeviceProfile(
            deviceModel = "OPPO Find X7 Ultra",
            aliases = listOf("PHY110"),
            focalEqRanges = listOf(
                FocalEqRange("14mm", 10, 18, LensClass.ULTRA_WIDE),
                FocalEqRange("70mm", 60, 90, LensClass.TELE),
            ),
            cameraIdMap = mapOf(0 to LensClass.MAIN, 1 to LensClass.ULTRA_WIDE, 3 to LensClass.TELE),
            profileVersion = 4,
        ),
    )

    fun failedItems(): List<FailedItem> = listOf(
        FailedItem(
            title = "IMG_NO_EXIF.jpg",
            detail = "缺少必要 EXIF，已跳过分析",
            createdAt = 1_710_000_010_000,
        ),
        FailedItem(
            title = "IMG_PARSE_FAIL.heic",
            detail = "UserComment 解码失败",
            createdAt = 1_710_000_020_000,
        ),
    )
}
