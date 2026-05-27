package com.oplus.groupimaging.domain

import android.content.IntentSender
import java.time.LocalDate

enum class LensClass {
    MAIN,
    ULTRA_WIDE,
    TELE,
    UNKNOWN,
}

enum class ParseStatus {
    PENDING,
    PARSED,
    PARTIAL,
    SKIPPED,
    FAILED,
}

enum class CapturePairStatus {
    NONE,
    PRIMARY,
    RAW_COMPANION,
    UNPAIRED_RAW,
}

enum class MediaFormat {
    JPEG,
    HEIC,
    DNG,
    OTHER,
}

enum class RuleActionType {
    MOVE,
}

enum class MoveCandidateRole {
    PRIMARY,
    RAW_COMPANION,
}

enum class MoveCandidateStatus {
    PENDING,
    MOVED,
    FAILED,
    SKIPPED,
}

enum class ScanType {
    FULL,
    INCREMENTAL,
}

enum class ScanStatus {
    IDLE,
    RUNNING,
    COMPLETED,
    FAILED,
}

enum class InsightDimension {
    LENS,
    FOCAL_EQ,
    LIVE,
    RAW,
    DEVICE,
    MONTH,
    YEAR,
    MODE,
}

data class MediaAsset(
    val assetId: String,
    val mediaStoreId: Long,
    val path: String?,
    val relativePath: String?,
    val uri: String,
    val fileName: String,
    val mimeType: String,
    val size: Long,
    val createdAt: Long,
    val modifiedAt: Long,
    val isOplusOriginal: Boolean,
    val isLivePhoto: Boolean,
    val isRaw: Boolean,
    val pairedCaptureId: String?,
    val deviceModel: String?,
    val focalLength: Double?,
    val focalLengthEq: Int?,
    val lensClass: LensClass,
    val captureModeLabel: String?,
    val userCommentRaw: String?,
    val userCommentDigest: String?,
    val parseStatus: ParseStatus,
    val parseVersion: Int,
    val sourceConfidence: Double,
    val capturePairStatus: CapturePairStatus,
    val contentSignature: String,
)

data class CaptureSession(
    val captureId: String,
    val primaryAssetId: String,
    val pairedRawAssetId: String?,
    val captureTime: Long,
    val deviceModel: String?,
    val lensClass: LensClass,
    val focalLengthEq: Int?,
    val isLivePhoto: Boolean,
    val isRawCapture: Boolean,
    val captureModeLabel: String?,
)

data class FocalEqRange(
    val label: String,
    val minInclusive: Int,
    val maxInclusive: Int,
    val lensClass: LensClass,
)

data class DeviceProfile(
    val deviceModel: String,
    val aliases: List<String>,
    val focalEqRanges: List<FocalEqRange>,
    val cameraIdMap: Map<Int, LensClass>,
    val profileVersion: Int,
)

data class SmartRule(
    val ruleId: String,
    val ruleName: String,
    val conditions: Map<String, List<String>>,
    val targetFolder: String,
    val actionType: RuleActionType,
    val enabled: Boolean,
)

data class ScanJob(
    val jobId: String,
    val type: ScanType,
    val status: ScanStatus,
    val totalCount: Int,
    val scannedCount: Int,
    val etaSeconds: Long?,
    val startedAt: Long,
    val finishedAt: Long?,
    val parseVersion: Int,
    val failureCount: Int,
)

data class FilterSpec(
    val deviceModels: Set<String> = emptySet(),
    val lensClasses: Set<LensClass> = emptySet(),
    val liveOnly: Boolean? = null,
    val rawOnly: Boolean? = null,
    val years: Set<Int> = emptySet(),
    val dates: Set<LocalDate> = emptySet(),
    val yearMonths: Set<String> = emptySet(),
    val focalEqRangeLabels: Set<String> = emptySet(),
    val captureModes: Set<String> = emptySet(),
)

data class HomeSummary(
    val totalCaptures: Int,
    val averageDailyCaptures: Double,
    val liveRatio: Double,
    val rawRatio: Double,
    val lensBreakdown: List<InsightBucket>,
    val focalBreakdown: List<InsightBucket>,
    val deviceBreakdown: List<InsightBucket>,
    val monthlyTrend: List<InsightBucket>,
    val yearlyTrend: List<InsightBucket>,
    val latestScanJob: ScanJob?,
)

data class CalendarBucket(
    val label: String,
    val count: Int,
)

data class InsightBucket(
    val label: String,
    val count: Int,
)

data class RuleGroupSummary(
    val label: String,
    val count: Int,
    val filters: FilterSpec,
)

data class MoveCandidate(
    val assetId: String,
    val sourcePath: String?,
    val sourceUri: String,
    val fileName: String,
    val destinationFileName: String,
    val destinationRelativePath: String,
    val destinationPath: String,
    val conflict: Boolean,
    val role: MoveCandidateRole = MoveCandidateRole.PRIMARY,
    val status: MoveCandidateStatus = MoveCandidateStatus.PENDING,
    val failureReason: String? = null,
    val captureDate: LocalDate? = null,
    val deviceModel: String? = null,
    val focalLengthEq: Int? = null,
    val captureModeLabel: String? = null,
    val isLivePhoto: Boolean = false,
    val isRaw: Boolean = false,
)

data class MovePreview(
    val token: String,
    val ruleName: String,
    val targetFolder: String,
    val count: Int,
    val conflicts: Int,
    val candidates: List<MoveCandidate>,
)

data class MoveResult(
    val token: String,
    val movedCount: Int,
    val failureCount: Int,
    val failures: List<String>,
)

sealed interface MoveExecutionOutcome {
    data class Completed(val result: MoveResult) : MoveExecutionOutcome
    data class RequiresWriteAccess(val intentSender: IntentSender) : MoveExecutionOutcome
}

data class AssetPreview(
    val assetId: String,
    val assetUri: String,
    val fileName: String,
    val captureDate: LocalDate?,
    val deviceModel: String?,
    val focalLengthEq: Int?,
    val captureModeLabel: String?,
    val isLivePhoto: Boolean,
    val isRaw: Boolean,
)

data class FailedItem(
    val title: String,
    val detail: String,
    val createdAt: Long,
)
