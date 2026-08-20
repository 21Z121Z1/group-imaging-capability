package io.github.z121z1.watermarkcleaner.core

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Gainmap
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ProcessedImage(
    val bitmap: Bitmap,
    val wasHdr: Boolean,
    val gainMapMode: GainMapMode,
)

enum class GainMapMode { NONE, CALIBRATED, LOCAL_FALLBACK }

class WatermarkProcessor(
    private val resolver: ContentResolver,
    private val profiles: ProfileRepository,
) {
    suspend fun process(uri: Uri, cleanHdrFallback: Boolean = true): ProcessedImage =
        withContext(Dispatchers.Default) {
            val primary = profiles.loadPrimary() ?: error("请先完成水印校准")
            val decoded = resolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(
                    input,
                    null,
                    BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 },
                )
            } ?: error("无法解码图片")

            try {
                require(decoded.width == primary.width && decoded.height == primary.height) {
                    "当前模型为 ${primary.width}×${primary.height}，输入为 ${decoded.width}×${decoded.height}；请重新校准此截图尺寸"
                }
                val output = decoded.copy(Bitmap.Config.ARGB_8888, true)
                    ?: error("无法创建可编辑位图")
                BitmapProfileApplier.apply(output, primary)

                val originalGainmap = decoded.gainmap
                var gainMapMode = GainMapMode.NONE
                if (originalGainmap != null) {
                    val oldContents = originalGainmap.gainmapContents
                    val newContents = oldContents.copy(Bitmap.Config.ARGB_8888, true)
                        ?: error("无法创建可编辑 HDR gain map")
                    val gainProfile = profiles.loadGain()
                    if (gainProfile != null &&
                        gainProfile.width == newContents.width && gainProfile.height == newContents.height
                    ) {
                        BitmapProfileApplier.apply(newContents, gainProfile)
                        gainMapMode = GainMapMode.CALIBRATED
                    } else if (cleanHdrFallback) {
                        GainMapMaskCleaner.clean(newContents, primary)
                        gainMapMode = GainMapMode.LOCAL_FALLBACK
                    }
                    // API 35+ copy constructor keeps every gain-map metadata field intact while replacing contents.
                    // minSdk is 34, so use the public metadata-preserving mutation on API 34.
                    val replacement = if (android.os.Build.VERSION.SDK_INT >= 35) {
                        Gainmap(originalGainmap, newContents)
                    } else {
                        originalGainmap.apply { setGainmapContents(newContents) }
                    }
                    output.gainmap = replacement
                }
                ProcessedImage(output, originalGainmap != null, gainMapMode)
            } finally {
                decoded.recycle()
            }
        }
}
