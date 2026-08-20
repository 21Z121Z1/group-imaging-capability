package dev.groupimaging.unmark.calibration

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dev.groupimaging.unmark.model.WatermarkProfile
import java.io.File
import java.io.FileOutputStream
import java.util.Arrays

/**
 * Calibrates the two independently encoded layers produced by an Ultra HDR screenshot.
 *
 * ColorOS may tone-map most of a uniform HDR probe into the primary JPEG while leaving the gain
 * map near a neutral baseline. Consequently the primary is fitted as an affine response against
 * its measured background level, while the gain map is fitted as a stable residual relative to
 * each screenshot's own gain-map background. This avoids assuming that OEM screenshot gain-map
 * code values are a linear copy of the source probe.
 */
class HdrCalibrationEngine(
    context: Context,
    private val calibrationEngine: CalibrationEngine = CalibrationEngine(context.contentResolver),
) {
    data class Result(
        val primary: WatermarkProfile,
        val gainmap: WatermarkProfile,
    )

    private val resolver = context.contentResolver
    private val cacheDir = context.cacheDir

    fun fit(screenshots: List<Uri>): Result {
        require(screenshots.size == GAIN_LEVELS.size) { "需要六张 HDR 校准截图" }

        val workDir = File(cacheDir, "hdr-calibration-${System.nanoTime()}").apply {
            check(mkdirs()) { "无法创建 HDR 校准缓存目录" }
        }
        val gainFiles = ArrayList<File>(screenshots.size)
        val primaryBaselines = FloatArray(screenshots.size)
        val gainBaselines = FloatArray(screenshots.size)

        try {
            var gainWidth = -1
            var gainHeight = -1
            screenshots.forEachIndexed { index, uri ->
                val bitmap = resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                    BitmapFactory.decodeFileDescriptor(
                        descriptor.fileDescriptor,
                        null,
                        BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        },
                    )
                } ?: error("无法解码第 ${index + 1} 张 HDR 校准截图")

                try {
                    val gainmap = bitmap.gainmap
                        ?: error("第 ${index + 1} 张截图不包含 Ultra HDR gain map；请确认 HDR 已启用")
                    val gainContents = gainmap.gainmapContents

                    if (gainWidth < 0) {
                        gainWidth = gainContents.width
                        gainHeight = gainContents.height
                    } else {
                        require(gainContents.width == gainWidth && gainContents.height == gainHeight) {
                            "六张 HDR 截图的 gain map 尺寸必须一致"
                        }
                    }

                    primaryBaselines[index] = medianGray(bitmap)
                    gainBaselines[index] = medianGray(gainContents)

                    val gainFile = File(workDir, "gain-$index.png")
                    FileOutputStream(gainFile).use { output ->
                        check(gainContents.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                            "无法缓存第 ${index + 1} 张 gain map"
                        }
                        output.flush()
                        output.fd.sync()
                    }
                    gainFiles += gainFile
                } finally {
                    bitmap.recycle()
                }
            }

            require(primaryBaselines.range() >= MIN_PRIMARY_SPREAD) {
                "HDR 校准亮度变化不足；请确认设备支持 HDR，且校准时窗口已切换到 HDR 模式"
            }

            val primary = calibrationEngine.fit(
                screenshots = screenshots,
                xLevels = primaryBaselines,
                selectionMode = CalibrationEngine.SelectionMode.ScreenOverlay,
            )
            require(primary.size > 0) { "HDR primary 中未检测到稳定水印" }

            val gainmap = calibrationEngine.fitResidualOffsets(
                images = gainFiles,
                baselines = gainBaselines,
            )

            return Result(primary = primary, gainmap = gainmap)
        } finally {
            workDir.deleteRecursively()
        }
    }

    private fun medianGray(bitmap: Bitmap): Float {
        val stepX = (bitmap.width / SAMPLE_GRID).coerceAtLeast(1)
        val stepY = (bitmap.height / SAMPLE_GRID).coerceAtLeast(1)
        val rows = ((bitmap.height - 1) / stepY) + 1
        val columns = ((bitmap.width - 1) / stepX) + 1
        val values = FloatArray(rows * columns)
        val row = IntArray(bitmap.width)
        var count = 0

        var y = 0
        while (y < bitmap.height) {
            bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
            var x = 0
            while (x < bitmap.width) {
                val argb = row[x]
                val red = argb ushr 16 and 0xff
                val green = argb ushr 8 and 0xff
                val blue = argb and 0xff
                values[count++] = (red + green + blue) / 3f
                x += stepX
            }
            y += stepY
        }

        Arrays.sort(values, 0, count)
        return if (count % 2 == 0) {
            (values[count / 2 - 1] + values[count / 2]) * 0.5f
        } else {
            values[count / 2]
        }
    }

    private fun FloatArray.range(): Float {
        var min = Float.POSITIVE_INFINITY
        var max = Float.NEGATIVE_INFINITY
        for (value in this) {
            if (value < min) min = value
            if (value > max) max = value
        }
        return max - min
    }

    companion object {
        val GAIN_LEVELS = intArrayOf(0, 51, 102, 153, 204, 255)
        private const val SAMPLE_GRID = 64
        private const val MIN_PRIMARY_SPREAD = 12f
    }
}
