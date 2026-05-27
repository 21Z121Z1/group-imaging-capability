package com.oplus.groupimaging.core

import com.oplus.groupimaging.domain.CapturePairStatus
import com.oplus.groupimaging.domain.CaptureSession
import com.oplus.groupimaging.domain.MediaAsset
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

class CaptureSessionMatcher @Inject constructor() {
    fun buildSessions(assets: List<MediaAsset>): Pair<List<CaptureSession>, List<MediaAsset>> {
        val sorted = assets.sortedBy { it.createdAt }
        val updated = sorted.toMutableList()
        val updatedIndexByAssetId = sorted.indices.associateBy { sorted[it].assetId }.toMutableMap()
        val sessions = mutableListOf<CaptureSession>()
        val consumedRaw = mutableSetOf<String>()

        val rawByDevicePath = sorted
            .filter { it.isRaw }
            .groupBy { "${it.deviceModel}:${it.relativePath}" }

        sorted.forEachIndexed { index, asset ->
            if (asset.isRaw || consumedRaw.contains(asset.assetId)) return@forEachIndexed
            val key = "${asset.deviceModel}:${asset.relativePath}"
            val rawMatch = rawByDevicePath[key]?.firstOrNull { candidate ->
                !consumedRaw.contains(candidate.assetId) &&
                    stemsMatch(candidate.fileName, asset.fileName) &&
                    abs(candidate.createdAt - asset.createdAt) <= 2_000L
            }
            val captureId = UUID.nameUUIDFromBytes("${asset.assetId}:${rawMatch?.assetId}".toByteArray()).toString()
            sessions += CaptureSession(
                captureId = captureId,
                primaryAssetId = asset.assetId,
                pairedRawAssetId = rawMatch?.assetId,
                captureTime = asset.createdAt,
                deviceModel = asset.deviceModel,
                lensClass = asset.lensClass,
                focalLengthEq = asset.focalLengthEq,
                isLivePhoto = asset.isLivePhoto,
                isRawCapture = rawMatch != null || asset.isRaw,
                captureModeLabel = asset.captureModeLabel,
            )
            updated[index] = asset.copy(pairedCaptureId = captureId)
            if (rawMatch != null) {
                consumedRaw += rawMatch.assetId
                val rawIndex = updatedIndexByAssetId[rawMatch.assetId] ?: -1
                if (rawIndex >= 0) {
                    updated[rawIndex] = rawMatch.copy(
                        pairedCaptureId = captureId,
                        capturePairStatus = CapturePairStatus.RAW_COMPANION,
                    )
                }
            }
        }

        sorted.filter { it.isRaw && it.pairedCaptureId == null && !consumedRaw.contains(it.assetId) }.forEach { asset ->
            val captureId = UUID.nameUUIDFromBytes(asset.assetId.toByteArray()).toString()
            sessions += CaptureSession(
                captureId = captureId,
                primaryAssetId = asset.assetId,
                pairedRawAssetId = asset.assetId,
                captureTime = asset.createdAt,
                deviceModel = asset.deviceModel,
                lensClass = asset.lensClass,
                focalLengthEq = asset.focalLengthEq,
                isLivePhoto = asset.isLivePhoto,
                isRawCapture = true,
                captureModeLabel = asset.captureModeLabel,
            )
            val rawIndex = updatedIndexByAssetId[asset.assetId] ?: -1
            if (rawIndex >= 0) {
                updated[rawIndex] = asset.copy(pairedCaptureId = captureId)
            }
        }
        return sessions to updated
    }

    private companion object {
        val RAW_JPG_SUFFIX = Regex("(_RAW|_JPG)$")
    }

    private fun stemsMatch(left: String, right: String): Boolean {
        val leftStem = left.substringBeforeLast('.').replace(RAW_JPG_SUFFIX, "")
        val rightStem = right.substringBeforeLast('.').replace(RAW_JPG_SUFFIX, "")
        return leftStem == rightStem
    }
}
