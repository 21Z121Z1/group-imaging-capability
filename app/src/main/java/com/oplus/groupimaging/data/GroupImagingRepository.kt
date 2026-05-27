package com.oplus.groupimaging.data

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.room.withTransaction
import com.oplus.groupimaging.core.CaptureSessionMatcher
import com.oplus.groupimaging.core.ExtractedMetadata
import com.oplus.groupimaging.core.MediaCandidate
import com.oplus.groupimaging.core.MediaScanner
import com.oplus.groupimaging.core.MetadataExtractor
import com.oplus.groupimaging.core.OplusClassifier
import com.oplus.groupimaging.core.ScanCursorPosition
import com.oplus.groupimaging.core.defaultDeviceProfiles
import com.oplus.groupimaging.core.mediaStoreImageUriForId
import com.oplus.groupimaging.data.scan.ScanScheduler
import com.oplus.groupimaging.domain.AssetPreview
import com.oplus.groupimaging.domain.CapturePairStatus
import com.oplus.groupimaging.domain.CaptureSession
import com.oplus.groupimaging.domain.DeviceProfile
import com.oplus.groupimaging.domain.FailedItem
import com.oplus.groupimaging.domain.FilterSpec
import com.oplus.groupimaging.domain.HomeSummary
import com.oplus.groupimaging.domain.InsightBucket
import com.oplus.groupimaging.domain.InsightDimension
import com.oplus.groupimaging.domain.LensClass
import com.oplus.groupimaging.domain.MediaAsset
import com.oplus.groupimaging.domain.MoveExecutionOutcome
import com.oplus.groupimaging.domain.MoveCandidate
import com.oplus.groupimaging.domain.MoveCandidateRole
import com.oplus.groupimaging.domain.MoveCandidateStatus
import com.oplus.groupimaging.domain.MovePreview
import com.oplus.groupimaging.domain.MoveResult
import com.oplus.groupimaging.domain.ParseStatus
import com.oplus.groupimaging.domain.RuleGroupSummary
import com.oplus.groupimaging.domain.ScanJob
import com.oplus.groupimaging.domain.ScanStatus
import com.oplus.groupimaging.domain.ScanType
import com.oplus.groupimaging.domain.repository.OplusInsightRepository
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import dagger.hilt.android.qualifiers.ApplicationContext

@OptIn(ExperimentalCoroutinesApi::class)
class GroupImagingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: GroupImagingDatabase,
    private val scanner: MediaScanner,
    private val metadataExtractor: MetadataExtractor,
    private val captureSessionMatcher: CaptureSessionMatcher,
    private val scanScheduler: ScanScheduler,
) : OplusInsightRepository {
    private val parseVersion = 3
    private val logTag = "GroupImagingScan"
    private val progressFlushInterval = 25
    private val metadataParallelism = 4
    private val entityUpsertBatchSize = 200
    private val metadataDispatcher = Dispatchers.IO.limitedParallelism(metadataParallelism)
    private val contentResolver get() = context.contentResolver

    override suspend fun scheduleScan(
        scanType: ScanType,
        extraRoots: Set<String>,
    ) {
        scanScheduler.enqueue(scanType, extraRoots)
    }

    override suspend fun refreshLibrary(
        scanType: ScanType,
        extraRoots: Set<String>,
    ) {
        withContext(Dispatchers.IO) {
            bootstrapProfiles()
            val scope = buildScanScope(extraRoots)
            val existingCursor = database.scanCursorDao().get(scope.scopeHash)
            val execution = determineScanExecution(scanType, scope, existingCursor)
            val jobId = UUID.randomUUID().toString()
            val startedAt = System.currentTimeMillis()
            val writeGenerationId = if (execution.effectiveType == ScanType.FULL) {
                newScanGenerationId()
            } else {
                activeGenerationId()
            }
            var durableScannedPosition = execution.startAfter
            var durableParsedPosition = existingCursor?.lastParsedPosition()
            var durableScannedCount = 0
            var durableFailureCount = 0

            if (execution.effectiveType == ScanType.FULL) {
                database.captureSessionDao().clearGeneration(writeGenerationId)
                database.mediaAssetDao().clearGeneration(writeGenerationId)
            }

            Log.i(
                logTag,
                "Scan started type=${execution.effectiveType.name} requested=${scanType.name} scope=${scope.rootsJson} reason=${execution.reason}",
            )
            upsertCursor(
                scope = scope,
                previous = existingCursor,
                activeJobId = jobId,
                lastScanned = existingCursor?.lastScannedPosition(),
                lastParsed = existingCursor?.lastParsedPosition(),
                resume = execution.startAfter,
            )
            upsertScanJob(
                jobId = jobId,
                type = execution.effectiveType,
                status = ScanStatus.RUNNING,
                totalCount = 0,
                scannedCount = 0,
                startedAt = startedAt,
                finishedAt = null,
                failureCount = 0,
            )

            runCatching {
                val profiles = loadProfiles()
                val classifier = OplusClassifier(profiles)
                val candidates = scanner.scan(extraRoots, execution.startAfter)
                val totalCount = candidates.size
                Log.i(logTag, "Enumerated $totalCount candidates")
                upsertScanJob(
                    jobId = jobId,
                    type = execution.effectiveType,
                    status = ScanStatus.RUNNING,
                    totalCount = totalCount,
                    scannedCount = durableScannedCount,
                    startedAt = startedAt,
                    finishedAt = null,
                    failureCount = durableFailureCount,
                )

                val pendingAssets = mutableListOf<MediaAssetEntity>()
                candidates.chunked(metadataParallelism).forEach { batch ->
                    val results = extractBatch(batch, classifier)
                    results.forEachIndexed { index, result ->
                        pendingAssets += result.asset.toEntity(writeGenerationId)
                        durableScannedCount += 1
                        if (result.failed) durableFailureCount += 1
                        val candidatePosition = result.candidate.cursorPosition()
                        durableScannedPosition = candidatePosition
                        if (result.asset.parseStatus == ParseStatus.PARSED) {
                            durableParsedPosition = candidatePosition
                        }
                        val shouldFlush = durableScannedCount == 1 ||
                            durableScannedCount % progressFlushInterval == 0 ||
                            durableScannedCount == totalCount
                        if (shouldFlush) {
                            upsertMediaAssets(pendingAssets)
                            pendingAssets.clear()
                            upsertCursor(
                                scope = scope,
                                previous = existingCursor,
                                activeJobId = jobId,
                                lastScanned = durableScannedPosition,
                                lastParsed = durableParsedPosition,
                                resume = durableScannedPosition,
                            )
                            upsertScanJob(
                                jobId = jobId,
                                type = execution.effectiveType,
                                status = ScanStatus.RUNNING,
                                totalCount = totalCount,
                                scannedCount = durableScannedCount,
                                startedAt = startedAt,
                                finishedAt = null,
                                failureCount = durableFailureCount,
                            )
                            Log.d(
                                logTag,
                                "Cursor checkpoint saved modifiedAt=${candidatePosition.modifiedAt} id=${candidatePosition.mediaStoreId}",
                            )
                        } else if (index == results.lastIndex && pendingAssets.isNotEmpty() && durableScannedCount == totalCount) {
                            upsertMediaAssets(pendingAssets)
                            pendingAssets.clear()
                        }
                    }
                }

                if (pendingAssets.isNotEmpty()) {
                    upsertMediaAssets(pendingAssets)
                }

                Log.i(
                    logTag,
                    "Metadata extraction finished scanned=$durableScannedCount failures=$durableFailureCount",
                )
                val allAssets = database.mediaAssetDao().getAllForGeneration(writeGenerationId).map { it.toDomain() }
                val indexableAssets = allAssets.filter { it.parseStatus == ParseStatus.PARSED }
                val (sessions, updatedAssets) = captureSessionMatcher.buildSessions(indexableAssets)
                Log.i(logTag, "Built ${sessions.size} capture sessions from ${updatedAssets.size} assets")
                val updatedAssetsById = updatedAssets.associateBy { it.assetId }
                upsertMediaAssets(
                    allAssets.map { asset -> updatedAssetsById[asset.assetId] ?: asset }.map { it.toEntity(writeGenerationId) },
                )
                database.captureSessionDao().clearGeneration(writeGenerationId)
                upsertCaptureSessions(sessions.map { it.toEntity(writeGenerationId) })

                if (execution.effectiveType == ScanType.FULL) {
                    setActiveGeneration(writeGenerationId)
                }
                val scannedCursor = durableScannedPosition ?: existingCursor?.lastScannedPosition() ?: ScanCursorPosition(0L, 0L)
                val parsedCursor = durableParsedPosition ?: existingCursor?.lastParsedPosition()
                upsertCursor(
                    scope = scope,
                    previous = existingCursor,
                    activeJobId = null,
                    lastScanned = scannedCursor,
                    lastParsed = parsedCursor,
                    resume = null,
                )
                upsertScanJob(
                    jobId = jobId,
                    type = execution.effectiveType,
                    status = ScanStatus.COMPLETED,
                    totalCount = totalCount,
                    scannedCount = durableScannedCount,
                    startedAt = startedAt,
                    finishedAt = System.currentTimeMillis(),
                    failureCount = durableFailureCount,
                )
                Log.i(
                    logTag,
                    "Scan completed total=$totalCount scanned=$durableScannedCount failures=$durableFailureCount sessions=${sessions.size}",
                )
            }.getOrElse { error ->
                Log.e(logTag, "Scan failed", error)
                upsertCursor(
                    scope = scope,
                    previous = existingCursor,
                    activeJobId = jobId,
                    lastScanned = durableScannedPosition ?: existingCursor?.lastScannedPosition(),
                    lastParsed = durableParsedPosition,
                    resume = durableScannedPosition,
                )
                upsertScanJob(
                    jobId = jobId,
                    type = execution.effectiveType,
                    status = ScanStatus.FAILED,
                    totalCount = durableScannedCount.coerceAtLeast(0),
                    scannedCount = durableScannedCount,
                    startedAt = startedAt,
                    finishedAt = System.currentTimeMillis(),
                    failureCount = durableFailureCount.coerceAtLeast(1),
                )
                throw error
            }
        }
    }

    override suspend fun loadExtraScanDirectories(): List<String> = withContext(Dispatchers.IO) {
        database.scanDirectoryDao().getAll().map { it.relativePath }
    }

    override suspend fun saveExtraScanDirectories(directories: List<String>): List<String> = withContext(Dispatchers.IO) {
        val normalized = normalizeExtraScanDirectories(directories)
        val updatedAt = System.currentTimeMillis()
        database.withTransaction {
            database.scanDirectoryDao().clearAll()
            if (normalized.isNotEmpty()) {
                database.scanDirectoryDao().upsertAll(
                    normalized.map { relativePath ->
                        ScanDirectoryEntity(
                            relativePath = relativePath,
                            updatedAt = updatedAt,
                        )
                    },
                )
            }
        }
        normalized
    }

    override suspend fun loadHomeSummary(filters: FilterSpec): HomeSummary = withContext(Dispatchers.IO) {
        val sessions = filteredSessions(filters)
        val latest = database.scanJobDao().latest()?.toDomain()
        val totalCaptures = sessions.size
        val dayBuckets = sessions.groupBy { yyyyMmDd(it.captureTime) }
        val lensBreakdown = sessions.groupCount { it.lensClass.name }
        val focalBreakdown = sessions.groupCount { bucketFocal(it.focalLengthEq) }
        val deviceBreakdown = sessions.groupCount { it.deviceModel ?: "未知机型" }
        val monthBreakdown = sessions.groupCount { yyyyMm(it.captureTime) }
        val yearBreakdown = sessions.groupCount { yyyy(it.captureTime) }
        HomeSummary(
            totalCaptures = totalCaptures,
            averageDailyCaptures = if (dayBuckets.isEmpty()) 0.0 else totalCaptures.toDouble() / dayBuckets.size.toDouble(),
            liveRatio = ratio(sessions.count { it.isLivePhoto }, totalCaptures),
            rawRatio = ratio(sessions.count { it.isRawCapture }, totalCaptures),
            lensBreakdown = lensBreakdown,
            focalBreakdown = focalBreakdown,
            deviceBreakdown = deviceBreakdown,
            monthlyTrend = monthBreakdown,
            yearlyTrend = yearBreakdown,
            latestScanJob = latest,
        )
    }

    override suspend fun loadCalendarSummary(filters: FilterSpec): List<InsightBucket> = withContext(Dispatchers.IO) {
        filteredSessions(filters).groupCount { yyyyMmDd(it.captureTime) }
    }

    override suspend fun loadInsightBreakdown(
        dimension: InsightDimension,
        filters: FilterSpec,
    ): List<InsightBucket> = withContext(Dispatchers.IO) {
        val sessions = filteredSessions(filters)
        when (dimension) {
            InsightDimension.LENS -> sessions.groupCount { it.lensClass.name }
            InsightDimension.FOCAL_EQ -> sessions.groupCount { bucketFocal(it.focalLengthEq) }
            InsightDimension.LIVE -> listOf(
                InsightBucket("Live", sessions.count { it.isLivePhoto }),
                InsightBucket("Non-Live", sessions.count { !it.isLivePhoto }),
            )
            InsightDimension.RAW -> listOf(
                InsightBucket("RAW", sessions.count { it.isRawCapture }),
                InsightBucket("Non-RAW", sessions.count { !it.isRawCapture }),
            )
            InsightDimension.DEVICE -> sessions.groupCount { it.deviceModel ?: "未知机型" }
            InsightDimension.MONTH -> sessions.groupCount { yyyyMm(it.captureTime) }
            InsightDimension.YEAR -> sessions.groupCount { yyyy(it.captureTime) }
            InsightDimension.MODE -> sessions.groupCount { it.captureModeLabel ?: "未判定" }
        }
    }

    override suspend fun loadRuleGroups(): List<RuleGroupSummary> = withContext(Dispatchers.IO) {
        val sessions = database.captureSessionDao().getAll().map { it.toDomain() }
        listOf(
            RuleGroupSummary("主摄", sessions.count { it.lensClass == LensClass.MAIN }, FilterSpec(lensClasses = setOf(LensClass.MAIN))),
            RuleGroupSummary("超广角", sessions.count { it.lensClass == LensClass.ULTRA_WIDE }, FilterSpec(lensClasses = setOf(LensClass.ULTRA_WIDE))),
            RuleGroupSummary("长焦", sessions.count { it.lensClass == LensClass.TELE }, FilterSpec(lensClasses = setOf(LensClass.TELE))),
            RuleGroupSummary("Live Photo", sessions.count { it.isLivePhoto }, FilterSpec(liveOnly = true)),
            RuleGroupSummary("RAW", sessions.count { it.isRawCapture }, FilterSpec(rawOnly = true)),
            RuleGroupSummary("大师", sessions.count { it.captureModeLabel == "大师" }, FilterSpec(captureModes = setOf("大师"))),
        )
    }

    override suspend fun loadRulePreview(filters: FilterSpec): List<AssetPreview> = withContext(Dispatchers.IO) {
        val sessions = filteredSessions(filters)
        val assets = database.mediaAssetDao().getAll().associateBy { it.assetId }
        sessions.mapNotNull { session ->
            assets[session.primaryAssetId]?.toAssetPreview()
        }
    }

    override suspend fun previewMove(
        ruleName: String,
        targetFolder: String,
        filters: FilterSpec,
    ): MovePreview = withContext(Dispatchers.IO) {
        val sessions = filteredSessions(filters)
        val assetsById = database.mediaAssetDao().getAll().associateBy { it.assetId }
        val moveTarget = resolveMoveTarget(targetFolder)
        val existingNames = existingDisplayNamesForPath(moveTarget.relativePath)
        val reservedNames = existingNames.mapTo(mutableSetOf()) { it.lowercase() }
        val candidates = sessions.flatMap { session ->
            buildList {
                assetsById[session.primaryAssetId]?.let { entity ->
                    add(entity.toMoveCandidate(moveTarget, reservedNames, MoveCandidateRole.PRIMARY))
                }
                session.pairedRawAssetId?.let { rawAssetId ->
                    assetsById[rawAssetId]?.let { entity ->
                        add(entity.toMoveCandidate(moveTarget, reservedNames, MoveCandidateRole.RAW_COMPANION))
                    }
                }
            }
        }
        val preview = MovePreview(
            token = UUID.randomUUID().toString(),
            ruleName = ruleName,
            targetFolder = moveTarget.absolutePath,
            count = candidates.size,
            conflicts = candidates.count { it.conflict },
            candidates = candidates,
        )
        val now = System.currentTimeMillis()
        database.withTransaction {
            database.movePlanDao().upsert(
                MovePlanEntity(
                    token = preview.token,
                    ruleName = ruleName,
                    targetFolder = moveTarget.absolutePath,
                    targetRelativePath = moveTarget.relativePath,
                    createdAt = now,
                    status = "PENDING",
                    completedAt = null,
                ),
            )
            database.moveCandidateDao().upsertAll(candidates.map { it.toEntity(preview.token, now) })
        }
        preview
    }

    override suspend fun executeMove(
        previewToken: String,
        writeAccessGranted: Boolean,
    ): MoveExecutionOutcome = withContext(Dispatchers.IO) {
        val preview = loadMovePreview(previewToken)
            ?: return@withContext MoveExecutionOutcome.Completed(
                MoveResult(previewToken, 0, 1, listOf("preview token not found")),
            )
        val pendingCandidates = preview.candidates.filterNot { it.status == MoveCandidateStatus.MOVED }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !writeAccessGranted) {
            createWriteAccessIntentSender(pendingCandidates.map { mediaWriteUriString(it.sourceUri) })?.let { intentSender ->
                return@withContext MoveExecutionOutcome.RequiresWriteAccess(intentSender)
            }
        }
        val failures = mutableListOf<String>()
        var movedCount = preview.candidates.count { it.status == MoveCandidateStatus.MOVED }
        val candidatesToMove = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && writeAccessGranted) {
            pendingCandidates.take(MEDIASTORE_WRITE_REQUEST_MAX_URIS)
        } else {
            pendingCandidates
        }
        candidatesToMove.forEach { candidate ->
            val sourceUri = mediaWriteUriString(candidate.sourceUri).toUriOrNull()
            if (sourceUri == null) {
                val failure = "${candidate.fileName}: source uri unavailable"
                failures += failure
                updateMoveCandidate(previewToken, candidate, MoveCandidateStatus.FAILED, failure)
                return@forEach
            }
            var failureMessage: String? = null
            val moved = runCatching {
                moveMediaStoreItem(
                    sourceUri = sourceUri,
                    destinationFileName = candidate.destinationFileName,
                    destinationRelativePath = candidate.destinationRelativePath,
                )
            }.fold(
                onSuccess = { it },
                onFailure = { error ->
                    when {
                        Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && error is RecoverableSecurityException && !writeAccessGranted -> {
                            return@withContext MoveExecutionOutcome.RequiresWriteAccess(
                                error.userAction.actionIntent.intentSender,
                            )
                        }
                        else -> {
                            failureMessage = "${candidate.fileName}: ${error.message ?: "MediaStore update failed"}"
                            false
                        }
                    }
                },
            )
            if (moved) {
                movedCount += 1
                updateMoveCandidate(previewToken, candidate, MoveCandidateStatus.MOVED, null)
            } else {
                val failure = failureMessage ?: "${candidate.fileName}: MediaStore update returned 0 rows"
                failures += failure
                updateMoveCandidate(previewToken, candidate, MoveCandidateStatus.FAILED, failure)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && writeAccessGranted) {
            val refreshed = loadMovePreview(previewToken)
            val nextPendingUris = refreshed
                ?.candidates
                ?.filter { it.status == MoveCandidateStatus.PENDING }
                ?.map { mediaWriteUriString(it.sourceUri) }
                .orEmpty()
            createWriteAccessIntentSender(nextPendingUris)?.let { intentSender ->
                return@withContext MoveExecutionOutcome.RequiresWriteAccess(intentSender)
            }
        }
        val completedPreview = loadMovePreview(previewToken)
        val completedCandidates = completedPreview?.candidates ?: preview.candidates
        val allFailures = completedCandidates
            .filter { it.status == MoveCandidateStatus.FAILED }
            .map { candidate ->
                candidate.failureReason ?: "${candidate.fileName}: move failed"
            }
        val result = MoveResult(
            token = previewToken,
            movedCount = completedCandidates.count { it.status == MoveCandidateStatus.MOVED }.coerceAtLeast(movedCount),
            failureCount = allFailures.size,
            failures = allFailures,
        )
        database.movePlanDao().get(previewToken)?.let { plan ->
            database.movePlanDao().upsert(
                plan.copy(
                    status = if (allFailures.isEmpty()) "COMPLETED" else "PARTIAL_FAILED",
                    completedAt = System.currentTimeMillis(),
                ),
            )
        }
        recordMoveAudit(
            previewToken = previewToken,
            movedCount = result.movedCount,
            failures = allFailures,
        )
        MoveExecutionOutcome.Completed(result)
    }

    override suspend fun latestScanJob(): ScanJob? = withContext(Dispatchers.IO) {
        database.scanJobDao().latest()?.toDomain()
    }

    override fun latestScanJobFlow(): Flow<ScanJob?> =
        database.scanJobDao().latestFlow()
            .map { it?.toDomain() }
            .flowOn(Dispatchers.IO)

    override suspend fun loadFailedItems(): List<FailedItem> = withContext(Dispatchers.IO) {
        val parseFailures = database.mediaAssetDao().getAll()
            .mapNotNull {
                when (ParseStatus.valueOf(it.parseStatus)) {
                    ParseStatus.FAILED -> FailedItem(
                        title = it.fileName,
                        detail = "解析失败",
                        createdAt = it.modifiedAt,
                    )
                    ParseStatus.SKIPPED -> FailedItem(
                        title = it.fileName,
                        detail = "缺少必要 EXIF 或非本机拍摄，已跳过分析",
                        createdAt = it.modifiedAt,
                    )
                    else -> null
                }
            }
        val moveFailures = database.archiveAuditDao().recent(50).flatMap { audit ->
            JSONArray(audit.failureJson).let { json ->
                buildList {
                    for (index in 0 until json.length()) {
                        add(
                            FailedItem(
                                title = "移动失败",
                                detail = json.getString(index),
                                createdAt = audit.createdAt,
                            ),
                        )
                    }
                }
            }
        }
        (parseFailures + moveFailures).sortedByDescending { it.createdAt }
    }

    override suspend fun loadDeviceProfiles(): List<DeviceProfile> = withContext(Dispatchers.IO) {
        bootstrapProfiles()
        loadProfiles()
    }

    private suspend fun extractBatch(
        batch: List<MediaCandidate>,
        classifier: OplusClassifier,
    ): List<ExtractResult> = coroutineScope {
        batch.map { candidate ->
            async(metadataDispatcher) {
                runCatching {
                    val metadata: ExtractedMetadata = metadataExtractor.extract(candidate)
                    ExtractResult(candidate, classifier.classify(candidate, metadata, parseVersion), false)
                }.getOrElse { error ->
                    Log.w(logTag, "Failed to extract metadata for ${candidate.displayName}", error)
                    ExtractResult(candidate, candidate.toFailedAsset(parseVersion), true)
                }
            }
        }.awaitAll()
    }

    private suspend fun upsertMediaAssets(items: List<MediaAssetEntity>) {
        items.chunked(entityUpsertBatchSize).forEach { chunk ->
            database.mediaAssetDao().upsertAll(chunk)
        }
    }

    private suspend fun upsertCaptureSessions(items: List<CaptureSessionEntity>) {
        items.chunked(entityUpsertBatchSize).forEach { chunk ->
            database.captureSessionDao().upsertAll(chunk)
        }
    }

    private suspend fun filteredSessions(filters: FilterSpec): List<CaptureSession> {
        val all = database.captureSessionDao().getAll().map { it.toDomain() }
        return all.filter { session ->
            val captureDate = localDate(session.captureTime)
            if (filters.deviceModels.isNotEmpty() && session.deviceModel !in filters.deviceModels) return@filter false
            if (filters.lensClasses.isNotEmpty() && session.lensClass !in filters.lensClasses) return@filter false
            if (filters.liveOnly != null && session.isLivePhoto != filters.liveOnly) return@filter false
            if (filters.rawOnly != null && session.isRawCapture != filters.rawOnly) return@filter false
            if (filters.captureModes.isNotEmpty() && session.captureModeLabel !in filters.captureModes) return@filter false
            if (filters.years.isNotEmpty() && captureDate.year !in filters.years) return@filter false
            if (filters.dates.isNotEmpty() && captureDate !in filters.dates) return@filter false
            if (filters.yearMonths.isNotEmpty() && yyyyMm(session.captureTime) !in filters.yearMonths) return@filter false
            if (filters.focalEqRangeLabels.isNotEmpty() && !matchesFocalFilter(session.focalLengthEq, filters.focalEqRangeLabels)) return@filter false
            true
        }
    }

    private suspend fun bootstrapProfiles() {
        val existing = database.deviceProfileDao().getAll()
        if (existing.isNotEmpty()) return
        database.deviceProfileDao().upsertAll(defaultDeviceProfiles().map { it.toEntity() })
    }

    private suspend fun loadProfiles() = database.deviceProfileDao().getAll().map { it.toDomain() }

    private suspend fun activeGenerationId(): Long =
        database.indexGenerationDao().activeGenerationId() ?: DEFAULT_GENERATION_ID

    private suspend fun setActiveGeneration(generationId: Long) {
        database.indexGenerationDao().upsert(
            IndexGenerationEntity(
                generationKey = ACTIVE_GENERATION_KEY,
                activeGenerationId = generationId,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    private fun newScanGenerationId(): Long =
        ((System.currentTimeMillis() * 1_000L) + (System.nanoTime().mod(1_000L)))
            .coerceAtLeast(DEFAULT_GENERATION_ID + 1)

    private suspend fun upsertScanJob(
        jobId: String,
        type: ScanType,
        status: ScanStatus,
        totalCount: Int,
        scannedCount: Int,
        startedAt: Long,
        finishedAt: Long?,
        failureCount: Int,
    ) {
        database.scanJobDao().upsert(
            ScanJobEntity(
                jobId = jobId,
                type = type.name,
                status = status.name,
                totalCount = totalCount,
                scannedCount = scannedCount,
                etaSeconds = null,
                startedAt = startedAt,
                finishedAt = finishedAt,
                parseVersion = parseVersion,
                failureCount = failureCount,
            ),
        )
    }

    private suspend fun upsertCursor(
        scope: ScanScope,
        previous: ScanCursorEntity?,
        activeJobId: String?,
        lastScanned: ScanCursorPosition?,
        lastParsed: ScanCursorPosition?,
        resume: ScanCursorPosition?,
    ) {
        database.scanCursorDao().upsert(
            ScanCursorEntity(
                scopeHash = scope.scopeHash,
                rootsJson = scope.rootsJson,
                parseVersion = parseVersion,
                lastSuccessfulModifiedAt = lastParsed?.modifiedAt,
                lastSuccessfulMediaId = lastParsed?.mediaStoreId,
                lastScannedModifiedAt = lastScanned?.modifiedAt,
                lastScannedMediaId = lastScanned?.mediaStoreId,
                resumeModifiedAt = resume?.modifiedAt,
                resumeMediaId = resume?.mediaStoreId,
                activeJobId = activeJobId,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        Log.d(
            logTag,
            "Cursor state saved scope=${scope.scopeHash} activeJobId=$activeJobId lastScanned=$lastScanned lastParsed=$lastParsed resume=$resume previousActive=${previous?.activeJobId}",
        )
    }

    private fun determineScanExecution(
        requestedType: ScanType,
        scope: ScanScope,
        cursor: ScanCursorEntity?,
    ): ScanExecution {
        val execution = determineScanExecution(
            requestedType = requestedType,
            scopeRootsJson = scope.rootsJson,
            parseVersion = parseVersion,
            cursorRootsJson = cursor?.rootsJson,
            cursorParseVersion = cursor?.parseVersion,
            activeJobId = cursor?.activeJobId,
            resumePosition = cursor?.resumePosition(),
            lastScannedPosition = cursor?.lastScannedPosition(),
        )
        execution.startAfter?.let { position ->
            Log.i(logTag, "${execution.reason} start from modifiedAt=${position.modifiedAt} id=${position.mediaStoreId}")
        }
        return execution
    }

    private fun buildScanScope(extraRoots: Set<String>): ScanScope {
        val roots = MediaScanner.effectiveRoots(extraRoots).sortedBy { it.lowercase() }
        val rootsJson = JSONArray(roots).toString()
        val scopeHash = sha256Hex(rootsJson)
        return ScanScope(roots, rootsJson, scopeHash)
    }

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString(separator = "") { byte -> "%02x".format(byte) }

    private fun List<CaptureSession>.groupCount(selector: (CaptureSession) -> String): List<InsightBucket> =
        groupBy(selector).map { InsightBucket(it.key, it.value.size) }.sortedByDescending { it.count }

    private fun ratio(part: Int, total: Int): Double =
        if (total == 0) 0.0 else ((part.toDouble() / total.toDouble()) * 1000.0).roundToInt() / 10.0

    private fun bucketFocal(value: Int?): String = when {
        value == null -> "未知焦段"
        value <= 18 -> "0-18mm"
        value in 19..34 -> "19-34mm"
        value in 35..70 -> "35-70mm"
        else -> "71mm+"
    }

    private fun yyyyMmDd(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .toString()

    private fun localDate(epochMillis: Long): LocalDate = Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    private fun yyyyMm(epochMillis: Long): String = yyyyMmDd(epochMillis).substring(0, 7)

    private fun yyyy(epochMillis: Long): String = yyyyMmDd(epochMillis).substring(0, 4)

    private fun matchesFocalFilter(value: Int?, labels: Set<String>): Boolean {
        if (value == null) return "未知焦段" in labels
        if (bucketFocal(value) in labels) return true
        return labels.any { label -> value in parseFocalLabel(label) }
    }

    private fun parseFocalLabel(label: String): IntRange {
        val normalized = label
            .replace("–", "-")
            .replace("—", "-")
            .removeSuffix("mm")
            .trim()
        return when {
            normalized.endsWith("+") -> {
                val min = normalized.removeSuffix("+").trim().toIntOrNull() ?: return IntRange.EMPTY
                min..Int.MAX_VALUE
            }
            "-" in normalized -> {
                val parts = normalized.split("-", limit = 2)
                val min = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: return IntRange.EMPTY
                val max = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: return IntRange.EMPTY
                min..max
            }
            else -> IntRange.EMPTY
        }
    }

    private fun uniqueDisplayName(
        requestedName: String,
        reservedNames: MutableSet<String>,
    ): String {
        var candidate = requestedName
        var index = 1
        while (!reservedNames.add(candidate.lowercase())) {
            candidate = appendConflictSuffix(requestedName, index)
            index += 1
        }
        return candidate
    }

    private fun appendConflictSuffix(fileName: String, index: Int): String {
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex <= 0 || dotIndex == fileName.lastIndex) {
            "${fileName}__${index}"
        } else {
            val name = fileName.substring(0, dotIndex)
            val extension = fileName.substring(dotIndex)
            "${name}__${index}${extension}"
        }
    }

    private fun existingDisplayNamesForPath(relativePath: String): Set<String> {
        val names = mutableSetOf<String>()
        contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
            "${MediaStore.MediaColumns.RELATIVE_PATH} = ?",
            arrayOf(relativePath),
            null,
        )?.use { cursor ->
            val displayNameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                names += cursor.getString(displayNameIndex).orEmpty()
            }
        }
        return names
    }

    private fun moveMediaStoreItem(
        sourceUri: Uri,
        destinationFileName: String,
        destinationRelativePath: String,
    ): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, destinationFileName)
            put(MediaStore.MediaColumns.RELATIVE_PATH, destinationRelativePath)
        }
        return contentResolver.update(sourceUri, values, null, null) == 1
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun createWriteAccessIntentSender(uriStrings: List<String>): IntentSender? {
        val uris = chunkWriteRequestUris(uriStrings)
            .firstOrNull()
            ?.mapNotNull(String::toUriOrNull)
            .orEmpty()
        if (uris.isEmpty()) return null
        return MediaStore.createWriteRequest(contentResolver, uris).intentSender
    }

    private fun MediaAssetEntity.toMoveCandidate(
        moveTarget: MoveTarget,
        reservedNames: MutableSet<String>,
        role: MoveCandidateRole,
    ): MoveCandidate {
        val destinationFileName = uniqueDisplayName(fileName, reservedNames)
        val destinationPath = File(moveTarget.absolutePath, destinationFileName).absolutePath
        return MoveCandidate(
            assetId = assetId,
            sourcePath = path,
            sourceUri = uri,
            fileName = fileName,
            destinationFileName = destinationFileName,
            destinationRelativePath = moveTarget.relativePath,
            destinationPath = destinationPath,
            conflict = destinationFileName != fileName,
            role = role,
            captureDate = localDate(createdAt),
            deviceModel = deviceModel,
            focalLengthEq = focalLengthEq,
            captureModeLabel = captureModeLabel,
            isLivePhoto = isLivePhoto,
            isRaw = isRaw,
        )
    }

    private suspend fun loadMovePreview(token: String): MovePreview? {
        val plan = database.movePlanDao().get(token) ?: return null
        val candidates = database.moveCandidateDao().getForPlan(token).map { it.toDomain() }
        return MovePreview(
            token = plan.token,
            ruleName = plan.ruleName,
            targetFolder = plan.targetFolder,
            count = candidates.size,
            conflicts = candidates.count { it.conflict },
            candidates = candidates,
        )
    }

    private suspend fun updateMoveCandidate(
        planToken: String,
        candidate: MoveCandidate,
        status: MoveCandidateStatus,
        failureReason: String?,
    ) {
        database.moveCandidateDao().upsert(
            candidate.copy(status = status, failureReason = failureReason)
                .toEntity(planToken, System.currentTimeMillis()),
        )
    }

    private suspend fun recordMoveAudit(
        previewToken: String,
        movedCount: Int,
        failures: List<String>,
    ) {
        database.archiveAuditDao().insert(
            ArchiveAuditEntity(
                auditId = UUID.randomUUID().toString(),
                previewToken = previewToken,
                createdAt = System.currentTimeMillis(),
                movedCount = movedCount,
                failureCount = failures.size,
                failureJson = JSONArray(failures).toString(),
            ),
        )
    }
}

private const val DEFAULT_MOVE_RELATIVE_PATH = "DCIM/myalbums/"
private const val ACTIVE_GENERATION_KEY = "active"
private const val DEFAULT_GENERATION_ID = 0L
internal const val MEDIASTORE_WRITE_REQUEST_MAX_URIS = 1_000

internal fun chunkWriteRequestUris(uriStrings: List<String>): List<List<String>> =
    uriStrings.chunked(MEDIASTORE_WRITE_REQUEST_MAX_URIS)

internal data class MoveTarget(
    val absolutePath: String,
    val relativePath: String,
)

private fun String.toUriOrNull(): Uri? = runCatching { Uri.parse(this) }.getOrNull()

private fun String.ensureTrailingSlash(): String = if (endsWith("/")) this else "$this/"

internal fun mediaWriteUriString(uriString: String): String {
    legacyFilesMediaUri.matchEntire(uriString)
        ?.groupValues
        ?.getOrNull(1)
        ?.toLongOrNull()
        ?.let { return mediaStoreImageUriForId(it) }
    val uri = uriString.toUriOrNull() ?: return uriString
    val segments = uri.pathSegments
    val isLegacyFilesMediaUri = uri.scheme == "content" &&
        uri.authority == MediaStore.AUTHORITY &&
        "file" in segments
    if (!isLegacyFilesMediaUri) return uriString
    val id = segments.lastOrNull()?.toLongOrNull() ?: return uriString
    return mediaStoreImageUriForId(id)
}

private val legacyFilesMediaUri = Regex("^content://media/[^/]+/file/(\\d+)$")

internal fun resolveMoveTarget(targetFolder: String): MoveTarget {
    val normalizedAbsolute = targetFolder.ensureTrailingSlash()
    val normalizedRelative = normalizedAbsolute
        .removePrefix("/storage/emulated/0/")
        .removePrefix("/")
        .ifBlank { DEFAULT_MOVE_RELATIVE_PATH.removeSuffix("/") }
        .ensureTrailingSlash()
    val absolutePath = if (normalizedAbsolute.startsWith("/storage/emulated/0/")) {
        normalizedAbsolute
    } else {
        "/storage/emulated/0/$normalizedRelative"
    }
    return MoveTarget(absolutePath = absolutePath, relativePath = normalizedRelative)
}

internal fun resolveRuleMoveTarget(
    baseFolder: String,
    ruleName: String,
): MoveTarget {
    val base = resolveMoveTarget(baseFolder)
    val albumFolder = sanitizeAlbumFolderSegment(ruleName)
    return resolveMoveTarget("${base.absolutePath}$albumFolder/")
}

private fun sanitizeAlbumFolderSegment(value: String): String {
    val sanitized = value
        .trim()
        .replace(Regex("[/\\\\]+"), "-")
        .replace(Regex("\\p{Cntrl}+"), "")
        .trim()
        .trim('.')
    return sanitized.ifBlank { "未命名规则" }
}

private data class ScanScope(
    val roots: List<String>,
    val rootsJson: String,
    val scopeHash: String,
)

internal data class ScanExecution(
    val effectiveType: ScanType,
    val startAfter: ScanCursorPosition?,
    val reason: String,
)

private data class ExtractResult(
    val candidate: MediaCandidate,
    val asset: MediaAsset,
    val failed: Boolean,
)

private fun MediaCandidate.cursorPosition() = ScanCursorPosition(
    modifiedAt = modifiedAt,
    mediaStoreId = id,
)

private fun ScanCursorEntity.lastParsedPosition(): ScanCursorPosition? {
    val modifiedAt = lastSuccessfulModifiedAt ?: return null
    val mediaStoreId = lastSuccessfulMediaId ?: return null
    return ScanCursorPosition(modifiedAt, mediaStoreId)
}

private fun ScanCursorEntity.lastScannedPosition(): ScanCursorPosition? {
    val modifiedAt = lastScannedModifiedAt ?: lastSuccessfulModifiedAt ?: return null
    val mediaStoreId = lastScannedMediaId ?: lastSuccessfulMediaId ?: return null
    return ScanCursorPosition(modifiedAt, mediaStoreId)
}

private fun ScanCursorEntity.resumePosition(): ScanCursorPosition? {
    val modifiedAt = resumeModifiedAt ?: return null
    val mediaStoreId = resumeMediaId ?: return null
    return ScanCursorPosition(modifiedAt, mediaStoreId)
}

internal fun determineScanExecution(
    requestedType: ScanType,
    scopeRootsJson: String,
    parseVersion: Int,
    cursorRootsJson: String?,
    cursorParseVersion: Int?,
    activeJobId: String?,
    resumePosition: ScanCursorPosition?,
    lastScannedPosition: ScanCursorPosition?,
): ScanExecution {
    if (requestedType == ScanType.FULL) {
        return ScanExecution(ScanType.FULL, null, "requested_full")
    }
    if (cursorRootsJson == null || cursorParseVersion == null) {
        return ScanExecution(ScanType.FULL, null, "first_scan")
    }
    if (cursorRootsJson != scopeRootsJson) {
        return ScanExecution(ScanType.FULL, null, "roots_changed")
    }
    if (cursorParseVersion != parseVersion) {
        return ScanExecution(ScanType.FULL, null, "parse_version_changed")
    }
    if (activeJobId != null && resumePosition != null) {
        return ScanExecution(ScanType.INCREMENTAL, resumePosition, "resume")
    }
    return ScanExecution(
        effectiveType = ScanType.INCREMENTAL,
        startAfter = lastScannedPosition ?: ScanCursorPosition(0L, 0L),
        reason = "incremental",
    )
}

private fun MediaAsset.toEntity(generationId: Long) = MediaAssetEntity(
    generationId = generationId,
    assetId = assetId,
    mediaStoreId = mediaStoreId,
    path = path,
    relativePath = relativePath,
    uri = uri,
    fileName = fileName,
    mimeType = mimeType,
    size = size,
    createdAt = createdAt,
    modifiedAt = modifiedAt,
    isOplusOriginal = isOplusOriginal,
    isLivePhoto = isLivePhoto,
    isRaw = isRaw,
    pairedCaptureId = pairedCaptureId,
    deviceModel = deviceModel,
    focalLength = focalLength,
    focalLengthEq = focalLengthEq,
    lensClass = lensClass.name,
    captureModeLabel = captureModeLabel,
    userCommentRaw = userCommentRaw,
    userCommentDigest = userCommentDigest,
    parseStatus = parseStatus.name,
    parseVersion = parseVersion,
    sourceConfidence = sourceConfidence,
    capturePairStatus = capturePairStatus.name,
    contentSignature = contentSignature,
)

private fun MediaAssetEntity.toDomain() = MediaAsset(
    assetId = assetId,
    mediaStoreId = mediaStoreId,
    path = path,
    relativePath = relativePath,
    uri = uri,
    fileName = fileName,
    mimeType = mimeType,
    size = size,
    createdAt = createdAt,
    modifiedAt = modifiedAt,
    isOplusOriginal = isOplusOriginal,
    isLivePhoto = isLivePhoto,
    isRaw = isRaw,
    pairedCaptureId = pairedCaptureId,
    deviceModel = deviceModel,
    focalLength = focalLength,
    focalLengthEq = focalLengthEq,
    lensClass = LensClass.valueOf(lensClass),
    captureModeLabel = captureModeLabel,
    userCommentRaw = userCommentRaw,
    userCommentDigest = userCommentDigest,
    parseStatus = ParseStatus.valueOf(parseStatus),
    parseVersion = parseVersion,
    sourceConfidence = sourceConfidence,
    capturePairStatus = CapturePairStatus.valueOf(capturePairStatus),
    contentSignature = contentSignature,
)

private fun MediaCandidate.toFailedAsset(parseVersion: Int) = MediaAsset(
    assetId = UUID.nameUUIDFromBytes("media:${id}".toByteArray()).toString(),
    mediaStoreId = id,
    path = absolutePath,
    relativePath = relativePath,
    uri = uri,
    fileName = displayName,
    mimeType = mimeType,
    size = size,
    createdAt = createdAt,
    modifiedAt = modifiedAt,
    isOplusOriginal = false,
    isLivePhoto = false,
    isRaw = mimeType.contains("dng", ignoreCase = true) || displayName.endsWith(".dng", ignoreCase = true),
    pairedCaptureId = null,
    deviceModel = null,
    focalLength = null,
    focalLengthEq = null,
    lensClass = LensClass.UNKNOWN,
    captureModeLabel = null,
    userCommentRaw = null,
    userCommentDigest = null,
    parseStatus = ParseStatus.FAILED,
    parseVersion = parseVersion,
    sourceConfidence = 0.0,
    capturePairStatus = if (mimeType.contains("dng", ignoreCase = true) || displayName.endsWith(".dng", ignoreCase = true)) {
        CapturePairStatus.UNPAIRED_RAW
    } else {
        CapturePairStatus.NONE
    },
    contentSignature = "${id}:${size}:${modifiedAt}",
)

private fun CaptureSession.toEntity(generationId: Long) = CaptureSessionEntity(
    generationId = generationId,
    captureId = captureId,
    primaryAssetId = primaryAssetId,
    pairedRawAssetId = pairedRawAssetId,
    captureTime = captureTime,
    deviceModel = deviceModel,
    lensClass = lensClass.name,
    focalLengthEq = focalLengthEq,
    isLivePhoto = isLivePhoto,
    isRawCapture = isRawCapture,
    captureModeLabel = captureModeLabel,
)

private fun MediaAssetEntity.toAssetPreview() = AssetPreview(
    assetId = assetId,
    assetUri = uri,
    fileName = fileName,
    captureDate = Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate(),
    deviceModel = deviceModel,
    focalLengthEq = focalLengthEq,
    captureModeLabel = captureModeLabel,
    isLivePhoto = isLivePhoto,
    isRaw = isRaw,
)

private fun DeviceProfileEntity.toDomain(): DeviceProfile {
    val aliases = JSONArray(aliasesJson).let { json -> buildList { for (i in 0 until json.length()) add(json.getString(i)) } }
    val rangesArray = JSONArray(focalEqRangesJson)
    val ranges = buildList {
        for (i in 0 until rangesArray.length()) {
            val item = rangesArray.getJSONObject(i)
            add(
                com.oplus.groupimaging.domain.FocalEqRange(
                    label = item.getString("label"),
                    minInclusive = item.getInt("minInclusive"),
                    maxInclusive = item.getInt("maxInclusive"),
                    lensClass = LensClass.valueOf(item.getString("lensClass")),
                ),
            )
        }
    }
    val cameraMapJson = JSONObject(cameraIdMapJson)
    val cameraMap = buildMap {
        cameraMapJson.keys().forEach { key ->
            put(key.toInt(), LensClass.valueOf(cameraMapJson.getString(key)))
        }
    }
    return DeviceProfile(deviceModel, aliases, ranges, cameraMap, profileVersion)
}

private fun DeviceProfile.toEntity(): DeviceProfileEntity {
    val aliases = JSONArray().apply {
        this@toEntity.aliases.forEach(::put)
    }
    val ranges = JSONArray().apply {
        focalEqRanges.forEach { range ->
            put(
                JSONObject()
                    .put("label", range.label)
                    .put("minInclusive", range.minInclusive)
                    .put("maxInclusive", range.maxInclusive)
                    .put("lensClass", range.lensClass.name),
            )
        }
    }
    val cameraMap = JSONObject().apply {
        cameraIdMap.forEach { (id, lens) -> put(id.toString(), lens.name) }
    }
    return DeviceProfileEntity(deviceModel, aliases.toString(), ranges.toString(), cameraMap.toString(), profileVersion)
}

private fun CaptureSessionEntity.toDomain() = CaptureSession(
    captureId = captureId,
    primaryAssetId = primaryAssetId,
    pairedRawAssetId = pairedRawAssetId,
    captureTime = captureTime,
    deviceModel = deviceModel,
    lensClass = LensClass.valueOf(lensClass),
    focalLengthEq = focalLengthEq,
    isLivePhoto = isLivePhoto,
    isRawCapture = isRawCapture,
    captureModeLabel = captureModeLabel,
)

private fun ScanJobEntity.toDomain() = ScanJob(
    jobId = jobId,
    type = ScanType.valueOf(type),
    status = ScanStatus.valueOf(status),
    totalCount = totalCount,
    scannedCount = scannedCount,
    etaSeconds = etaSeconds,
    startedAt = startedAt,
    finishedAt = finishedAt,
    parseVersion = parseVersion,
    failureCount = failureCount,
)

private fun MoveCandidate.toEntity(planToken: String, updatedAt: Long) = MoveCandidateEntity(
    planToken = planToken,
    assetId = assetId,
    sourcePath = sourcePath,
    sourceUri = sourceUri,
    fileName = fileName,
    destinationFileName = destinationFileName,
    destinationRelativePath = destinationRelativePath,
    destinationPath = destinationPath,
    conflict = conflict,
    role = role.name,
    status = status.name,
    failureReason = failureReason,
    captureDate = captureDate?.toString(),
    deviceModel = deviceModel,
    focalLengthEq = focalLengthEq,
    captureModeLabel = captureModeLabel,
    isLivePhoto = isLivePhoto,
    isRaw = isRaw,
    updatedAt = updatedAt,
)

private fun MoveCandidateEntity.toDomain() = MoveCandidate(
    assetId = assetId,
    sourcePath = sourcePath,
    sourceUri = sourceUri,
    fileName = fileName,
    destinationFileName = destinationFileName,
    destinationRelativePath = destinationRelativePath,
    destinationPath = destinationPath,
    conflict = conflict,
    role = MoveCandidateRole.valueOf(role),
    status = MoveCandidateStatus.valueOf(status),
    failureReason = failureReason,
    captureDate = captureDate?.let(LocalDate::parse),
    deviceModel = deviceModel,
    focalLengthEq = focalLengthEq,
    captureModeLabel = captureModeLabel,
    isLivePhoto = isLivePhoto,
    isRaw = isRaw,
)
