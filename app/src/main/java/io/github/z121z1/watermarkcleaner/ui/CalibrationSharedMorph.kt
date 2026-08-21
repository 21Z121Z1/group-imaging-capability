package io.github.z121z1.watermarkcleaner.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.z121z1.watermarkcleaner.platform.ColorOsSurfaceRole
import kotlin.math.roundToInt

@Stable
class CalibrationMorphGeometry internal constructor() {
    var source: Rect? by mutableStateOf(null)
        internal set
    var target: Rect? by mutableStateOf(null)
        internal set

    val ready: Boolean
        get() = source != null && target != null
}

@Composable
fun rememberCalibrationMorphGeometry(): CalibrationMorphGeometry =
    remember { CalibrationMorphGeometry() }

/**
 * Forward navigation owns 0 -> 1. Predictive back owns the reverse direction by
 * multiplying the settled enter fraction by (1 - backProgress). This means the
 * exact same element geometry is used for tap-to-open and gesture-to-return.
 */
@Composable
fun rememberCalibrationMorphProgress(
    open: Boolean,
    backProgress: Float,
    geometry: CalibrationMorphGeometry,
): Float {
    val enter = remember { Animatable(0f) }

    androidx.compose.runtime.LaunchedEffect(open, geometry.target) {
        if (open && geometry.ready) {
            enter.animateTo(
                1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        } else if (!open) {
            enter.snapTo(0f)
            geometry.target = null
        }
    }

    return (enter.value * (1f - backProgress.coerceIn(0f, 1f))).coerceIn(0f, 1f)
}

@Composable
fun CalibrationEntryCard(
    modelReady: Boolean,
    visible: Boolean,
    geometry: CalibrationMorphGeometry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .height(104.dp)
            .onGloballyPositioned { geometry.source = it.boundsInRoot() }
            .graphicsLayer { alpha = if (visible) 1f else 0f },
    ) {
        ColorOsMaterialSurface(
            modifier = Modifier.fillMaxSize(),
            role = ColorOsSurfaceRole.CARD,
            corner = ColorOsCornerProfile.LARGE,
        ) {
            CalibrationHeroContent(modelReady = modelReady, progress = 0f)
        }
        // Keep interaction in Compose above the vendor AndroidView layer. The
        // ColorOS spotlight drawable installs its own touch listener underneath.
        Box(Modifier.fillMaxSize().clickable(onClick = onClick))
    }
}

@Composable
fun CalibrationTargetPlaceholder(
    geometry: CalibrationMorphGeometry,
    modifier: Modifier = Modifier,
) {
    Spacer(
        modifier
            .fillMaxWidth()
            .height(144.dp)
            .onGloballyPositioned { geometry.target = it.boundsInRoot() },
    )
}

@Composable
fun CalibrationSharedHero(
    modelReady: Boolean,
    geometry: CalibrationMorphGeometry,
    progress: Float,
) {
    val source = geometry.source ?: return
    val target = geometry.target ?: return
    val density = LocalDensity.current
    val p = progress.coerceIn(0f, 1f)

    fun mix(a: Float, b: Float): Float = a + (b - a) * p

    val left = mix(source.left, target.left)
    val top = mix(source.top, target.top)
    val widthPx = mix(source.width, target.width).coerceAtLeast(1f)
    val heightPx = mix(source.height, target.height).coerceAtLeast(1f)
    val width = with(density) { widthPx.toDp() }
    val height = with(density) { heightPx.toDp() }
    val radius = (24f + 6f * p).dp

    Box(
        Modifier
            .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
            .size(width, height)
            .clip(RoundedCornerShape(radius)),
    ) {
        ColorOsMaterialSurface(
            modifier = Modifier.fillMaxSize(),
            role = ColorOsSurfaceRole.CARD,
            corner = ColorOsCornerProfile.LARGE,
        ) {
            CalibrationHeroContent(modelReady = modelReady, progress = p)
        }
    }
}

@Composable
private fun CalibrationHeroContent(modelReady: Boolean, progress: Float) {
    val p = progress.coerceIn(0f, 1f)
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "校准",
            fontSize = (20f + 8f * p).sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height((4f + 3f * p).dp))
        Text(
            text = if (modelReady) {
                "模型已建立 · 可重新采集 SDR / HDR 六灰阶"
            } else {
                "建立截图链路模型 · SDR / HDR 六灰阶"
            },
            fontSize = (13f + 2f * p).sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (p > 0.55f) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "返回时拖动这张卡，它会落回处理页的对应位置",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer { alpha = ((p - 0.55f) / 0.45f).coerceIn(0f, 1f) },
            )
        }
    }
}
