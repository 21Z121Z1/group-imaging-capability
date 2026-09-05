package io.github.z121z1.watermarkcleaner.core

import android.content.Context
import java.io.File

class ProfileRepository(context: Context) {
    private val dir = File(context.filesDir, "profiles")
    private val legacyPrimaryFile = File(dir, "primary.wmr3")
    private val legacyGainFile = File(dir, "gain.wmr3")
    private val legacyV2PrimaryFile = File(dir, "primary.wmr2")
    private val legacyV2GainFile = File(dir, "gain.wmr2")

    fun loadBase(width: Int, height: Int, range: DynamicRange): WatermarkProfile? {
        readWatermarkOrNull(baseFile(width, height, range))?.let { return it }
        if (range == DynamicRange.SDR) {
            val legacy = readWatermarkOrNull(legacyPrimaryFile)
            if (legacy?.width == width && legacy.height == height) return legacy
        }
        return null
    }

    fun saveBase(range: DynamicRange, profile: WatermarkProfile) {
        WatermarkProfileCodec.writeAtomically(profile, baseFile(profile.width, profile.height, range))
        legacyV2PrimaryFile.delete()
    }

    fun loadHdrGain(baseWidth: Int, baseHeight: Int): GainMapProfile? =
        readGainOrNull(hdrGainFile(baseWidth, baseHeight))

    fun saveHdrGain(profile: GainMapProfile) {
        GainMapProfileCodec.writeAtomically(profile, hdrGainFile(profile.baseWidth, profile.baseHeight))
        legacyGainFile.delete()
        legacyV2GainFile.delete()
    }

    fun deleteHdrGain(baseWidth: Int, baseHeight: Int) {
        hdrGainFile(baseWidth, baseHeight).delete()
    }

    fun hasAnyBase(): Boolean {
        if (legacyPrimaryFile.isFile) return true
        return dir.listFiles().orEmpty().any { BASE_FILE.matches(it.name) }
    }

    fun calibratedTargets(): Set<CalibrationTarget> {
        val result = linkedSetOf<CalibrationTarget>()
        dir.listFiles().orEmpty().forEach { file ->
            val match = BASE_FILE.matchEntire(file.name) ?: return@forEach
            val range = if (match.groupValues[1] == DynamicRange.HDR.storageToken) DynamicRange.HDR else DynamicRange.SDR
            val width = match.groupValues[2].toIntOrNull() ?: return@forEach
            val height = match.groupValues[3].toIntOrNull() ?: return@forEach
            if (width != height) {
                result += CalibrationTarget(CalibrationOrientation.fromDimensions(width, height), range)
            }
        }
        readWatermarkOrNull(legacyPrimaryFile)?.let { legacy ->
            if (legacy.width != legacy.height) {
                result += CalibrationTarget(
                    CalibrationOrientation.fromDimensions(legacy.width, legacy.height),
                    DynamicRange.SDR,
                )
            }
        }
        return result
    }

    fun clear() {
        dir.listFiles().orEmpty().forEach { file ->
            if (
                BASE_FILE.matches(file.name) || HDR_GAIN_FILE.matches(file.name) ||
                file == legacyPrimaryFile || file == legacyGainFile ||
                file == legacyV2PrimaryFile || file == legacyV2GainFile
            ) {
                file.delete()
            }
        }
    }

    private fun baseFile(width: Int, height: Int, range: DynamicRange): File =
        File(dir, "base_${range.storageToken}_${width}x${height}.wmr3")

    private fun hdrGainFile(baseWidth: Int, baseHeight: Int): File =
        File(dir, "gain_hdr_${baseWidth}x${baseHeight}.gmr1")

    private fun readWatermarkOrNull(file: File): WatermarkProfile? {
        if (!file.isFile) return null
        return runCatching { file.inputStream().use(WatermarkProfileCodec::read) }.getOrNull()
    }

    private fun readGainOrNull(file: File): GainMapProfile? {
        if (!file.isFile) return null
        return runCatching { file.inputStream().use(GainMapProfileCodec::read) }.getOrNull()
    }

    companion object {
        private val BASE_FILE = Regex("base_(sdr|hdr)_(\\d+)x(\\d+)\\.wmr3")
        private val HDR_GAIN_FILE = Regex("gain_hdr_(\\d+)x(\\d+)\\.gmr1")
    }
}
