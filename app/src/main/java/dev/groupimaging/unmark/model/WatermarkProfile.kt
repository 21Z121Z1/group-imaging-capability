package dev.groupimaging.unmark.model

/**
 * Sparse per-pixel affine compositor model.
 *
 * For every indexed pixel and RGB channel the calibrated screenshot pipeline is modeled as:
 *   observed = slope * source + intercept
 * Removal applies the exact inverse and leaves all non-indexed pixels untouched.
 */
data class WatermarkProfile(
    val width: Int,
    val height: Int,
    val indices: IntArray,
    val slopeR: FloatArray,
    val slopeG: FloatArray,
    val slopeB: FloatArray,
    val interceptR: FloatArray,
    val interceptG: FloatArray,
    val interceptB: FloatArray,
) {
    val size: Int get() = indices.size

    init {
        require(width in 1..20_000) { "Invalid profile width: $width" }
        require(height in 1..20_000) { "Invalid profile height: $height" }
        require(size <= width.toLong() * height.toLong()) { "Profile contains too many pixels" }
        require(
            listOf(slopeR, slopeG, slopeB, interceptR, interceptG, interceptB).all {
                it.size == size
            },
        ) { "Profile arrays have inconsistent lengths" }

        var previous = -1
        val pixelCount = width.toLong() * height.toLong()
        indices.forEachIndexed { i, index ->
            require(index > previous) { "Profile indices must be strictly increasing" }
            require(index.toLong() in 0 until pixelCount) { "Profile index out of bounds: $index" }
            previous = index

            val slopes = floatArrayOf(slopeR[i], slopeG[i], slopeB[i])
            val intercepts = floatArrayOf(interceptR[i], interceptG[i], interceptB[i])
            require(slopes.all { it.isFinite() && it in 0.25f..1.5f }) {
                "Invalid slope at profile record $i"
            }
            require(intercepts.all { it.isFinite() && it in -128f..128f }) {
                "Invalid intercept at profile record $i"
            }
        }
    }

    fun matches(width: Int, height: Int): Boolean = this.width == width && this.height == height
}
