package com.oplus.groupimaging.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_assets",
    primaryKeys = ["generationId", "assetId"],
    indices = [
        Index(value = ["generationId"]),
        Index(value = ["generationId", "mediaStoreId"]),
        Index(value = ["generationId", "captureModeLabel"]),
        Index(value = ["generationId", "deviceModel"]),
        Index(value = ["generationId", "focalLengthEq"]),
        Index(value = ["generationId", "lensClass"]),
        Index(value = ["generationId", "createdAt"]),
        Index(value = ["generationId", "modifiedAt"]),
    ],
)
data class MediaAssetEntity(
    val generationId: Long,
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
    val lensClass: String,
    val captureModeLabel: String?,
    val userCommentRaw: String?,
    val userCommentDigest: String?,
    val parseStatus: String,
    val parseVersion: Int,
    val sourceConfidence: Double,
    val capturePairStatus: String,
    val contentSignature: String,
)

@Entity(tableName = "scan_cursors")
data class ScanCursorEntity(
    @PrimaryKey val scopeHash: String,
    val rootsJson: String,
    val parseVersion: Int,
    val lastSuccessfulModifiedAt: Long?,
    val lastSuccessfulMediaId: Long?,
    val lastScannedModifiedAt: Long?,
    val lastScannedMediaId: Long?,
    val resumeModifiedAt: Long?,
    val resumeMediaId: Long?,
    val activeJobId: String?,
    val updatedAt: Long,
)

@Entity(
    tableName = "capture_sessions",
    primaryKeys = ["generationId", "captureId"],
    indices = [
        Index(value = ["generationId"]),
        Index(value = ["generationId", "captureTime"]),
        Index(value = ["generationId", "lensClass"]),
        Index(value = ["generationId", "deviceModel"]),
        Index(value = ["generationId", "focalLengthEq"]),
        Index(value = ["generationId", "captureModeLabel"]),
    ],
)
data class CaptureSessionEntity(
    val generationId: Long,
    val captureId: String,
    val primaryAssetId: String,
    val pairedRawAssetId: String?,
    val captureTime: Long,
    val deviceModel: String?,
    val lensClass: String,
    val focalLengthEq: Int?,
    val isLivePhoto: Boolean,
    val isRawCapture: Boolean,
    val captureModeLabel: String?,
)

@Entity(tableName = "device_profiles")
data class DeviceProfileEntity(
    @PrimaryKey val deviceModel: String,
    val aliasesJson: String,
    val focalEqRangesJson: String,
    val cameraIdMapJson: String,
    val profileVersion: Int,
)

@Entity(tableName = "smart_rules")
data class SmartRuleEntity(
    @PrimaryKey val ruleId: String,
    val ruleName: String,
    val conditionsJson: String,
    val targetFolder: String,
    val actionType: String,
    val enabled: Boolean,
)

@Entity(tableName = "scan_jobs")
data class ScanJobEntity(
    @PrimaryKey val jobId: String,
    val type: String,
    val status: String,
    val totalCount: Int,
    val scannedCount: Int,
    val etaSeconds: Long?,
    val startedAt: Long,
    val finishedAt: Long?,
    val parseVersion: Int,
    val failureCount: Int,
)

@Entity(tableName = "scan_directories")
data class ScanDirectoryEntity(
    @PrimaryKey val relativePath: String,
    val updatedAt: Long,
)

@Entity(tableName = "index_generations")
data class IndexGenerationEntity(
    @PrimaryKey val generationKey: String,
    val activeGenerationId: Long,
    val updatedAt: Long,
)

@Entity(tableName = "move_plans")
data class MovePlanEntity(
    @PrimaryKey val token: String,
    val ruleName: String,
    val targetFolder: String,
    val targetRelativePath: String,
    val createdAt: Long,
    val status: String,
    val completedAt: Long?,
)

@Entity(
    tableName = "move_candidates",
    primaryKeys = ["planToken", "assetId", "role"],
    indices = [
        Index(value = ["planToken"]),
        Index(value = ["status"]),
    ],
)
data class MoveCandidateEntity(
    val planToken: String,
    val assetId: String,
    val sourcePath: String?,
    val sourceUri: String,
    val fileName: String,
    val destinationFileName: String,
    val destinationRelativePath: String,
    val destinationPath: String,
    val conflict: Boolean,
    val role: String,
    val status: String,
    val failureReason: String?,
    val captureDate: String?,
    val deviceModel: String?,
    val focalLengthEq: Int?,
    val captureModeLabel: String?,
    val isLivePhoto: Boolean,
    val isRaw: Boolean,
    val updatedAt: Long,
)

@Entity(tableName = "archive_audits")
data class ArchiveAuditEntity(
    @PrimaryKey val auditId: String,
    val previewToken: String,
    val createdAt: Long,
    val movedCount: Int,
    val failureCount: Int,
    val failureJson: String,
)
