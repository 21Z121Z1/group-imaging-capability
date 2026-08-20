package io.github.z121z1.watermarkcleaner.core

import android.content.Context
import java.io.File

class ProfileRepository(context: Context) {
    private val dir = File(context.filesDir, "profiles")
    private val primaryFile = File(dir, "primary.wmr2")
    private val gainFile = File(dir, "gain.wmr2")

    fun loadPrimary(): WatermarkProfile? = readOrNull(primaryFile)
    fun loadGain(): WatermarkProfile? = readOrNull(gainFile)

    fun savePrimary(profile: WatermarkProfile) = WatermarkProfileCodec.writeAtomically(profile, primaryFile)
    fun saveGain(profile: WatermarkProfile) = WatermarkProfileCodec.writeAtomically(profile, gainFile)

    fun clear() {
        primaryFile.delete()
        gainFile.delete()
    }

    fun hasPrimary(): Boolean = primaryFile.isFile
    fun hasGain(): Boolean = gainFile.isFile

    private fun readOrNull(file: File): WatermarkProfile? {
        if (!file.isFile) return null
        return runCatching { file.inputStream().use(WatermarkProfileCodec::read) }.getOrNull()
    }
}
