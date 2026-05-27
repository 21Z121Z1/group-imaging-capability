package com.oplus.groupimaging.core

import com.oplus.groupimaging.domain.CapturePairStatus
import com.oplus.groupimaging.domain.DeviceProfile
import com.oplus.groupimaging.domain.FocalEqRange
import com.oplus.groupimaging.domain.LensClass
import com.oplus.groupimaging.domain.MediaAsset
import com.oplus.groupimaging.domain.ParseStatus
import java.util.Locale
import java.util.UUID

class OplusClassifier(
    private val profiles: List<DeviceProfile>,
) {
    fun classify(candidate: MediaCandidate, metadata: ExtractedMetadata, parseVersion: Int): MediaAsset {
        val decoded = OplusCommentDecoder.decode(metadata.userComment)
        val deviceModel = metadata.model ?: metadata.make
        val lensClass = resolveLens(deviceModel, metadata.focalLengthEq)
        val isRaw = candidate.mimeType.contains("dng", ignoreCase = true) ||
            candidate.displayName.endsWith(".dng", ignoreCase = true)
        val originalSignals = listOf(
            candidate.relativePath?.contains("DCIM", ignoreCase = true) == true,
            metadata.make?.contains("oppo", ignoreCase = true) == true ||
                metadata.make?.contains("oplus", ignoreCase = true) == true ||
                metadata.model?.contains("oppo", ignoreCase = true) == true ||
                metadata.model?.contains("oplus", ignoreCase = true) == true,
            metadata.userComment?.startsWith("oplus_", ignoreCase = true) == true ||
                metadata.userComment?.startsWith("oppo_", ignoreCase = true) == true,
        )
        val sourceConfidence = originalSignals.count { it } / originalSignals.size.toDouble()
        val isOplusOriginal = metadata.hasRequiredExif && sourceConfidence >= 0.67
        val parseStatus = when {
            !metadata.hasRequiredExif -> ParseStatus.SKIPPED
            !isOplusOriginal -> ParseStatus.SKIPPED
            decoded == null -> ParseStatus.PARTIAL
            else -> ParseStatus.PARSED
        }
        val signature = "${candidate.id}:${candidate.size}:${candidate.modifiedAt}"
        return MediaAsset(
            assetId = UUID.nameUUIDFromBytes("media:${candidate.id}".toByteArray()).toString(),
            mediaStoreId = candidate.id,
            path = candidate.absolutePath,
            relativePath = candidate.relativePath,
            uri = candidate.uri,
            fileName = candidate.displayName,
            mimeType = candidate.mimeType,
            size = candidate.size,
            createdAt = metadata.capturedAt,
            modifiedAt = candidate.modifiedAt,
            isOplusOriginal = isOplusOriginal,
            isLivePhoto = decoded?.livePhotoFlag == true,
            isRaw = isRaw,
            pairedCaptureId = null,
            deviceModel = deviceModel,
            focalLength = metadata.focalLengthMm,
            focalLengthEq = metadata.focalLengthEq,
            lensClass = lensClass,
            captureModeLabel = decoded?.captureModeLabel,
            userCommentRaw = metadata.userComment.takeUnless { parseStatus == ParseStatus.PARSED },
            userCommentDigest = metadata.userComment?.let(::sha256Hex),
            parseStatus = parseStatus,
            parseVersion = parseVersion,
            sourceConfidence = sourceConfidence,
            capturePairStatus = when {
                isRaw -> CapturePairStatus.UNPAIRED_RAW
                else -> CapturePairStatus.NONE
            },
            contentSignature = signature,
        )
    }

    private fun resolveLens(deviceModel: String?, focalEq: Int?): LensClass {
        val value = focalEq ?: return LensClass.UNKNOWN
        val modelLower = deviceModel?.lowercase(Locale.ROOT).orEmpty()
        val profile = profiles.firstOrNull { profile ->
            profile.deviceModel.lowercase(Locale.ROOT) == modelLower ||
                profile.aliases.any { it.lowercase(Locale.ROOT) == modelLower }
        }
        val range = profile?.focalEqRanges?.firstOrNull { value in it.minInclusive..it.maxInclusive }
        return range?.lensClass ?: when {
            value <= 18 -> LensClass.ULTRA_WIDE
            value in 19..34 -> LensClass.MAIN
            value >= 35 -> LensClass.TELE
            else -> LensClass.UNKNOWN
        }
    }
}

private val defaultFocalEqRanges = listOf(
    FocalEqRange("0-18mm", 0, 18, LensClass.ULTRA_WIDE),
    FocalEqRange("19-34mm", 19, 34, LensClass.MAIN),
    FocalEqRange("35mm+", 35, 240, LensClass.TELE),
)

fun defaultDeviceProfiles(): List<DeviceProfile> = listOf(
    DeviceProfile(
        deviceModel = "OPPO Find X9",
        aliases = listOf("OPPO Find X9", "Oplus Find X9"),
        focalEqRanges = defaultFocalEqRanges,
        cameraIdMap = mapOf(0 to LensClass.MAIN, 1 to LensClass.ULTRA_WIDE, 2 to LensClass.TELE),
        profileVersion = 1,
    ),
    DeviceProfile(
        deviceModel = "OPPO Find X8 Ultra",
        aliases = listOf("OPPO Find X8 Ultra", "Oplus Find X8 Ultra"),
        focalEqRanges = defaultFocalEqRanges,
        cameraIdMap = mapOf(0 to LensClass.MAIN, 1 to LensClass.ULTRA_WIDE, 2 to LensClass.TELE, 3 to LensClass.TELE),
        profileVersion = 1,
    ),
)
