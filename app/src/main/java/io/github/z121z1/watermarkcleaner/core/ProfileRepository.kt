package io.github.z121z1.watermarkcleaner.core

import android.content.Context
import java.io.File

class ProfileRepository(context: Context) {
    private val dir = File(context.filesDir, "profiles")
    private val primaryFile = File(dir, "primary.wmr3")
    private val gainFile = File(dir, "gain.wmr3")
    private val legacyPrimaryFile = File(dir, "primary.wmr2")
    private val legacyGainFile = File(dir, "gain.wmr2")

    fun loadPrimary(): WatermarkProfile? = readOrNull(primaryFile)
    fun loadGain(): WatermarkProfile? = readOrNull(gainFile)

    fun savePrimary(profile: WatermarkProfile) {
        WatermarkProfileCodec.writeAtomically(profile, primaryFile)
        legacyPrimaryFile.delete()
    }

    fun saveGain(profile: WatermarkProfile) {
        WatermarkProfileCodec.writeAtomically(profile, gainFile)
        legacyGainFile.delete()
    }

    fun clear() {
        primaryFile.delete()
        gainFile.delete()
        legacyPrimaryFile.delete()
        legacyGainFile.delete()
    }

    fun hasPrimary(): Boolean = primaryFile.isFile && readOrNull(primaryFile) != null
    fun hasGain(): Boolean = gainFile.isFile && readOrNull(gainFile) != null

    private fun readOrNull(file: File): WatermarkProfile? {
        if (!file.isFile) return null
        return runCatching { file.inputStream().use(WatermarkProfileCodec::read) }.getOrNull()
    }
}
