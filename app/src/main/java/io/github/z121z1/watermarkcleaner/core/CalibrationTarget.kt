package io.github.z121z1.watermarkcleaner.core

enum class DynamicRange(val storageToken: String, val label: String) {
    SDR("sdr", "SDR"),
    HDR("hdr", "HDR"),
}

enum class CalibrationOrientation(val label: String) {
    PORTRAIT("竖屏"),
    LANDSCAPE("横屏"),
    ;

    fun matches(width: Int, height: Int): Boolean = when (this) {
        PORTRAIT -> height > width
        LANDSCAPE -> width > height
    }

    companion object {
        fun fromDimensions(width: Int, height: Int): CalibrationOrientation {
            require(width != height) { "不支持正方形截图校准" }
            return if (height > width) PORTRAIT else LANDSCAPE
        }
    }
}

data class CalibrationTarget(
    val orientation: CalibrationOrientation,
    val dynamicRange: DynamicRange,
) {
    val label: String get() = "${orientation.label} ${dynamicRange.label}"
}
