package io.github.z121z1.watermarkcleaner.core

import android.graphics.Bitmap
import android.graphics.Gainmap

object GainMapProfileApplier {
    fun apply(contents: Bitmap, gainmap: Gainmap, profile: GainMapProfile) {
        require(contents.width == profile.width && contents.height == profile.height) { "gain map 尺寸与 HDR 校准模型不一致" }
        require(GainMapBitmapIO.isSdrToHdr(gainmap)) { "HDR_TO_SDR gain map 暂不支持精确去水印" }
        val channels = GainMapBitmapIO.read(contents)
        require(channels.layout == profile.layout) { "gain map 通道布局与 HDR 校准模型不一致" }
        val metadata = GainMapBitmapIO.metadata(gainmap).first

        for (pixel in profile.pixels) {
            val index = pixel.index
            val observedR = GainMapMath.toLogGain(channels.r[index], metadata[0])
            val observedG = GainMapMath.toLogGain(channels.g[index], metadata[1])
            val observedB = GainMapMath.toLogGain(channels.b[index], metadata[2])
            channels.r[index] = GainMapMath.fromLogGain(
                GainMapMath.invertLogGain(observedR, pixel.slopeR, pixel.interceptR),
                metadata[0],
            )
            if (channels.layout == GainMapLayout.MONO) {
                channels.g[index] = channels.r[index]
                channels.b[index] = channels.r[index]
            } else {
                channels.g[index] = GainMapMath.fromLogGain(
                    GainMapMath.invertLogGain(observedG, pixel.slopeG, pixel.interceptG),
                    metadata[1],
                )
                channels.b[index] = GainMapMath.fromLogGain(
                    GainMapMath.invertLogGain(observedB, pixel.slopeB, pixel.interceptB),
                    metadata[2],
                )
            }
        }
        GainMapBitmapIO.write(contents, channels)
    }
}
