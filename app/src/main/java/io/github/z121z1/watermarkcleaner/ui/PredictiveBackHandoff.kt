package io.github.z121z1.watermarkcleaner.ui

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * App-level equivalent of ColorOS' predictive -> continuous handoff principle.
 *
 * Gesture progress owns the transform during interaction. On commit we keep the
 * last progress and spring from that exact state to 1 before changing logical
 * navigation state. A cancelled gesture springs back from its current value
 * instead of snapping to zero. Root Activity back is intentionally not handled
 * here so ColorOS Shell/Launcher can own predictive-continuous back-to-home.
 */
@Stable
data class PredictiveBackHandoffSnapshot(
    val progress: Float,
    val active: Boolean,
)

@Composable
fun rememberPredictiveBackHandoff(
    enabled: Boolean,
    onCommit: () -> Unit,
): PredictiveBackHandoffSnapshot {
    val progress = remember { Animatable(0f) }
    val recoveryScope = rememberCoroutineScope()
    var active by remember { mutableStateOf(false) }

    PredictiveBackHandler(enabled = enabled) { events: Flow<BackEventCompat> ->
        active = true
        try {
            events.collect { event ->
                progress.snapTo(event.progress.coerceIn(0f, 1f))
            }

            // Ownership handoff: continue from the gesture's final transform;
            // never reset to zero before committing the navigation state.
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            onCommit()
            progress.snapTo(0f)
            active = false
        } catch (cancelled: CancellationException) {
            // PredictiveBackHandler cancels this coroutine for a cancelled
            // gesture. Recover in the composition scope so the visual surface
            // continues smoothly from its exact cancellation state.
            recoveryScope.launch {
                progress.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                )
                active = false
            }
            throw cancelled
        }
    }

    return PredictiveBackHandoffSnapshot(
        progress = progress.value.coerceIn(0f, 1f),
        active = active,
    )
}

/** Current surface controlled by the predictive gesture. */
fun Modifier.predictiveBackOutgoing(progress: Float): Modifier = graphicsLayer {
    val p = progress.coerceIn(0f, 1f)
    scaleX = 1f - 0.055f * p
    scaleY = 1f - 0.055f * p
    translationX = size.width * 0.055f * p
    alpha = 1f - p
}

/** Destination surface kept alive behind the current surface for handoff. */
fun Modifier.predictiveBackIncoming(progress: Float): Modifier = graphicsLayer {
    val p = progress.coerceIn(0f, 1f)
    scaleX = 0.965f + 0.035f * p
    scaleY = 0.965f + 0.035f * p
    translationX = -size.width * 0.018f * (1f - p)
    alpha = 0.45f + 0.55f * p
}
