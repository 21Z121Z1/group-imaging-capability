package dev.groupimaging.unmark.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Two-pane content layout that reacts to freeform resizing and keeps content off physical hinges.
 * Hinge bounds from Material adaptive APIs use window coordinates, so the parent position is
 * measured in the same coordinate space before converting the split back to dp.
 */
@Composable
fun AdaptiveTwoPane(
    directive: PaneScaffoldDirective,
    posture: Posture,
    modifier: Modifier = Modifier,
    primary: @Composable () -> Unit,
    secondary: @Composable () -> Unit,
) {
    var originInWindow by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { originInWindow = it.positionInWindow() },
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val verticalHinge = posture.hingeList.firstOrNull {
            it.isVertical && it.isSeparating &&
                it.bounds.right > originInWindow.x &&
                it.bounds.left < originInWindow.x + widthPx
        }
        val horizontalHinge = posture.hingeList.firstOrNull {
            !it.isVertical && it.isSeparating &&
                it.bounds.bottom > originInWindow.y &&
                it.bounds.top < originInWindow.y + heightPx
        }

        when {
            verticalHinge != null -> {
                val leftPx = (verticalHinge.bounds.left - originInWindow.x).coerceIn(0f, widthPx)
                val rightPx = (verticalHinge.bounds.right - originInWindow.x).coerceIn(leftPx, widthPx)
                val leftWidth = with(density) { leftPx.toDp() }
                val hingeWidth = with(density) { (rightPx - leftPx).toDp() }
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.width(leftWidth).fillMaxHeight()) { primary() }
                    Spacer(Modifier.width(hingeWidth))
                    Box(Modifier.weight(1f).fillMaxHeight()) { secondary() }
                }
            }

            horizontalHinge != null && posture.isTabletop -> {
                val topPx = (horizontalHinge.bounds.top - originInWindow.y).coerceIn(0f, heightPx)
                val bottomPx = (horizontalHinge.bounds.bottom - originInWindow.y).coerceIn(topPx, heightPx)
                val topHeight = with(density) { topPx.toDp() }
                val hingeHeight = with(density) { (bottomPx - topPx).toDp() }
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.height(topHeight).fillMaxWidth()) { primary() }
                    Spacer(Modifier.height(hingeHeight))
                    Box(Modifier.weight(1f).fillMaxWidth()) { secondary() }
                }
            }

            directive.maxHorizontalPartitions >= 2 -> {
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1.62f).fillMaxHeight()) { primary() }
                    Spacer(Modifier.width(directive.horizontalPartitionSpacerSize.coerceAtLeast(24.dp)))
                    Box(Modifier.weight(1f).fillMaxHeight()) { secondary() }
                }
            }

            else -> {
                // In compact/freeform windows both panes must receive bounded height. Measuring the
                // secondary LazyColumn without weight lets fillMaxSize consume the whole column and
                // collapse the preview. Explicit weights keep both surfaces usable while the
                // secondary pane remains independently scrollable.
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(0.9f).fillMaxWidth()) { primary() }
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.weight(1.1f).fillMaxWidth()) { secondary() }
                }
            }
        }
    }
}
