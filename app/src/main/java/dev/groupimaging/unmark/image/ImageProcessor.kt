package dev.groupimaging.unmark.image

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Gainmap
import android.net.Uri
import android.os.Build
import dev.groupimaging.unmark.model.AffineMath
import dev.groupimaging.unmark.model.WatermarkProfile

class ImageProcessor(private val resolver: ContentResolver) {
    data class ProcessedImage(
        val bitmap: Bitmap,
        val wasUltraHdr: Boolean,
    )

    fun decodeAndRemove(uri: Uri, profile: WatermarkProfile): ProcessedImage {
        val bitmap = resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            BitmapFactory.decodeFileDescriptor(
                descriptor.fileDescriptor,
                null,
                BitmapFactory.Options().apply {
                    inMutable = true
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                },
            )
        } ?: error("无法解码图片")

        require(profile.matches(bitmap.width, bitmap.height)) {
            "当前模型为 ${profile.width}×${profile.height}，图片为 ${bitmap.width}×${bitmap.height}，请重新校准"
        }

        val originalGainmap = bitmap.gainmap
        applySparseInverse(bitmap, profile)

        if (originalGainmap != null) {
            // ColorOS Ultra HDR screenshots can contain the same visible watermark in the
            // enhancement layer. Work on a copy so the decoded source remains a stable reference.
            val cleanedContents = originalGainmap.gainmapContents.copy(Bitmap.Config.ARGB_8888, true)
                ?: error("无法创建可编辑 HDR gain map")
            GainMapCleaner.clean(cleanedContents, profile)

            val replacement = if (Build.VERSION.SDK_INT >= 35) {
                // Public API 35 constructor copies every gain-map metadata field while replacing
                // only the enhancement bitmap.
                Gainmap(originalGainmap, cleanedContents)
            } else {
                // API 34 has public content mutation but not the metadata-preserving copy ctor.
                originalGainmap.apply { setGainmapContents(cleanedContents) }
            }
            bitmap.gainmap = replacement
        }

        return ProcessedImage(bitmap = bitmap, wasUltraHdr = originalGainmap != null)
    }

    internal fun applySparseInverse(bitmap: Bitmap, profile: WatermarkProfile) {
        require(bitmap.isMutable) { "Bitmap must be mutable" }
        require(profile.matches(bitmap.width, bitmap.height)) { "Profile dimensions do not match bitmap" }

        val row = IntArray(bitmap.width)
        var record = 0
        while (record < profile.size) {
            val firstIndex = profile.indices[record]
            val y = firstIndex / profile.width
            bitmap.getPixels(row, 0, profile.width, 0, y, profile.width, 1)

            while (record < profile.size && profile.indices[record] / profile.width == y) {
                val x = profile.indices[record] - y * profile.width
                val argb = row[x]
                val alpha = argb ushr 24 and 0xff
                val red = argb ushr 16 and 0xff
                val green = argb ushr 8 and 0xff
                val blue = argb and 0xff

                val recoveredR = AffineMath.inverse(red, profile.slopeR[record], profile.interceptR[record])
                val recoveredG = AffineMath.inverse(green, profile.slopeG[record], profile.interceptG[record])
                val recoveredB = AffineMath.inverse(blue, profile.slopeB[record], profile.interceptB[record])

                row[x] = (alpha shl 24) or (recoveredR shl 16) or (recoveredG shl 8) or recoveredB
                record++
            }

            bitmap.setPixels(row, 0, profile.width, 0, y, profile.width, 1)
        }
    }
}
