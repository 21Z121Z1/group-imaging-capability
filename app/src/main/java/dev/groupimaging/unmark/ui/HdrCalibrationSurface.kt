package dev.groupimaging.unmark.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Gainmap
import android.os.Build
import android.widget.ImageView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Displays genuine gain-map HDR content rather than merely switching the window color mode.
 * The primary stays at code value 128 while the six calibration steps sweep the enhancement
 * layer from 0 to 255. Android/ColorOS screenshot capture is then free to tone-map/re-encode this
 * content; HdrCalibrationEngine measures the actual resulting primary and gain-map baselines.
 */
@Composable
fun HdrCalibrationCaptureSurface(level: Int) {
    val probe = remember(level) { createProbe(level) }

    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_XY
                setBackgroundColor(Color.BLACK)
                setImageBitmap(probe.primary)
            }
        },
        update = { view -> view.setImageBitmap(probe.primary) },
        modifier = Modifier.fillMaxSize(),
    )

    DisposableEffect(probe) {
        onDispose {
            probe.primary.recycle()
            probe.gainContents.recycle()
        }
    }
}

private data class HdrProbe(
    val primary: Bitmap,
    val gainContents: Bitmap,
)

private fun createProbe(level: Int): HdrProbe {
    require(level in 0..255)
    val primary = Bitmap.createBitmap(PROBE_SIZE, PROBE_SIZE, Bitmap.Config.ARGB_8888).apply {
        eraseColor(Color.rgb(BASE_LEVEL, BASE_LEVEL, BASE_LEVEL))
    }
    val gainContents = Bitmap.createBitmap(PROBE_SIZE, PROBE_SIZE, Bitmap.Config.ARGB_8888).apply {
        eraseColor(Color.rgb(level, level, level))
    }
    val gainmap = Gainmap(gainContents).apply {
        setRatioMin(1f, 1f, 1f)
        setRatioMax(4f, 4f, 4f)
        setGamma(1f, 1f, 1f)
        setMinDisplayRatioForHdrTransition(1f)
        setDisplayRatioForFullHdr(4f)
        if (Build.VERSION.SDK_INT >= 36) {
            setGainmapDirection(Gainmap.GAINMAP_DIRECTION_SDR_TO_HDR)
        }
    }
    primary.gainmap = gainmap
    return HdrProbe(primary, gainContents)
}

private const val PROBE_SIZE = 64
private const val BASE_LEVEL = 128
