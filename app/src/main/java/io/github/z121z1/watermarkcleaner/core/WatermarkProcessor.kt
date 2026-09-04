package io.github.z121z1.watermarkcleaner.core

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Gainmap
import android.net.Uri
import android.os.Build
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
    suspend fun process(uri: Uri, cleanHdrGainMap: Boolean = true): ProcessedImage =
        withContext(Dispatchers.Default) {
            val decoded = resolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(
                    input,
                    null,
                    BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 },
                )
            } ?: error("无法解码图片")

            try {
                val originalGainmap = decoded.gainmap
                // ColorOS 17 screenshots from an HDR window are flattened to a Display-P3 JPEG in
                // the user's samples rather than exported as JPEG_R with an attached gain map.
                // Treat wide-gamut screenshots as the HDR/P3 compositor path so they use the
                // independently calibrated profile instead of accidentally falling back to SDR.
                val range = if (originalGainmap != null || decoded.colorSpace?.isWideGamut == true) {
                    DynamicRange.HDR
                } else {
                    DynamicRange.SDR
                }
                val orientation = CalibrationOrientation.fromDimensions(decoded.width, decoded.height)
                val primary = profiles.loadBase(decoded.width, decoded.height, range)
                    ?: error("缺少 ${orientation.label} ${range.label} 的 ${decoded.width}×${decoded.height} 校准模型；请先单独校准")

                val output = decoded.copy(Bitmap.Config.ARGB_8888, true)
                    ?: error("无法创建可编辑位图")
                BitmapProfileApplier.apply(output, primary)

                var gainMapMode = GainMapMode.NONE
                if (originalGainmap != null) {
                    require(cleanHdrGainMap) { "精确 HDR 去水印要求启用“清理 HDR gain map”" }
                    require(GainMapBitmapIO.isSdrToHdr(originalGainmap)) { "HDR_TO_SDR gain map 暂不支持精确去水印" }
                    val oldContents = originalGainmap.gainmapContents
                    val targetConfig = oldContents.config ?: Bitmap.Config.ARGB_8888
                    val newContents = oldContents.copy(targetConfig, true)
                        ?: error("无法创建可编辑 HDR gain map")
                    val gainProfile = profiles.loadHdrGain(decoded.width, decoded.height)
                        ?: error("这张图片带 gain map，但当前 ${orientation.label} HDR 校准截图是扁平 HDR/P3，未得到可验证的 gain-map 模型")
                    require(gainProfile.width == newContents.width && gainProfile.height == newContents.height) {
                        "HDR gain map 尺寸已变化；请重新校准 ${orientation.label} HDR"
                    }
                    val layout = GainMapBitmapIO.read(newContents).layout
                    require(gainProfile.layout == layout) {
                        "HDR gain map 通道布局已变化；请重新校准 ${orientation.label} HDR"
                    }
                    GainMapProfileApplier.apply(newContents, originalGainmap, gainProfile)
                    gainMapMode = GainMapMode.CALIBRATED
                    val replacement = if (Build.VERSION.SDK_INT >= 35) {
                        Gainmap(originalGainmap, newContents)
                    } else {
                        originalGainmap.apply { setGainmapContents(newContents) }
                    }
                    output.gainmap = replacement
                }
                ProcessedImage(output, range == DynamicRange.HDR, gainMapMode)
            } finally {
                decoded.recycle()
            }
        }
}
